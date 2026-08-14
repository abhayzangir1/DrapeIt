package com.drapeproof.mobile.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialNegative
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning

enum class DrapeMode(val label: String) {
    LIVE("🔴 LIVE"),
    PHOTO("📸 PHOTO"),
    COMPARE("⚖️ COMPARE"),
}

fun shouldOpenCameraSettings(requested: Boolean, granted: Boolean, showRationale: Boolean): Boolean {
    return requested && !granted && !showRationale
}

private data class CameraColorItem(val name: String, val hex: String, val category: String)

private val cameraColorPalette = listOf(
    CameraColorItem("Royal Burgundy", "#831843", "Jewel"),
    CameraColorItem("Deep Olive", "#3F6212", "Earth"),
    CameraColorItem("Cobalt Navy", "#1D4ED8", "Jewel"),
    CameraColorItem("Terracotta Clay", "#B45309", "Earth"),
    CameraColorItem("Emerald Pine", "#047857", "Jewel"),
    CameraColorItem("Pure White", "#FFFFFF", "Neutrals"),
    CameraColorItem("Warm Ivory", "#F7EFE8", "Neutrals"),
    CameraColorItem("Oatmeal Beige", "#E3D8C8", "Neutrals"),
    CameraColorItem("Camel Tan", "#C19A6B", "Neutrals"),
    CameraColorItem("Midnight Charcoal", "#1F2937", "Neutrals"),
    CameraColorItem("Dusty Rose", "#FDA4AF", "Pastels"),
    CameraColorItem("Sky Blue", "#93C5FD", "Pastels"),
)

private enum class CameraControlTab(val label: String) {
    FABRIC("FABRIC MATERIAL"),
    COLOR("COLORWAY"),
}

@Composable
fun DrapeCaptureScreen(
    onBack: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToTryOn: ((fabricId: String, colorHex: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var activeMode by remember { mutableStateOf(DrapeMode.LIVE) }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!permissionGranted && activeMode == DrapeMode.LIVE) {
        Surface(modifier = Modifier.fillMaxSize(), color = EditorialCream) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("Camera Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                Spacer(Modifier.height(8.dp))
                Text(
                    "DrapeIt measures live skin colorimetry and projects virtual fabrics onto your chest.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = EditorialMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Grant Camera Access", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onBack, shape = RoundedCornerShape(14.dp)) {
                    Text("Back to Studio", color = EditorialInk)
                }
            }
        }
        return
    }

    // Live Camera & Real Skin State
    var latestReading by remember { mutableStateOf<FrameReading?>(null) }
    var detectedSkinHex by remember { mutableStateOf<String?>(null) }
    val effectiveSkinHex = detectedSkinHex ?: "#D8B498"

    // Photo Mode State
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    photoBitmap = BitmapFactory.decodeStream(stream)
                }
            }
        }
    }

    // Fabric & Color Selection
    var activeTab by remember { mutableStateOf(CameraControlTab.FABRIC) }
    var selectedFabric by remember { mutableStateOf(FabricCatalog.defaultFabric) }
    var selectedColor by remember { mutableStateOf(cameraColorPalette[0]) }
    var isCustomMode by remember { mutableStateOf(false) }
    var customHue by remember { mutableStateOf(340f) }
    var customHex by remember { mutableStateOf<String?>(null) }

    // Bottom Drawer Collapse State & Why Breakdown State
    var isControlsExpanded by remember { mutableStateOf(true) }
    var isWhyExpanded by remember { mutableStateOf(false) }

    // Real Perceptual Color Compatibility Evaluation
    val activeColorHex = customHex ?: selectedColor.hex
    val harmonyResult = remember(effectiveSkinHex, activeColorHex) {
        TrueColorHarmonyEngine.evaluate(effectiveSkinHex, activeColorHex)
    }

    // Dynamic Status Colors
    val statusColor = when {
        harmonyResult.scorePercent >= 86 -> EditorialPositive
        harmonyResult.scorePercent >= 70 -> EditorialWarning
        harmonyResult.scorePercent >= 48 -> Color(0xFFF97316)
        else -> EditorialNegative
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "OvalStatusColor",
    )
    val animatedClothColor by animateColorAsState(
        targetValue = activeColorHex.asComposeColor(),
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "VirtualClothColor",
    )
    val animatedScore by animateIntAsState(
        targetValue = harmonyResult.scorePercent,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "LiveScoreInt",
    )

    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2000)
            toastMessage = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LIVE CAMERA OR PHOTO MODE VIEWPORT
        if (activeMode == DrapeMode.LIVE) {
            ControlledCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrame = { reading ->
                    latestReading = reading
                    reading.skinSrgb?.let { srgb ->
                        val hex = srgb.toHex()
                        detectedSkinHex = hex
                        SkinProfileRepository.save(
                            context,
                            StoredSkinProfile(
                                skinHex = hex,
                                evidenceTier = com.drapeproof.core.domain.EvidenceTier.CONTROLLED_PAIR,
                                source = "live_camera_ar",
                                capturedAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
                },
                onControlsReady = {},
                onCameraError = {},
            )
        } else {
            // PHOTO MODE CANVAS
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                photoBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Uploaded Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } ?: run {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📷", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Select Photo from Gallery", color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. 100% OPAQUE VIRTUAL CLOTH DRAPE & CHIN-ANCHORED MESH
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val reading = latestReading
            val hasFace = reading?.hasFace == true

            val chinX = if (hasFace && activeMode == DrapeMode.LIVE) (1.0f - reading!!.chinX) * width else width * 0.50f
            val chinY = if (hasFace && activeMode == DrapeMode.LIVE) reading!!.chinY * height else height * 0.58f

            val clothNeckTopY = (chinY + height * 0.035f).coerceIn(height * 0.45f, height * 0.75f)
            val neckDipY = (clothNeckTopY + height * 0.07f).coerceIn(height * 0.52f, height * 0.85f)

            // Dynamic Chin-Anchored Drape Polygon
            val drapePath = Path().apply {
                moveTo(0f, clothNeckTopY)
                cubicTo(
                    width * 0.20f, clothNeckTopY,
                    chinX - width * 0.16f, neckDipY,
                    chinX, neckDipY,
                )
                cubicTo(
                    chinX + width * 0.16f, neckDipY,
                    width * 0.80f, clothNeckTopY,
                    width, clothNeckTopY,
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            // LAYER A: 100% SOLID OPAQUE FOUNDATION (Zero Shirt / Background Bleed)
            drawPath(
                path = drapePath,
                color = animatedClothColor,
                style = Fill,
            )

            // LAYER B: PHYSICAL WEAVE TEXTURE & SPECULAR HIGHLIGHTS
            when (selectedFabric.id) {
                "silk", "satin" -> {
                    drawPath(
                        path = drapePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.38f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.30f),
                            ),
                            startY = clothNeckTopY,
                            endY = height,
                        ),
                    )
                }
                "denim" -> {
                    val twillSpacing = 14.dp.toPx()
                    var x = 0f
                    while (x < width * 2) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.16f),
                            start = Offset(x, clothNeckTopY),
                            end = Offset(x - height * 0.4f, height),
                            strokeWidth = 2.dp.toPx(),
                        )
                        x += twillSpacing
                    }
                    drawPath(
                        path = drapePath,
                        color = Color(0xFFD4A373).copy(alpha = 0.75f),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                        ),
                    )
                }
                "linen" -> {
                    val slubSpacing = 18.dp.toPx()
                    var y = clothNeckTopY
                    while (y < height) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.12f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                        y += slubSpacing
                    }
                }
                "velvet" -> {
                    drawPath(
                        path = drapePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.35f),
                            ),
                        ),
                    )
                }
                "knit" -> {
                    val ribSpacing = 16.dp.toPx()
                    var rx = 0f
                    while (rx < width) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.18f),
                            start = Offset(rx, clothNeckTopY),
                            end = Offset(rx, height),
                            strokeWidth = 3.dp.toPx(),
                        )
                        rx += ribSpacing
                    }
                }
                else -> {
                    drawPath(
                        path = drapePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.24f),
                            ),
                            startY = clothNeckTopY,
                            endY = height,
                        ),
                    )
                }
            }

            // LAYER C: Tailored Collar Seam Edge
            drawPath(
                path = drapePath,
                color = Color.White.copy(alpha = 0.50f),
                style = Stroke(width = 2.dp.toPx()),
            )

            // LAYER D: Center Face Reticle Oval with Dynamic Match Gradient
            if (activeMode == DrapeMode.LIVE) {
                val faceCenter = Offset(width * 0.50f, height * 0.34f)
                val ovalW = width * 0.52f
                val ovalH = height * 0.34f

                drawOval(
                    color = if (hasFace) animatedStatusColor else Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(faceCenter.x - ovalW / 2, faceCenter.y - ovalH / 2),
                    size = Size(ovalW, ovalH),
                    style = Stroke(width = 3.5.dp.toPx()),
                )
            }
        }

        // 3. TOP BAR: MODE SELECTOR & CAPTURE QUALITY PILL
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Top Mode Toggle: LIVE vs PHOTO vs COMPARE
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(3.dp),
                ) {
                    DrapeMode.values().forEach { mode ->
                        val isSel = activeMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) EditorialSienna else Color.Transparent)
                                .clickable {
                                    if (mode == DrapeMode.COMPARE) {
                                        onNavigateToCompare()
                                    } else {
                                        activeMode = mode
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = Color.White,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                // Close Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Live Capture Quality Pill
            val qualityLabel = latestReading?.lightingStatusLabel ?: "Align Face in Oval"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    qualityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.90f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(235.dp))

            // PROMINENT MATCH PERCENTAGE BADGE (Directly below face oval)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.5.dp, animatedStatusColor.copy(alpha = 0.85f), RoundedCornerShape(18.dp))
                    .clickable { isWhyExpanded = !isWhyExpanded }
                    .padding(horizontal = 16.dp, vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$animatedScore%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = animatedStatusColor,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "•  ${harmonyResult.harmonyLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (isWhyExpanded) "▲" else "▼", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                }
            }

            // SLIDE-UP "WHY IT WORKS" BREAKDOWN PANEL
            AnimatedVisibility(
                visible = isWhyExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(top = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Why This Works",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(6.dp))
                        harmonyResult.reasonsList.forEach { reason ->
                            Text(
                                reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                }
            }
        }

        // 4. FLOATING TOAST FEEDBACK
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.90f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(msg, style = MaterialTheme.typography.labelLarge, color = EditorialWarning, fontWeight = FontWeight.Bold)
            }
        }

        // 5. COLLAPSIBLE NATIVE CAMERA DIAL CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { isControlsExpanded = !isControlsExpanded }
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                Text(
                    if (isControlsExpanded) "▼ Hide Controls" else "▲ Open Controls",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                )
            }

            AnimatedVisibility(
                visible = isControlsExpanded,
                enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.90f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CameraControlTab.values().forEach { tab ->
                                val isSelected = activeTab == tab
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { activeTab = tab }
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        tab.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                        letterSpacing = 1.sp,
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.height(3.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(EditorialSienna),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        when (activeTab) {
                            CameraControlTab.FABRIC -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    FabricCatalog.allFabrics.forEach { fab ->
                                        val isSelected = selectedFabric.id == fab.id
                                        val scale by animateFloatAsState(if (isSelected) 1.12f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .scale(scale)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedFabric = fab }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(fab.icon, fontSize = 22.sp)
                                            Spacer(Modifier.height(3.dp))
                                            Text(
                                                fab.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }
                                }
                            }

                            CameraControlTab.COLOR -> {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            if (isCustomMode) "Custom Color Dial" else "Curated Color Palette",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                        )
                                        Text(
                                            if (isCustomMode) "Presets" else "+ Custom Picker",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EditorialWarning,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable { isCustomMode = !isCustomMode },
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    if (isCustomMode) {
                                        Slider(
                                            value = customHue,
                                            onValueChange = {
                                                customHue = it
                                                val rgb = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.75f, 0.85f))
                                                customHex = String.format("#%06X", 0xFFFFFF and rgb)
                                                selectedColor = CameraColorItem("Custom Shade", customHex!!, "Custom")
                                            },
                                            valueRange = 0f..360f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = EditorialSienna,
                                                activeTrackColor = EditorialSienna,
                                            ),
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            cameraColorPalette.forEach { item ->
                                                val isSelected = activeColorHex.equals(item.hex, ignoreCase = true)
                                                val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier
                                                        .clickable {
                                                            selectedColor = item
                                                            customHex = null
                                                        }
                                                        .padding(2.dp),
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .scale(scale)
                                                            .clip(CircleShape)
                                                            .background(item.hex.asComposeColor())
                                                            .border(
                                                                if (isSelected) 2.5.dp else 1.dp,
                                                                if (isSelected) EditorialSienna else Color.White.copy(alpha = 0.4f),
                                                                CircleShape,
                                                            ),
                                                    )
                                                    Spacer(Modifier.height(3.dp))
                                                    Text(
                                                        item.name,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) EditorialSienna else Color.White.copy(alpha = 0.7f),
                                                        fontSize = 10.sp,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Action Buttons: [ ⚖️ Compare ] + [ 📸 AI Try-On ]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = onNavigateToCompare,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.42f)
                                    .height(48.dp),
                            ) {
                                Text("⚖️ Compare", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (onNavigateToTryOn != null) {
                                        onNavigateToTryOn(selectedFabric.id, activeColorHex)
                                    } else {
                                        onBack()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                modifier = Modifier
                                    .weight(0.58f)
                                    .height(48.dp),
                            ) {
                                Text("📸 AI Try-On  →", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.asComposeColor(): Color {
    return runCatching {
        val value = removePrefix("#").toLong(16)
        Color(
            red = ((value shr 16) and 0xFF).toInt(),
            green = ((value shr 8) and 0xFF).toInt(),
            blue = (value and 0xFF).toInt(),
        )
    }.getOrDefault(Color.Gray)
}
