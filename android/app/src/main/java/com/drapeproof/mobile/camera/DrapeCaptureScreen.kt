package com.drapeproof.mobile.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.camera.view.PreviewView
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.avatar.AvatarLighting
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.fabric.FabricTextureShader
import com.drapeproof.mobile.ui.UniversalColorPickerDialog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialNegative
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@Composable
fun DrapeCaptureScreen(
    onBack: () -> Unit,
    onNavigateToCompare: () -> Unit,
    onNavigateToTryOn: ((fabricId: String, colorHex: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    // Live Camera & Face Tracking State
    var latestReading by remember { mutableStateOf<FrameReading?>(null) }
    var detectedSkinHex by remember { mutableStateOf<String?>(null) }
    var rawFrameBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
    var selectedFabric by remember { mutableStateOf(FabricCatalog.defaultFabric) }
    var isFabricDropdownOpen by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(cameraColorPalette[0]) }
    var isCustomPickerOpen by remember { mutableStateOf(false) }
    var customHue by remember { mutableStateOf(340f) }
    var customHex by remember { mutableStateOf<String?>(null) }

    // Why Breakdown State
    var isWhyExpanded by remember { mutableStateOf(false) }

    // Real Perceptual Color Compatibility Evaluation
    val activeColorHex = customHex ?: selectedColor.hex
    val hasFaceDetected = latestReading?.hasFace == true
    val effectiveSkinHex = detectedSkinHex ?: "#D8B498"

    val harmonyResult = remember(effectiveSkinHex, activeColorHex) {
        TrueColorHarmonyEngine.evaluate(effectiveSkinHex, activeColorHex)
    }

    val statusColor = when {
        harmonyResult.scorePercent >= 86 -> EditorialPositive
        harmonyResult.scorePercent >= 70 -> EditorialWarning
        harmonyResult.scorePercent >= 48 -> Color(0xFFF97316)
        else -> EditorialNegative
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = if (hasFaceDetected) statusColor else Color.White.copy(alpha = 0.6f),
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
            kotlinx.coroutines.delay(2200)
            toastMessage = null
        }
    }

    // Landmark Low-Pass Filter Smoothing (removes tracking jitter for stable, weighted drape)
    var smoothedChinX by remember { mutableStateOf(0.50f) }
    var smoothedChinY by remember { mutableStateOf(0.54f) }
    var smoothedYaw by remember { mutableStateOf(0.0f) }

    LaunchedEffect(latestReading) {
        val reading = latestReading
        if (reading != null && reading.hasFace) {
            val targetX = 1.0f - reading.chinX
            val targetY = reading.chinY
            smoothedChinX += 0.28f * (targetX - smoothedChinX)
            smoothedChinY += 0.28f * (targetY - smoothedChinY)
            val targetYaw = (targetX - 0.50f) * 3.14f
            smoothedYaw += 0.20f * (targetYaw - smoothedYaw)
        }
    }

    // Decoupled Fabric Tile Texture Cache
    val activeTile = remember(selectedFabric.id) {
        FabricTextureShader.getOrLoadTile(context, selectedFabric.id)
    }

    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LIVE CAMERA OR PHOTO MODE VIEWPORT
        if (activeMode == DrapeMode.LIVE) {
            ControlledCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrame = { reading ->
                    latestReading = reading
                    reading.skinSrgb?.let { srgb ->
                        detectedSkinHex = srgb.toHex()
                    }
                },
                onControlsReady = {},
                onCameraError = {},
                onPreviewReady = { view -> activePreviewView = view },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap!!.asImageBitmap(),
                        contentDescription = "Uploaded Selfie",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("🖼️", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Upload a Selfie Photo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Select a clear portrait to test luxury fabrics and colors directly on your photo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        ) {
                            Text("Select Photo from Gallery", color = Color.White)
                        }
                    }
                }
            }
        }

        // 2. 100% OPAQUE REALISTIC FABRIC DRAPE CANVAS (PBR Textures + Luminance-Preserving Blend)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val hasFace = latestReading?.hasFace == true

            val chinX = if (hasFace && activeMode == DrapeMode.LIVE) smoothedChinX * width else width * 0.50f
            val chinY = if (hasFace && activeMode == DrapeMode.LIVE) smoothedChinY * height else height * 0.54f

            val clothNeckTopY = (chinY + height * 0.032f).coerceIn(height * 0.44f, height * 0.72f)
            val neckDipY = (clothNeckTopY + height * 0.065f).coerceIn(height * 0.50f, height * 0.80f)

            // Tailored Anatomical Drape Polygon
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

            // Render Photorealistic PBR Material Shaders
            FabricTextureShader.renderFabricDrape(
                scope = this,
                path = drapePath,
                fabric = selectedFabric,
                baseColor = animatedClothColor,
                width = width,
                height = height,
                neckTopY = clothNeckTopY,
                tileBitmap = activeTile,
                motionYaw = smoothedYaw,
            )

            // Center Face Reticle Oval
            if (activeMode == DrapeMode.LIVE) {
                val faceCenter = Offset(width * 0.50f, height * 0.32f)
                val ovalW = width * 0.54f
                val ovalH = height * 0.34f

                drawOval(
                    color = animatedStatusColor,
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

            // Guidance & Lighting Quality Pill
            val qualityLabel = when {
                !hasFaceDetected -> "Align Face in Oval & Hold Still"
                latestReading?.lightingStatusLabel != null -> latestReading!!.lightingStatusLabel
                else -> "Hold Still for Best Results"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
            ) {
                Text(
                    qualityLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(235.dp))

            // SLIDE-UP "WHY IT WORKS" BREAKDOWN (EXPANDS ABOVE SCORE BADGE)
            AnimatedVisibility(
                visible = isWhyExpanded && hasFaceDetected,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(bottom = 8.dp),
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
                                "•  $reason",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                }
            }

            // PROMINENT MATCH BADGE (PLACED COMPLETELY BELOW THE OVAL RETICLE)
            if (hasFaceDetected) {
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

        // 5. STREAMLINED CAMERA DIAL & SHUTTER CONTROLS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // FLOATING COMPACT FABRIC PILL
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.80f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { isFabricDropdownOpen = !isFabricDropdownOpen }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedFabric.icon, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${selectedFabric.name} ▼",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                DropdownMenu(
                    expanded = isFabricDropdownOpen,
                    onDismissRequest = { isFabricDropdownOpen = false },
                ) {
                    FabricCatalog.allFabrics.forEach { fab ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(fab.icon, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(fab.name, fontWeight = FontWeight.Bold, color = EditorialInk)
                                        Text(fab.weaveType, style = MaterialTheme.typography.labelSmall, color = EditorialMuted)
                                    }
                                }
                            },
                            onClick = {
                                selectedFabric = fab
                                isFabricDropdownOpen = false
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // UNIVERSAL COLOR PICKER DIALOG
            if (isCustomPickerOpen) {
                UniversalColorPickerDialog(
                    initialColorHex = activeColorHex,
                    onDismiss = { isCustomPickerOpen = false },
                    onColorSelected = { hex, name ->
                        customHex = hex
                        selectedColor = CameraColorItem(name, hex, "Custom")
                        isCustomPickerOpen = false
                    },
                )
            }

            // HORIZONTAL COLOR SWATCH ROW WITH FIXED COLOR PICKER ON RIGHT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Color swatches (Horizontal scrollable)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    cameraColorPalette.forEach { item ->
                        val isSelected = activeColorHex.equals(item.hex, ignoreCase = true)
                        val scale by animateFloatAsState(if (isSelected) 1.2f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

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
                                )
                                .clickable {
                                    selectedColor = item
                                    customHex = null
                                },
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // FIXED COLOR PICKER BUTTON (ALWAYS VISIBLE ON FAR RIGHT)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EditorialSand.copy(alpha = 0.25f))
                        .border(1.5.dp, EditorialSienna, CircleShape)
                        .clickable { isCustomPickerOpen = !isCustomPickerOpen },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎨", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // BOTTOM ACTION BAR: [ ⚖️ Compare ] + [ 📸 SHUTTER CAPTURE ] + [ 👗 AI Try-On ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Compare Button
                OutlinedButton(
                    onClick = onNavigateToCompare,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(100.dp),
                ) {
                    Text("⚖️ Compare", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                }

                // CENTRAL SHUTTER CAPTURE BUTTON (📸)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, EditorialSienna, CircleShape)
                        .clickable {
                            // Explicit user capture action
                            val capturedSkinHex = detectedSkinHex ?: "#D8B498"
                            SkinProfileRepository.save(
                                context,
                                StoredSkinProfile(
                                    skinHex = capturedSkinHex,
                                    evidenceTier = com.drapeproof.core.domain.EvidenceTier.CONTROLLED_PAIR,
                                    source = "live_camera_capture",
                                    capturedAtEpochMillis = System.currentTimeMillis(),
                                ),
                            )

                            val rawUserBitmap = if (activeMode == DrapeMode.LIVE) {
                                activePreviewView?.bitmap
                            } else {
                                photoBitmap
                            }

                            if (rawUserBitmap != null) {
                                // Save original user face as an avatar for Try-On studio
                                PhotoAvatarStore.saveAvatarFromBitmap(
                                    context = context,
                                    bitmap = rawUserBitmap,
                                    name = "My Photo",
                                    lighting = AvatarLighting.DAYLIGHT,
                                    skinHex = capturedSkinHex,
                                )

                                val comp = Bitmap.createBitmap(rawUserBitmap.width, rawUserBitmap.height, Bitmap.Config.ARGB_8888)
                                val compCanvas = Canvas(comp)
                                compCanvas.drawBitmap(rawUserBitmap, 0f, 0f, null)

                                val w = rawUserBitmap.width.toFloat()
                                val h = rawUserBitmap.height.toFloat()
                                val chinX = smoothedChinX * w
                                val chinY = smoothedChinY * h

                                val drapePath = AndroidPath().apply {
                                    moveTo(0f, chinY + h * 0.12f)
                                    cubicTo(
                                        w * 0.20f, chinY + h * 0.06f,
                                        chinX - w * 0.12f, chinY + h * 0.02f,
                                        chinX, chinY + h * 0.03f,
                                    )
                                    cubicTo(
                                        chinX + w * 0.12f, chinY + h * 0.02f,
                                        w * 0.80f, chinY + h * 0.06f,
                                        w, chinY + h * 0.12f,
                                    )
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }

                                val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = android.graphics.Color.parseColor(activeColorHex)
                                    style = Paint.Style.FILL
                                }
                                compCanvas.drawPath(drapePath, basePaint)

                                val rawTile = FabricTextureShader.getOrLoadRawBitmap(context, selectedFabric.id)
                                rawTile?.let { tile ->
                                    val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                                    val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        shader = tileShader
                                        alpha = (selectedFabric.textureAlpha * 255).toInt().coerceIn(0, 255)
                                        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                                    }
                                    compCanvas.drawPath(drapePath, tilePaint)
                                }

                                DrapeSnapRepository.saveSnap(
                                    context = context,
                                    bitmap = comp,
                                    colorHex = activeColorHex,
                                    colorName = selectedColor.name,
                                    fabricId = selectedFabric.id,
                                    fabricName = selectedFabric.name,
                                    matchScorePercent = harmonyResult.scorePercent,
                                    skinHex = capturedSkinHex,
                                )
                            } else {
                                val fallback = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888)
                                val c = Canvas(fallback)
                                c.drawColor(android.graphics.Color.parseColor(activeColorHex))
                                DrapeSnapRepository.saveSnap(
                                    context = context,
                                    bitmap = fallback,
                                    colorHex = activeColorHex,
                                    colorName = selectedColor.name,
                                    fabricId = selectedFabric.id,
                                    fabricName = selectedFabric.name,
                                    matchScorePercent = harmonyResult.scorePercent,
                                    skinHex = capturedSkinHex,
                                )
                            }

                            toastMessage = "📸 Captured! Saved to Compare & Lookbook."
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EditorialSienna),
                    )
                }

                // AI Try-On Button
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
                    modifier = Modifier.width(100.dp),
                ) {
                    Text("📸 Try-On", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
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
