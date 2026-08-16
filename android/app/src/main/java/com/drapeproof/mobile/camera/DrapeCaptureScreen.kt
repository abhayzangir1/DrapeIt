package com.drapeproof.mobile.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import com.drapeproof.mobile.ui.sound.SoundEffectManager
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.drapeproof.mobile.avatar.AvatarLighting
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.fabric.FabricTextureShader
import com.drapeproof.mobile.ui.UniversalColorPickerDialog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialNegative
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning
import kotlinx.coroutines.launch

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

@Composable
fun DrapeCaptureScreen(
    initialFabricId: String? = null,
    initialColorHex: String? = null,
    onBack: () -> Unit = {},
    onNavigateToLooks: () -> Unit = {},
    onNavigateToCompare: () -> Unit = {},
    onNavigateToTryOn: ((fabricId: String, colorHex: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val currentView = LocalView.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SoundEffectManager.init()
    }

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

    var cameraControls by remember { mutableStateOf<DrapeCameraControls?>(null) }
    var latestReading by remember { mutableStateOf<FrameReading?>(null) }
    var detectedSkinHex by remember { mutableStateOf<String?>(null) }
    var activePreviewView by remember { mutableStateOf<PreviewView?>(null) }

    // Run real one-shot face and skin tone analysis on static photo when uploaded from gallery
    LaunchedEffect(photoBitmap) {
        val bmp = photoBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            runCatching {
                val landmarker = FaceLandmarker.createFromOptions(
                    context,
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                        .setBaseOptions(
                            BaseOptions.builder()
                                .setModelAssetPath("face_landmarker.task")
                                .build(),
                        )
                        .setRunningMode(RunningMode.IMAGE)
                        .setNumFaces(1)
                        .setMinFaceDetectionConfidence(0.50f)
                        .setMinFacePresenceConfidence(0.50f)
                        .setMinTrackingConfidence(0.50f)
                        .build(),
                )
                val mpImage = BitmapImageBuilder(bmp).build()
                try {
                    val result = landmarker.detect(mpImage)
                    val reading = analyzeFrame(bmp, result)
                    withContext(Dispatchers.Main) {
                        latestReading = reading
                        if (reading.skinSrgb != null) {
                            detectedSkinHex = reading.skinSrgb.toHex()
                        }
                    }
                } finally {
                    mpImage.close()
                    landmarker.close()
                }
            }
        }
    }

    var selectedFabric by remember {
        mutableStateOf(if (initialFabricId != null) FabricCatalog.findById(initialFabricId) else FabricCatalog.defaultFabric)
    }
    var isFabricListExpanded by remember { mutableStateOf(false) }
    var isCustomPickerOpen by remember { mutableStateOf(false) }
    var selectedColor by remember {
        mutableStateOf(cameraColorPalette.find { it.hex.equals(initialColorHex, ignoreCase = true) } ?: cameraColorPalette[0])
    }
    var customHex by remember { mutableStateOf(initialColorHex) }
    val isFaceInFrame = photoBitmap != null || (latestReading != null && latestReading?.hasFace == true && latestReading?.skinSrgb != null && (latestReading?.faceLuminance ?: 0.0) >= 0.15)

    val activeColorHex = customHex ?: selectedColor.hex
    val storedProfile = remember { SkinProfileRepository.load(context) }
    val effectiveSkinHex = detectedSkinHex ?: storedProfile?.skinHex ?: ""
    val liveLuminance = latestReading?.faceLuminance ?: 0.50

    val harmonyResult = remember(effectiveSkinHex, activeColorHex, selectedFabric.id, liveLuminance, isFaceInFrame) {
        if (isFaceInFrame && effectiveSkinHex.isNotBlank()) {
            TrueColorHarmonyEngine.evaluate(
                skinHex = effectiveSkinHex,
                fabricHex = activeColorHex,
                fabricId = selectedFabric.id,
                ambientLuminance = liveLuminance,
            )
        } else {
            com.drapeproof.core.color.HarmonyAnalysisResult(
                scorePercent = 0,
                harmonyLabel = "Align Face",
                summaryFeedback = "Position your face in the oval to compute color harmony.",
                contrastScorePercent = 0,
                hueScorePercent = 0,
                chromaScorePercent = 0,
                reasonsList = emptyList(),
                deltaE00 = 0.0,
                deltaLuminance = 0.0,
                isFlattering = false,
            )
        }
    }

    val targetStatusColor = when {
        !isFaceInFrame -> Color.White.copy(alpha = 0.50f)
        harmonyResult.scorePercent >= 86 -> EditorialPositive
        harmonyResult.scorePercent >= 70 -> EditorialWarning
        else -> EditorialNegative
    }

    val animatedStatusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = tween(400),
        label = "statusColorAnim",
    )

    val animatedScore by animateIntAsState(
        targetValue = harmonyResult.scorePercent,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scoreAnim",
    )

    val animatedClothColor by animateColorAsState(
        targetValue = activeColorHex.asComposeColor(),
        animationSpec = tween(350),
        label = "clothColorAnim",
    )

    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(2200)
            toastMessage = null
        }
    }

    // Landmark Smoothing
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

    val activeTile = remember(selectedFabric.id) {
        FabricTextureShader.getOrLoadTile(context, selectedFabric.id)
    }

    // Infinite Looping LazyRow State
    val infiniteItemCount = 10000 * cameraColorPalette.size
    val colorListState = rememberLazyListState(initialFirstVisibleItemIndex = 5000 * cameraColorPalette.size)

    // Curtain Drop Freeze Animation States
    var capturedFreezeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCurtainDropping by remember { mutableStateOf(false) }
    val curtainDropAnim = remember { Animatable(0f) }
    val flashBurstAnim = remember { Animatable(0f) }

    var isControlsCollapsed by remember { mutableStateOf(false) }

    if (!permissionGranted && photoBitmap == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("Camera Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(8.dp))
                Text(
                    "DrapeIt projects virtual fabrics and measures live facial colorimetry in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("Upload Photo Instead", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val totalScreenHeight = maxHeight

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
                val faceCenter = Offset(width * 0.50f, height * 0.30f)
                val ovalW = width * 0.52f
                val ovalH = height * 0.32f

                drawOval(
                    color = animatedStatusColor,
                    topLeft = Offset(faceCenter.x - ovalW / 2, faceCenter.y - ovalH / 2),
                    size = Size(ovalW, ovalH),
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }
        }

        // 3. CAPTURED FRAME CURTAIN-MELT ANIMATION LAYER (ISOLATED TO CAMERA/CANVAS VIEWPORT)
        if (isCurtainDropping && capturedFreezeBitmap != null) {
            val progress = curtainDropAnim.value
            val density = LocalDensity.current
            val screenHeightPx = totalScreenHeight.value * density.density

            // 1. Subtle zoom-out in first 20%
            val zoomScale = 1.0f - (progress.coerceAtMost(0.20f) / 0.20f) * 0.05f

            // 2. Curtain melting / falling downwards from above
            val translateY = progress * screenHeightPx

            // 3. Alpha fades smoothly out in final phase
            val alpha = (1.0f - progress).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = zoomScale
                        this.scaleY = zoomScale
                        this.translationY = translateY
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                Image(
                    bitmap = capturedFreezeBitmap!!.asImageBitmap(),
                    contentDescription = "Frozen Curtain Snapshot",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // Subtle camera lens flash burst (isolated to viewport)
        if (flashBurstAnim.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashBurstAnim.value)),
            )
        }

        // 4. TOP BAR: CONCISE GUIDANCE / PHOTO RESET TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (photoBitmap != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(EditorialSienna)
                        .clickable { photoBitmap = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("✕ Live Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.68f))
                        .border(0.75.dp, EditorialGold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Face soft daylight & center face in reticle for real-time optics",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

        // 4. COMPATIBILITY PERCENTAGE BADGE ANCHORED PRECISELY BELOW THE RETICLE OVAL
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = totalScreenHeight * 0.475f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.78f))
                .border(1.5.dp, animatedStatusColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(animatedStatusColor))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isFaceInFrame) "$animatedScore% • ${harmonyResult.harmonyLabel}" else "Align face in oval • —%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }

        // 6. BOTTOM CONTROL DECK: MOVED SLIGHTLY DOWN WITH SNUG BOTTOM PADDING
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 2.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // COLLAPSE / EXPAND ARROW-ONLY TOGGLE
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                    .clickable {
                        runCatching { currentView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) }
                        isControlsCollapsed = !isControlsCollapsed
                    }
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isControlsCollapsed) "▲" else "▼",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }

            AnimatedVisibility(
                visible = !isControlsCollapsed,
                enter = fadeIn() + androidx.compose.animation.expandVertically(),
                exit = fadeOut() + androidx.compose.animation.shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ACTION ROW: [ 🖼️ Photo Upload ] + [ 📸 CAPTURE ] + [ 🧵 Fabric ]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // LEFT: PHOTO UPLOAD BUTTON
                        // LEFT: PHOTO UPLOAD BUTTON (MINIMAL GALLERY ICON)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .border(1.2.dp, Color.White.copy(alpha = 0.40f), CircleShape)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🖼️", fontSize = 20.sp)
                        }

                        // CENTER: COUTURE CONCENTRIC-RING SHUTTER BUTTON
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.60f))
                                .border(3.5.dp, EditorialSienna, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    val capturedSkinHex = detectedSkinHex ?: effectiveSkinHex

                                    val rawUserBitmap = if (photoBitmap != null) {
                                        photoBitmap
                                    } else {
                                        activePreviewView?.bitmap ?: runCatching {
                                            Bitmap.createBitmap(720, 1280, Bitmap.Config.ARGB_8888).apply {
                                                Canvas(this).drawColor(android.graphics.Color.DKGRAY)
                                            }
                                        }.getOrNull()
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
                                        val chinY = (smoothedChinY * h).coerceIn(h * 0.35f, h * 0.65f)

                                        val drapePath = AndroidPath().apply {
                                            moveTo(0f, chinY + h * 0.04f)
                                            cubicTo(
                                                w * 0.20f, chinY + h * 0.02f,
                                                chinX - w * 0.14f, chinY + h * 0.01f,
                                                chinX, chinY + h * 0.02f,
                                            )
                                            cubicTo(
                                                chinX + w * 0.14f, chinY + h * 0.01f,
                                                w * 0.80f, chinY + h * 0.02f,
                                                w, chinY + h * 0.04f,
                                            )
                                            lineTo(w, h)
                                            lineTo(0f, h)
                                            close()
                                        }

                                        // 1. Draw vibrant saturated drape base color
                                        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            color = android.graphics.Color.parseColor(activeColorHex)
                                            style = Paint.Style.FILL
                                        }
                                        compCanvas.drawPath(drapePath, basePaint)

                                        // 2. Blend subtle fabric weave texture with MULTIPLY so color remains rich
                                        val rawTile = FabricTextureShader.getOrLoadRawBitmap(context, selectedFabric.id)
                                        rawTile?.let { tile ->
                                            val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                                            val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                shader = tileShader
                                                alpha = (selectedFabric.textureAlpha * 0.30f * 255).toInt().coerceIn(25, 90)
                                                xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                                            }
                                            compCanvas.drawPath(drapePath, tilePaint)
                                        }

                                        val snap = DrapeSnapRepository.saveSnap(
                                            context = context,
                                            bitmap = comp,
                                            colorHex = activeColorHex,
                                            colorName = selectedColor.name,
                                            fabricId = selectedFabric.id,
                                            fabricName = selectedFabric.name,
                                            matchScorePercent = harmonyResult.scorePercent,
                                            skinHex = capturedSkinHex,
                                        )

                                        // Play camera shutter sound + haptic feedback
                                        SoundEffectManager.playShutter(currentView)

                                        // Trigger Flash Burst & Curtain-Melt Drop Freeze Animation
                                        capturedFreezeBitmap = comp
                                        isCurtainDropping = true
                                        scope.launch {
                                            flashBurstAnim.snapTo(0.85f)
                                            curtainDropAnim.snapTo(0f)
                                            launch {
                                                flashBurstAnim.animateTo(0f, tween(240))
                                            }
                                            curtainDropAnim.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(900, easing = FastOutSlowInEasing),
                                            )
                                            isCurtainDropping = false
                                            capturedFreezeBitmap = null
                                        }

                                        toastMessage = "📸 Look saved! Match: ${snap?.matchScorePercent ?: harmonyResult.scorePercent}%"
                                        scope.launch {
                                            kotlinx.coroutines.delay(2200)
                                            toastMessage = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(EditorialSienna),
                            )
                        }

                        // RIGHT: FABRIC MATERIAL BUTTON WITH EMBEDDED TEXT
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.5.dp, EditorialGold.copy(alpha = 0.70f), CircleShape)
                                .clickable {
                                    runCatching { currentView.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK) }
                                    isFabricListExpanded = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(selectedFabric.icon, fontSize = 16.sp)
                                Text(
                                    "Fabric",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // BOTTOM-MOST: INFINITE CONTINUOUS LOOPING COLOR SWATCHES
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // PERMANENT COLOR PICKER BUTTON (FIXED ON LEFT)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.75f))
                                .border(1.5.dp, Color.White, CircleShape)
                                .clickable { isCustomPickerOpen = true },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🎨", fontSize = 18.sp)
                        }

                        Spacer(Modifier.width(8.dp))

                        // ENDLESS INFINITE HORIZONTAL SWATCHES
                        LazyRow(
                            state = colorListState,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            items(infiniteItemCount) { index ->
                                val item = cameraColorPalette[index % cameraColorPalette.size]
                                val isSelected = activeColorHex.equals(item.hex, ignoreCase = true)
                                val animScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.22f else 1.0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "swatchScale",
                                )

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .scale(animScale)
                                        .clip(CircleShape)
                                        .background(item.hex.asComposeColor())
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.35f),
                                            shape = CircleShape,
                                        )
                                        .clickable {
                                            runCatching { currentView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
                                            selectedColor = item
                                            customHex = null
                                        },
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. TOAST NOTIFICATION BADGE
        if (toastMessage != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(toastMessage!!, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 8. FABRIC SELECTION MODAL SHEET (TOP-LEVEL Z-INDEX OVERLAY)
        if (isFabricListExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { isFabricListExpanded = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Choose Fabric Texture", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { isFabricListExpanded = false },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        val chunked = FabricCatalog.allFabrics.chunked(3)
                        chunked.forEach { rowFabrics ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowFabrics.forEach { fab ->
                                    val isSel = fab.id == selectedFabric.id
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSel) EditorialGold.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
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
                                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
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

        // 9. COLOR PICKER DIALOG
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
