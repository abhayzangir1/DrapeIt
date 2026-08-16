package com.drapeproof.mobile.tryon

import android.content.Context
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SkinProfileRepository
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.avatar.AvatarLighting
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.avatar.SavedAvatar
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.fabric.FabricTextureShader
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.network.RemoteTaskResult
import com.drapeproof.mobile.network.UploadInput
import com.drapeproof.mobile.profile.ProfileSettingsModal
import com.drapeproof.mobile.ui.UniversalColorPickerDialog
import com.drapeproof.mobile.ui.components.FullScreenImageViewerModal
import com.drapeproof.mobile.ui.sound.SoundEffectManager
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSienna
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.UUID

enum class GarmentCategory(val title: String, val apiValue: String, val icon: String) {
    UPPER_BODY("Upper Body", "upper_body", "👕"),
    LOWER_BODY("Lower Body", "lower_body", "👖"),
    FULL_BODY("Full Body", "full_body", "👗"),
}

enum class GarmentSilhouette(
    val id: String,
    val displayName: String,
    val icon: String,
    val category: GarmentCategory,
    val genderTarget: String, // "female", "male", "unisex"
) {
    // WOMEN
    BLOUSE("blouse", "Silk Blouse", "👚", GarmentCategory.UPPER_BODY, "female"),
    WOMEN_TSHIRT("w_tshirt", "Fitted T-Shirt", "👕", GarmentCategory.UPPER_BODY, "female"),
    WOMEN_BLAZER("w_blazer", "Tailored Blazer", "🧥", GarmentCategory.UPPER_BODY, "female"),
    WOMEN_CARDIGAN("w_cardigan", "Knit Cardigan", "🧶", GarmentCategory.UPPER_BODY, "female"),
    DRESS("dress", "A-Line Midi Dress", "👗", GarmentCategory.FULL_BODY, "female"),
    GOWN("gown", "Evening Gown", "👗", GarmentCategory.FULL_BODY, "female"),
    SKIRT("skirt", "Pleated Midi Skirt", "🩳", GarmentCategory.LOWER_BODY, "female"),
    WOMEN_TROUSERS("w_pants", "High-Waist Trousers", "👖", GarmentCategory.LOWER_BODY, "female"),
    WOMEN_JEANS("w_jeans", "Slim Denim Jeans", "👖", GarmentCategory.LOWER_BODY, "female"),

    // MEN
    SHIRT("shirt", "Button-Down Shirt", "👔", GarmentCategory.UPPER_BODY, "male"),
    TSHIRT("tshirt", "Crewneck T-Shirt", "👕", GarmentCategory.UPPER_BODY, "male"),
    BLAZER("blazer", "Structured Blazer", "🧥", GarmentCategory.UPPER_BODY, "male"),
    SWEATER("sweater", "Ribbed Sweater", "🧶", GarmentCategory.UPPER_BODY, "male"),
    POLO("polo", "Classic Polo Shirt", "🎽", GarmentCategory.UPPER_BODY, "male"),
    PANTS("pants", "Tailored Trousers", "👖", GarmentCategory.LOWER_BODY, "male"),
    MEN_JEANS("m_jeans", "Straight Denim Jeans", "👖", GarmentCategory.LOWER_BODY, "male"),
    MEN_SHORTS("m_shorts", "Chino Shorts", "🩳", GarmentCategory.LOWER_BODY, "male"),
}

enum class TryOnInputSource(val title: String, val subtitle: String) {
    GARMENT("Garment", "Upload person & garment"),
    STYLE("Style", "Choose cut, color & fabric"),
    SWAP("Swap", "Instant topwear color swap"),
}

fun generateReferenceGarmentBitmap(
    context: Context,
    silhouette: GarmentSilhouette,
    colorHex: String,
    fabric: FabricMaterial,
    gender: String,
): Bitmap {
    val width = 640
    val height = 800
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    // Base background clean off-white canvas
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(colorHex)
        style = Paint.Style.FILL
    }

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    val path = Path()

    when (silhouette.category) {
        GarmentCategory.FULL_BODY -> {
            // Dress silhouette
            path.moveTo(width * 0.35f, height * 0.15f)
            path.lineTo(width * 0.20f, height * 0.22f)
            path.lineTo(width * 0.25f, height * 0.30f)
            path.lineTo(width * 0.35f, height * 0.28f)
            path.lineTo(width * 0.32f, height * 0.45f) // Waist
            path.lineTo(width * 0.15f, height * 0.85f) // Flare bottom left
            path.lineTo(width * 0.85f, height * 0.85f) // Flare bottom right
            path.lineTo(width * 0.68f, height * 0.45f) // Waist right
            path.lineTo(width * 0.65f, height * 0.28f)
            path.lineTo(width * 0.75f, height * 0.30f)
            path.lineTo(width * 0.80f, height * 0.22f)
            path.lineTo(width * 0.65f, height * 0.15f)
            path.quadTo(width * 0.50f, height * 0.22f, width * 0.35f, height * 0.15f) // Collar neckline
            path.close()
        }
        GarmentCategory.LOWER_BODY -> {
            // Pants/Trousers silhouette
            path.moveTo(width * 0.25f, height * 0.15f)
            path.lineTo(width * 0.75f, height * 0.15f)
            path.lineTo(width * 0.80f, height * 0.88f)
            path.lineTo(width * 0.58f, height * 0.88f)
            path.lineTo(width * 0.50f, height * 0.42f) // Crotch
            path.lineTo(width * 0.42f, height * 0.88f)
            path.lineTo(width * 0.20f, height * 0.88f)
            path.close()
        }
        GarmentCategory.UPPER_BODY -> {
            // Shirts / Blazer / Tops
            path.moveTo(width * 0.33f, height * 0.15f)
            path.lineTo(width * 0.12f, height * 0.25f)
            path.lineTo(width * 0.18f, height * 0.38f)
            path.lineTo(width * 0.28f, height * 0.32f)
            path.lineTo(width * 0.26f, height * 0.85f)
            path.lineTo(width * 0.74f, height * 0.85f)
            path.lineTo(width * 0.72f, height * 0.32f)
            path.lineTo(width * 0.82f, height * 0.38f)
            path.lineTo(width * 0.88f, height * 0.25f)
            path.lineTo(width * 0.67f, height * 0.15f)
            path.quadTo(width * 0.50f, height * 0.24f, width * 0.33f, height * 0.15f)
            path.close()
        }
    }

    canvas.drawPath(path, paint)
    canvas.drawPath(path, strokePaint)

    // Apply procedural textile texture overlay
    val textureBmp = FabricTextureShader.getOrLoadRawBitmap(context, fabric.id)
    if (textureBmp != null) {
        val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            alpha = 140
        }
        canvas.drawBitmap(textureBmp, null, Rect(0, 0, width, height), blendPaint)
    }

    return bmp
}

fun applyInstantColorSwap(sourceBitmap: Bitmap, targetHex: String): Bitmap {
    val width = sourceBitmap.width
    val height = sourceBitmap.height
    val resultBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(resultBmp)

    // Draw base image
    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

    // Create upper body color overlay with soft-light blending
    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(targetHex)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        alpha = 140
    }

    // Mask upper chest area (y: 28% to 75%, x: 18% to 82%)
    val chestPath = Path().apply {
        addOval(
            width * 0.18f,
            height * 0.30f,
            width * 0.82f,
            height * 0.76f,
            Path.Direction.CW,
        )
    }

    canvas.save()
    canvas.clipPath(chestPath)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    canvas.restore()

    return resultBmp
}

@Composable
fun TryOnScreen(
    initialFabricId: String = "silk",
    initialColorHex: String = "#831843",
    initialGarmentUri: Uri? = null,
    initialSnapId: String? = null,
    onNavigateToLooks: () -> Unit = {},
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cut: String) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val currentView = LocalView.current
    val scope = rememberCoroutineScope()

    var inputSource by remember {
        mutableStateOf(if (initialGarmentUri != null) TryOnInputSource.GARMENT else TryOnInputSource.GARMENT)
    }
    var selectedFabric by remember { mutableStateOf(FabricCatalog.findById(initialFabricId)) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex) }
    var selectedSilhouette by remember { mutableStateOf(GarmentSilhouette.BLOUSE) }

    var customGarmentUri by remember { mutableStateOf<Uri?>(initialGarmentUri) }
    var rawGarmentBitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var isCropperOpen by remember { mutableStateOf(false) }

    var activeAvatar by remember { mutableStateOf<SavedAvatar?>(PhotoAvatarStore.getActiveAvatar(context)) }
    var isSilhouetteModalOpen by remember { mutableStateOf(false) }
    var isColorPickerOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf<String?>(null) }
    var tryOnResultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedOutfitId by remember { mutableStateOf<String?>(null) }
    var fullScreenPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var showAvatarPromptDialog by remember { mutableStateOf(false) }
    var showGarmentPromptDialog by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val saved = PhotoAvatarStore.saveAvatarFromUri(
                context = context,
                sourceUri = uri,
                name = "Headshot",
                lighting = AvatarLighting.DAYLIGHT,
                skinHex = SkinProfileRepository.load(context)?.skinHex ?: "#D8B498",
            )
            if (saved != null) {
                activeAvatar = saved
            }
        }
    }

    val garmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        rawGarmentBitmapToCrop = bmp
                        isCropperOpen = true
                    }
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulseAnim")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha",
    )

    fun resetState() {
        activeAvatar = null
        customGarmentUri = null
        tryOnResultBitmap = null
        savedOutfitId = null
        generationStatus = null
        isGenerating = false
    }

    fun executeGeneration() {
        if (activeAvatar == null) {
            showAvatarPromptDialog = true
            return
        }

        val avatarFile = File(activeAvatar!!.imagePath)
        if (!avatarFile.exists()) {
            showAvatarPromptDialog = true
            return
        }

        val avatarBmp = BitmapFactory.decodeFile(avatarFile.absolutePath) ?: return

        isGenerating = true
        generationStatus = "Initializing Neural Clothes V3 Pipeline…"
        savedOutfitId = null

        scope.launch(Dispatchers.IO) {
            try {
                if (inputSource == TryOnInputSource.SWAP) {
                    withContext(Dispatchers.Main) { generationStatus = "Applying color swap…" }
                    val swapBmp = applyInstantColorSwap(avatarBmp, selectedColorHex)

                    val realSkinHex = SkinProfileRepository.load(context)?.skinHex ?: "#D8B498"
                    val swapHarmony = TrueColorHarmonyEngine.evaluate(realSkinHex, selectedColorHex, selectedFabric.id)
                    val swapScore = swapHarmony.scorePercent

                    // Auto-save outfit to WardrobeRepository
                    val outfit = SavedTryOnOutfit(
                        title = "Color Swap ($selectedColorHex)",
                        fabricName = selectedFabric.name,
                        colorHex = selectedColorHex,
                        topwearCut = selectedSilhouette.displayName,
                        bottomwearCut = "Standard",
                        bottomwearColor = "",
                        resultImagePath = null,
                        matchScorePercent = swapScore,
                    )
                    WardrobeRepository.addOutfit(context, outfit)

                    DrapeSnapRepository.saveSnap(
                        context = context,
                        bitmap = swapBmp,
                        colorHex = selectedColorHex,
                        colorName = "Custom Swap",
                        fabricId = selectedFabric.id,
                        fabricName = selectedFabric.name,
                        matchScorePercent = swapScore,
                        skinHex = realSkinHex,
                    )

                    withContext(Dispatchers.Main) {
                        tryOnResultBitmap = swapBmp
                        isGenerating = false
                        generationStatus = null
                    }
                    return@launch
                }

                val finalGarmentBmp: Bitmap = if (inputSource == TryOnInputSource.GARMENT && customGarmentUri != null) {
                    val stream = context.contentResolver.openInputStream(customGarmentUri!!)
                    BitmapFactory.decodeStream(stream) ?: generateReferenceGarmentBitmap(context, selectedSilhouette, selectedColorHex, selectedFabric, "unisex")
                } else {
                    withContext(Dispatchers.Main) { generationStatus = "Synthesizing ${selectedFabric.name} ${selectedSilhouette.displayName}…" }
                    generateReferenceGarmentBitmap(
                        context = context,
                        silhouette = selectedSilhouette,
                        colorHex = selectedColorHex,
                        fabric = selectedFabric,
                        gender = selectedSilhouette.genderTarget,
                    )
                }

                val avatarStream = ByteArrayOutputStream().apply { avatarBmp.compress(Bitmap.CompressFormat.JPEG, 92, this) }
                val garmentStream = ByteArrayOutputStream().apply { finalGarmentBmp.compress(Bitmap.CompressFormat.JPEG, 92, this) }
                val avatarBytes = avatarStream.toByteArray()
                val garmentBytes = garmentStream.toByteArray()

                val api = DrapeProofApiClient()
                val ok = api.ensureSession("drapeit-client-2026")
                if (!ok) throw Exception("Could not establish secure session with YouCam Cloud.")

                withContext(Dispatchers.Main) { generationStatus = "Uploading portrait & garment…" }

                val tickets = api.requestUploadTickets(
                    feature = "try-on",
                    inputs = listOf(
                        UploadInput(contentType = "image/jpeg", fileName = "person.jpg", bytes = avatarBytes),
                        UploadInput(contentType = "image/jpeg", fileName = "garment.jpg", bytes = garmentBytes),
                    ),
                )
                api.upload(tickets[0], avatarBytes)
                api.upload(tickets[1], garmentBytes)

                withContext(Dispatchers.Main) { generationStatus = "Fitting garment with YouCam AI…" }

                val task = api.startTryOn(
                    sourceFileId = tickets[0].fileId,
                    referenceFileId = tickets[1].fileId,
                    garmentCategory = selectedSilhouette.category.apiValue,
                    provider = "clothes",
                    gender = selectedSilhouette.genderTarget,
                    operationId = UUID.randomUUID().toString(),
                )

                var attempts = 0
                var finalResultUrl: String? = null

                while (attempts < 30) {
                    delay(2000)
                    attempts++
                    withContext(Dispatchers.Main) { generationStatus = "Neural drape in progress (${attempts * 2}s)…" }
                    val pollRes = api.poll("try-on", task.taskId)
                    when (pollRes) {
                        is RemoteTaskResult.Running -> { /* continue */ }
                        is RemoteTaskResult.TryOnImage -> {
                            finalResultUrl = pollRes.imageUrl
                            break
                        }
                        is RemoteTaskResult.Failed -> {
                            throw Exception("${pollRes.code}: ${pollRes.message}")
                        }
                        else -> {}
                    }
                }

                if (finalResultUrl != null) {
                    withContext(Dispatchers.Main) { generationStatus = "Downloading photorealistic look…" }
                    val urlStream = URL(finalResultUrl).openStream()
                    val downloadedBmp = BitmapFactory.decodeStream(urlStream)

                    val vtoSkinHex = SkinProfileRepository.load(context)?.skinHex ?: "#D8B498"
                    val vtoHarmony = TrueColorHarmonyEngine.evaluate(vtoSkinHex, selectedColorHex, selectedFabric.id)
                    val vtoScore = vtoHarmony.scorePercent

                    // Auto-save outfit to WardrobeRepository and DrapeSnapRepository
                    val outfit = SavedTryOnOutfit(
                        title = "${selectedFabric.name} ${selectedSilhouette.displayName}",
                        fabricName = selectedFabric.name,
                        colorHex = selectedColorHex,
                        topwearCut = selectedSilhouette.displayName,
                        bottomwearCut = "Tailored",
                        bottomwearColor = "",
                        resultImagePath = null,
                        matchScorePercent = vtoScore,
                    )
                    WardrobeRepository.addOutfit(context, outfit)

                    DrapeSnapRepository.saveSnap(
                        context = context,
                        bitmap = downloadedBmp,
                        colorHex = selectedColorHex,
                        colorName = selectedSilhouette.displayName,
                        fabricId = selectedFabric.id,
                        fabricName = selectedFabric.name,
                        matchScorePercent = vtoScore,
                        skinHex = vtoSkinHex,
                    )

                    withContext(Dispatchers.Main) {
                        tryOnResultBitmap = downloadedBmp
                        isGenerating = false
                        generationStatus = null
                    }
                } else {
                    throw Exception("Try-on generation timed out. Please retry.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isGenerating = false
                    generationStatus = "Try-on error: ${e.message}"
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(18.dp))

            // TOP HEADER BAR WITH SETTINGS ICON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Try-On",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Virtual photorealistic wardrobe studio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .clickable {
                            SoundEffectManager.playTap(currentView)
                            isSettingsOpen = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            // MODE SWITCHER TABS: [ Garment ] [ Style ] [ Swap ]
            SecondaryTabRow(
                selectedTabIndex = inputSource.ordinal,
                containerColor = Color.Transparent,
                contentColor = EditorialSienna,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(inputSource.ordinal),
                        color = EditorialSienna,
                        height = 3.dp,
                    )
                },
                divider = {},
            ) {
                TryOnInputSource.values().forEach { tab ->
                    val isSelected = inputSource == tab
                    Tab(
                        selected = isSelected,
                        onClick = {
                            SoundEffectManager.playTap(currentView)
                            inputSource = tab
                        },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EditorialSienna else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            // TWO PROMINENT LARGE SELECTION BOXES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // BOX 1: PERSON IMAGE
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(185.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    val activeAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                    val avatarBmp = if (activeAvatarFile?.exists() == true) BitmapFactory.decodeFile(activeAvatarFile.absolutePath) else null

                    if (avatarBmp != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = avatarBmp.asImageBitmap(),
                                contentDescription = "Your Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.75f))
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Text(
                                    "🔄",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        SoundEffectManager.playTap(currentView)
                                        val rotated = com.drapeproof.mobile.util.ImageExportUtils.rotateBitmap(avatarBmp)
                                        activeAvatarFile?.let { f ->
                                            java.io.FileOutputStream(f).use { rotated.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                                            activeAvatar = activeAvatar?.copy(imagePath = f.absolutePath)
                                        }
                                    },
                                )
                                Text(
                                    "Replace",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        SoundEffectManager.playTap(currentView)
                                        avatarPicker.launch("image/*")
                                    },
                                )
                                Text(
                                    "✕",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.clickable {
                                        SoundEffectManager.playTap(currentView)
                                        activeAvatar?.let { av ->
                                            PhotoAvatarStore.deleteAvatar(context, av.id)
                                        }
                                        PhotoAvatarStore.clearActiveAvatar(context)
                                        activeAvatar = null
                                    },
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    SoundEffectManager.playTap(currentView)
                                    avatarPicker.launch("image/*")
                                }
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📤", fontSize = 32.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Upload Person",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // BOX 2: GARMENT / SILHOUETTE / INTEGRATED COLOR CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(185.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    when (inputSource) {
                        TryOnInputSource.GARMENT -> {
                            val garmentBmp = if (customGarmentUri != null) {
                                runCatching { context.contentResolver.openInputStream(customGarmentUri!!)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
                            } else null

                            if (garmentBmp != null) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        bitmap = garmentBmp.asImageBitmap(),
                                        contentDescription = "Uploaded Dress",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.75f))
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        Text(
                                            "🔄",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.clickable {
                                                SoundEffectManager.playTap(currentView)
                                                val rotated = com.drapeproof.mobile.util.ImageExportUtils.rotateBitmap(garmentBmp)
                                                val tempFile = java.io.File(context.cacheDir, "garment_rot_${System.currentTimeMillis()}.jpg")
                                                java.io.FileOutputStream(tempFile).use { rotated.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                                                customGarmentUri = Uri.fromFile(tempFile)
                                            },
                                        )
                                        Text(
                                            "✂️ Crop",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.clickable {
                                                SoundEffectManager.playTap(currentView)
                                                rawGarmentBitmapToCrop = garmentBmp
                                                isCropperOpen = true
                                            },
                                        )
                                        Text(
                                            "Replace",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.clickable {
                                                SoundEffectManager.playTap(currentView)
                                                garmentPicker.launch("image/*")
                                            },
                                        )
                                        Text(
                                            "✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171),
                                            modifier = Modifier.clickable {
                                                SoundEffectManager.playTap(currentView)
                                                customGarmentUri = null
                                            },
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            SoundEffectManager.playTap(currentView)
                                            garmentPicker.launch("image/*")
                                        }
                                        .padding(14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("👔", fontSize = 28.sp)
                                            Spacer(Modifier.width(4.dp))
                                            Text("📤", fontSize = 20.sp)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "Upload Garment",
                                            textAlign = TextAlign.Center,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text("Prompts to crop", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        TryOnInputSource.STYLE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        SoundEffectManager.playTap(currentView)
                                        isSilhouetteModalOpen = true
                                    }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(selectedSilhouette.icon, fontSize = 36.sp)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "Choose Silhouette",
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(selectedSilhouette.displayName, fontSize = 11.sp, color = EditorialSienna, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        TryOnInputSource.SWAP -> {
                            // INTEGRATED TARGET COLOR CARD: TOP 35% SOLID FILL, BOTTOM 65% SWATCHES & COLOR WHEEL
                            Column(modifier = Modifier.fillMaxSize()) {
                                // TOP 35%: SOLID TARGET COLOR BAR
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.38f)
                                        .background(selectedColorHex.asComposeColor())
                                        .clickable {
                                            SoundEffectManager.playTap(currentView)
                                            isColorPickerOpen = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "Target: ${selectedColorHex.uppercase()}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                    )
                                }

                                // BOTTOM 65%: COLOR SWATCHES & PERMANENT WHEEL BUTTON
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(0.62f)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                                            .clickable {
                                                SoundEffectManager.playTap(currentView)
                                                isColorPickerOpen = true
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("🎨", fontSize = 16.sp)
                                    }

                                    Spacer(Modifier.width(6.dp))

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        listOf("#831843", "#1E3A8A", "#065F46", "#78350F", "#4C1D95", "#0F172A", "#D97706", "#2563EB", "#E11D48").forEach { hex ->
                                            val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(hex.asComposeColor())
                                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                                                    .clickable {
                                                        SoundEffectManager.playTap(currentView)
                                                        selectedColorHex = hex
                                                    },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // DETAIL CONTROLS ONLY FOR STYLE (COLOR & FABRIC) TAB
            if (inputSource == TryOnInputSource.STYLE) {
                // 1. COLOR SELECTION CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "COLOR PALETTE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialSienna,
                                letterSpacing = 1.2.sp,
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EditorialSienna.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    selectedColorHex.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialSienna,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, EditorialSienna, CircleShape)
                                    .clickable {
                                        SoundEffectManager.playTap(currentView)
                                        isColorPickerOpen = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🎨", fontSize = 18.sp)
                            }

                            listOf("#831843", "#1E3A8A", "#065F46", "#78350F", "#4C1D95", "#0F172A", "#9A3412", "#0E7490", "#D97706", "#2563EB", "#059669", "#E11D48", "#475569").forEach { hex ->
                                val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(if (isSel) 3.dp else 1.dp, if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                                        .clickable {
                                            SoundEffectManager.playTap(currentView)
                                            selectedColorHex = hex
                                        },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 2. FABRIC MATERIAL TEXTURE CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "FABRIC TEXTURE & WEAVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialSienna,
                                letterSpacing = 1.2.sp,
                            )
                            Text(
                                selectedFabric.name,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FabricCatalog.allFabrics.forEach { fab ->
                                val isSel = selectedFabric.id == fab.id
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) EditorialSienna.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    modifier = Modifier
                                        .width(115.dp)
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .clickable {
                                            SoundEffectManager.playTap(currentView)
                                            selectedFabric = fab
                                        },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(fab.icon, fontSize = 24.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            fab.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSel) EditorialSienna else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            fab.weaveType,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // RESULT CARD & TRY-ON CTA
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (tryOnResultBitmap != null) {
                        Text(
                            "TRY-ON RESULT (TAP TO ENLARGE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EditorialPositive,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Image(
                            bitmap = tryOnResultBitmap!!.asImageBitmap(),
                            contentDescription = "Try-On Result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    SoundEffectManager.playTap(currentView)
                                    fullScreenPreviewBitmap = tryOnResultBitmap
                                },
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        val activeAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                        val avatarBmp = if (activeAvatarFile?.exists() == true) BitmapFactory.decodeFile(activeAvatarFile.absolutePath) else null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .graphicsLayer { if (isGenerating) alpha = pulseGlow },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (avatarBmp != null) {
                                Image(
                                    bitmap = avatarBmp.asImageBitmap(),
                                    contentDescription = "Avatar Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            }

                            if (isGenerating) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.65f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            generationStatus ?: "Generating Try-On…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            } else if (avatarBmp == null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👔", fontSize = 42.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Upload photo to begin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val needsGarment = inputSource == TryOnInputSource.GARMENT && customGarmentUri == null
                    val needsAvatar = activeAvatar == null

                    val ctaText = when {
                        isGenerating -> "Generating…"
                        needsAvatar -> "Upload Person"
                        needsGarment -> "Upload Garment"
                        inputSource == TryOnInputSource.SWAP -> "Swap Color"
                        inputSource == TryOnInputSource.STYLE -> "Try On ${selectedSilhouette.displayName}"
                        else -> "Generate Try-On"
                    }

                    Button(
                        onClick = {
                            if (needsAvatar) {
                                SoundEffectManager.playTap(currentView)
                                showAvatarPromptDialog = true
                                return@Button
                            }
                            if (needsGarment) {
                                SoundEffectManager.playTap(currentView)
                                showGarmentPromptDialog = true
                                return@Button
                            }
                            SoundEffectManager.playTap(currentView)
                            executeGeneration()
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text(ctaText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (tryOnResultBitmap != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // DOWNLOAD / SAVE TO GALLERY BUTTON
                            OutlinedButton(
                                onClick = {
                                    tryOnResultBitmap?.let { bmp ->
                                        SoundEffectManager.playTap(currentView)
                                        com.drapeproof.mobile.util.ImageExportUtils.saveImageToGallery(
                                            context = context,
                                            bitmap = bmp,
                                            title = "DrapeIt_TryOn_${System.currentTimeMillis()}",
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("💾 Save", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }

                            // RESET / TRY ANOTHER BUTTON
                            OutlinedButton(
                                onClick = {
                                    SoundEffectManager.playTap(currentView)
                                    resetState()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Try Another", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }

                            // VIEW IN LOOKS BUTTON
                            Button(
                                onClick = {
                                    SoundEffectManager.playTap(currentView)
                                    onNavigateToLooks()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.weight(1.1f),
                            ) {
                                Text("View Looks", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // SETTINGS MODAL
        if (isSettingsOpen) {
            ProfileSettingsModal(
                onDismiss = { isSettingsOpen = false },
            )
        }

        // SILHOUETTE & CUT SELECTION MODAL
        if (isSilhouetteModalOpen) {
            AlertDialog(
                onDismissRequest = { isSilhouetteModalOpen = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Choose Silhouette & Cut", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { isSilhouetteModalOpen = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        // GIRLS / WOMEN SECTION
                        Text("👧 FOR GIRLS & WOMEN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = EditorialSienna)
                        Spacer(Modifier.height(8.dp))

                        val femaleSilhouettes = GarmentSilhouette.values().filter { it.genderTarget == "female" }
                        femaleSilhouettes.forEach { sil ->
                            val isSel = selectedSilhouette == sil
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        SoundEffectManager.playTap(currentView)
                                        selectedSilhouette = sil
                                        isSilhouetteModalOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(sil.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(sil.displayName, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    Text(sil.category.title, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // BOYS / MEN SECTION
                        Text("👦 FOR BOYS & MEN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = EditorialSienna)
                        Spacer(Modifier.height(8.dp))

                        val maleSilhouettes = GarmentSilhouette.values().filter { it.genderTarget == "male" }
                        maleSilhouettes.forEach { sil ->
                            val isSel = selectedSilhouette == sil
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        SoundEffectManager.playTap(currentView)
                                        selectedSilhouette = sil
                                        isSilhouetteModalOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(sil.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(sil.displayName, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                    Text(sil.category.title, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
            )
        }

        // CROPPER MODAL
        if (isCropperOpen && rawGarmentBitmapToCrop != null) {
            GarmentCropperModal(
                sourceBitmap = rawGarmentBitmapToCrop!!,
                onDismiss = { isCropperOpen = false; rawGarmentBitmapToCrop = null },
                onCropped = { file ->
                    customGarmentUri = Uri.fromFile(file)
                    inputSource = TryOnInputSource.GARMENT
                    isCropperOpen = false
                    rawGarmentBitmapToCrop = null
                },
            )
        }

        // COLOR PICKER DIALOG
        if (isColorPickerOpen) {
            UniversalColorPickerDialog(
                initialColorHex = selectedColorHex,
                onDismiss = { isColorPickerOpen = false },
                onColorSelected = { hex, _ ->
                    selectedColorHex = hex
                    isColorPickerOpen = false
                },
            )
        }

        // PROMPT DIALOGS
        if (showAvatarPromptDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarPromptDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Upload Person Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Please select a clear photo of yourself to begin virtual try-on.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showAvatarPromptDialog = false
                            avatarPicker.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text("Choose Photo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAvatarPromptDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        }

        if (showGarmentPromptDialog) {
            AlertDialog(
                onDismissRequest = { showGarmentPromptDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Upload Garment", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("Please upload a photo of the clothing item you want to try on.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showGarmentPromptDialog = false
                            garmentPicker.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text("Choose Garment", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showGarmentPromptDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        }

        if (fullScreenPreviewBitmap != null) {
            FullScreenImageViewerModal(
                bitmap = fullScreenPreviewBitmap!!,
                title = "Try-On Result",
                subtitle = "${selectedFabric.name} • $selectedColorHex",
                onDismiss = { fullScreenPreviewBitmap = null },
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
