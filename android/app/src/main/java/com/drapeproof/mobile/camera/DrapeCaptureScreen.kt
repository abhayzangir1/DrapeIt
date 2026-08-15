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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

fun shouldOpenCameraSettings(hasRequestedPermission: Boolean, isGranted: Boolean, showRationale: Boolean): Boolean {
    return hasRequestedPermission && !isGranted && !showRationale
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

// Repeated palette for seamless infinite scrolling loop
private val infiniteColorPalette = cameraColorPalette + cameraColorPalette + cameraColorPalette + cameraColorPalette + cameraColorPalette

@Composable
fun DrapeCaptureScreen(
    initialFabricId: String? = null,
    initialColorHex: String? = null,
    onBack: () -> Unit,
    onNavigateToCompare: () -> Unit = {},
    onNavigateToTryOn: ((fabricId: String, colorHex: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    // Photo Mode State (if user chooses to drape on an uploaded photo)
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

    // Camera & Flash / Torch State
    var cameraControls by remember { mutableStateOf<DrapeCameraControls?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    // Live Camera & Face Tracking State
    var latestReading by remember { mutableStateOf<FrameReading?>(null) }
    var detectedSkinHex by remember { mutableStateOf<String?>(null) }
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }

    // Fabric & Color Selection (with optional preselection from Explore)
    var selectedFabric by remember {
        mutableStateOf(if (initialFabricId != null) FabricCatalog.findById(initialFabricId) else FabricCatalog.defaultFabric)
    }
    var isFabricListExpanded by remember { mutableStateOf(false) }
    var selectedColor by remember {
        mutableStateOf(cameraColorPalette.find { it.hex.equals(initialColorHex, ignoreCase = true) } ?: cameraColorPalette[0])
    }
    var customHex by remember { mutableStateOf(initialColorHex) }
    var isCustomPickerOpen by remember { mutableStateOf(false) }

    // Real Perceptual Color Compatibility Evaluation
    val activeColorHex = customHex ?: selectedColor.hex
    val hasFaceDetected = latestReading?.hasFace == true
    val effectiveSkinHex = detectedSkinHex ?: SkinProfileRepository.load(context)?.skinHex ?: "#D8B498"

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

    // Landmark Low-Pass Filter Smoothing
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

    val colorScrollState = rememberScrollState()

    // Handle Flash toggle
    LaunchedEffect(isFlashOn) {
        cameraControls?.setTorch(isFlashOn)
    }

    if (!permissionGranted && photoBitmap == null) {
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
                    "DrapeIt projects virtual fabrics and measures live facial colorimetry in real time.",
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
                OutlinedButton(onClick = { photoPickerLauncher.launch("image/*") }, shape = RoundedCornerShape(14.dp)) {
                    Text("Upload Photo Instead", color = EditorialInk)
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LIVE CAMERA OR PHOTO VIEWPORT
        if (photoBitmap == null) {
            ControlledCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrame = { reading ->
                    latestReading = reading
                    reading.skinSrgb?.let { srgb ->
                        detectedSkinHex = srgb.toHex()
                    }
                },
                onControlsReady = { controls -> cameraControls = controls },
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
                Image(
                    bitmap = photoBitmap!!.asImageBitmap(),
                    contentDescription = "Uploaded Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Low-light front screen fill flash glow when flash is enabled
        if (isFlashOn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFFBEB).copy(alpha = 0.35f)),
            )
        }

        // 2. PHOTOREALISTIC FABRIC DRAPE CANVAS (PBR Shader + Shading)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val hasFace = latestReading?.hasFace == true
            val chinX = if (hasFace && photoBitmap == null) smoothedChinX * width else width * 0.50f
            val chinY = if (hasFace && photoBitmap == null) smoothedChinY * height else height * 0.54f

            val clothNeckTopY = (chinY + height * 0.032f).coerceIn(height * 0.44f, height * 0.72f)
            val neckDipY = (clothNeckTopY + height * 0.065f).coerceIn(height * 0.50f, height * 0.80f)

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

            // Reticle Oval for centering face
            if (photoBitmap == null) {
                val faceCenter = Offset(width * 0.50f, height * 0.32f)
                val ovalW = width * 0.54f
                val ovalH = height * 0.34f

                drawOval(
                    color = animatedStatusColor,
                    topLeft = Offset(faceCenter.x - ovalW / 2, faceCenter.y - ovalH / 2),
                    size = Size(ovalW, ovalH),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }

        // 3. TOP BAR: TITLE + HARMONY SCORE PILL + FLASH TOGGLE (TOP RIGHT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Title & Photo status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("🪞 Drape Studio", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (photoBitmap != null) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(EditorialSienna)
                            .clickable { photoBitmap = null }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("✕ Live", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Right side: Match score badge + FLASH TOGGLE (TOP RIGHT)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Match Score Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(1.5.dp, animatedStatusColor, RoundedCornerShape(18.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(animatedStatusColor))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$animatedScore% ${harmonyResult.harmonyLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                // FLASH / TORCH TOGGLE BUTTON AT TOP RIGHT
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isFlashOn) Color(0xFFFBBF24) else Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, if (isFlashOn) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable { isFlashOn = !isFlashOn },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (isFlashOn) "⚡" else "⚡", fontSize = 18.sp, color = if (isFlashOn) Color.Black else Color.White)
                }
            }
        }

        // TOAST FEEDBACK
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.dp, EditorialSienna, RoundedCornerShape(16.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Text(toastMessage!!, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // 4. UPWARD EXPANDING FABRIC LIST MODAL / BOTTOM SHEET
        if (isFabricListExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.50f))
                    .clickable { isFabricListExpanded = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) {}
                        .padding(horizontal = 4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .navigationBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Select Fabric Material",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Text(
                                "Done ✕",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialSienna,
                                modifier = Modifier.clickable { isFabricListExpanded = false },
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Grid of all fabric materials
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FabricCatalog.allFabrics.chunked(3).forEach { rowFabrics ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    rowFabrics.forEach { fab ->
                                        val isSel = selectedFabric.id == fab.id
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSel) EditorialSienna else EditorialSand.copy(alpha = 0.5f))
                                                .border(1.5.dp, if (isSel) EditorialSienna else Color.Transparent, RoundedCornerShape(14.dp))
                                                .clickable {
                                                    selectedFabric = fab
                                                    isFabricListExpanded = false
                                                }
                                                .padding(vertical = 12.dp, horizontal = 6.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(fab.icon, fontSize = 22.sp)
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    fab.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSel) Color.White else EditorialInk,
                                                )
                                            }
                                        }
                                    }
                                    // Filler if odd
                                    if (rowFabrics.size < 3) {
                                        repeat(3 - rowFabrics.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. BOTTOM CONTROL DECK: ACTION ROW (PHOTO UPLOAD | CAPTURE | FABRIC) + INFINITE COLORS
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp, start = 14.dp, end = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ACTION ROW: [ 🖼️ Photo Upload ] (Left) + [ 📸 CAPTURE ] (Center) + [ 🧵 Fabric ] (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // LEFT: PHOTO UPLOAD ICON BUTTON
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🖼️", fontSize = 22.sp)
                }

                // CENTER: LARGE SHUTTER CAPTURE BUTTON (📸)
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, EditorialSienna, CircleShape)
                        .clickable {
                            val capturedSkinHex = detectedSkinHex ?: effectiveSkinHex

                            val rawUserBitmap = if (photoBitmap == null) {
                                activePreviewView?.bitmap
                            } else {
                                photoBitmap
                            }

                            if (rawUserBitmap != null) {
                                PhotoAvatarStore.saveAvatarFromBitmap(
                                    context = context,
                                    bitmap = rawUserBitmap,
                                    name = "My Portrait",
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
                            }

                            toastMessage = "📸 Look saved to Looks & Compare!"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(EditorialSienna),
                    )
                }

                // RIGHT: FABRIC ICON EXPAND BUTTON (UPWARD EXPANSION)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .border(1.5.dp, EditorialSienna, CircleShape)
                        .clickable { isFabricListExpanded = !isFabricListExpanded },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(selectedFabric.icon, fontSize = 20.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // BOTTOM-MOST: BORDERLESS INFINITE LOOPING COLOR SWATCHES + PERMANENT COLOR PICKER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // PERMANENT COLOR PICKER BUTTON (FIXED ON LEFT)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EditorialSand.copy(alpha = 0.30f))
                        .border(1.5.dp, EditorialSienna, CircleShape)
                        .clickable { isCustomPickerOpen = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎨", fontSize = 18.sp)
                }

                Spacer(Modifier.width(10.dp))

                // BORDERLESS INFINITE COLOR SWATCHES
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(colorScrollState),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    infiniteColorPalette.forEach { item ->
                        val isSelected = activeColorHex.equals(item.hex, ignoreCase = true)
                        val scale by animateFloatAsState(if (isSelected) 1.25f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(item.hex.asComposeColor())
                                .clickable {
                                    selectedColor = item
                                    customHex = null
                                },
                        )
                    }
                }
            }
        }

        // UNIVERSAL COLOR PICKER MODAL
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
