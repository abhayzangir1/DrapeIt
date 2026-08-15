package com.drapeproof.mobile.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.BitmapShader
import android.graphics.Shader
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.drapeproof.mobile.ui.components.FullScreenImageViewerModal
import com.drapeproof.mobile.ui.UniversalColorPickerDialog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning
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
    canvas.drawColor(android.graphics.Color.WHITE)

    val garmentPath = Path()
    val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(colorHex)
        style = Paint.Style.FILL
    }

    when (silhouette.category) {
        GarmentCategory.FULL_BODY -> {
            garmentPath.moveTo(width * 0.35f, height * 0.12f)
            garmentPath.lineTo(width * 0.20f, height * 0.16f)
            garmentPath.lineTo(width * 0.12f, height * 0.38f)
            garmentPath.lineTo(width * 0.24f, height * 0.40f)
            garmentPath.lineTo(width * 0.28f, height * 0.32f)
            garmentPath.lineTo(width * 0.22f, height * 0.88f)
            garmentPath.lineTo(width * 0.78f, height * 0.88f)
            garmentPath.lineTo(width * 0.72f, height * 0.32f)
            garmentPath.lineTo(width * 0.76f, height * 0.40f)
            garmentPath.lineTo(width * 0.88f, height * 0.38f)
            garmentPath.lineTo(width * 0.80f, height * 0.16f)
            garmentPath.lineTo(width * 0.65f, height * 0.12f)
            garmentPath.cubicTo(width * 0.58f, height * 0.22f, width * 0.42f, height * 0.22f, width * 0.35f, height * 0.12f)
            garmentPath.close()
        }
        GarmentCategory.UPPER_BODY -> {
            garmentPath.moveTo(width * 0.34f, height * 0.14f)
            garmentPath.lineTo(width * 0.18f, height * 0.20f)
            garmentPath.lineTo(width * 0.08f, height * 0.42f)
            garmentPath.lineTo(width * 0.22f, height * 0.46f)
            garmentPath.lineTo(width * 0.26f, height * 0.35f)
            garmentPath.lineTo(width * 0.24f, height * 0.76f)
            garmentPath.lineTo(width * 0.76f, height * 0.76f)
            garmentPath.lineTo(width * 0.74f, height * 0.35f)
            garmentPath.lineTo(width * 0.78f, height * 0.46f)
            garmentPath.lineTo(width * 0.92f, height * 0.42f)
            garmentPath.lineTo(width * 0.82f, height * 0.20f)
            garmentPath.lineTo(width * 0.66f, height * 0.14f)
            garmentPath.cubicTo(width * 0.58f, height * 0.24f, width * 0.42f, height * 0.24f, width * 0.34f, height * 0.14f)
            garmentPath.close()
        }
        GarmentCategory.LOWER_BODY -> {
            garmentPath.moveTo(width * 0.28f, height * 0.22f)
            garmentPath.lineTo(width * 0.72f, height * 0.22f)
            garmentPath.lineTo(width * 0.76f, height * 0.86f)
            garmentPath.lineTo(width * 0.56f, height * 0.86f)
            garmentPath.lineTo(width * 0.50f, height * 0.45f)
            garmentPath.lineTo(width * 0.44f, height * 0.86f)
            garmentPath.lineTo(width * 0.24f, height * 0.86f)
            garmentPath.close()
        }
    }

    canvas.drawPath(garmentPath, basePaint)

    val rawTile = FabricTextureShader.getOrLoadRawBitmap(context, fabric.id)
    rawTile?.let { tile ->
        val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = tileShader
            alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        }
        canvas.drawPath(garmentPath, tilePaint)
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = android.graphics.Color.argb(80, 0, 0, 0)
    }
    canvas.drawPath(garmentPath, shadowPaint)

    return bmp
}

fun applyInstantColorSwap(sourceBitmap: Bitmap, targetHex: String): Bitmap {
    val result = Bitmap.createBitmap(sourceBitmap.width, sourceBitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(sourceBitmap, 0f, 0f, null)

    val targetColor = android.graphics.Color.parseColor(targetHex)
    val tr = android.graphics.Color.red(targetColor) / 255f
    val tg = android.graphics.Color.green(targetColor) / 255f
    val tb = android.graphics.Color.blue(targetColor) / 255f

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            tr * 1.5f, 0f, 0f, 0f, 0f,
            0f, tg * 1.5f, 0f, 0f, 0f,
            0f, 0f, tb * 1.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    val w = sourceBitmap.width.toFloat()
    val h = sourceBitmap.height.toFloat()

    val torsoPath = Path().apply {
        moveTo(w * 0.15f, h * 0.42f)
        cubicTo(w * 0.25f, h * 0.38f, w * 0.75f, h * 0.38f, w * 0.85f, h * 0.42f)
        lineTo(w * 0.90f, h * 0.85f)
        lineTo(w * 0.10f, h * 0.85f)
        close()
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    canvas.save()
    canvas.clipPath(torsoPath)
    canvas.drawBitmap(sourceBitmap, 0f, 0f, paint)
    canvas.restore()

    return result
}

@Composable
fun TryOnScreen(
    initialFabricId: String? = null,
    initialColorHex: String? = null,
    initialGarmentUri: Uri? = null,
    onNavigateToLooks: () -> Unit = {},
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, category: String) -> Unit = { _, _, _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputSource by remember {
        mutableStateOf(
            if (initialGarmentUri != null) TryOnInputSource.GARMENT else if (initialFabricId != null) TryOnInputSource.STYLE else TryOnInputSource.GARMENT,
        )
    }

    // Avatar starts null so user can upload directly
    var activeAvatar by remember { mutableStateOf<SavedAvatar?>(null) }
    var customGarmentUri by remember { mutableStateOf(initialGarmentUri) }

    var selectedSilhouette by remember { mutableStateOf(GarmentSilhouette.SHIRT) }
    var isSilhouetteModalOpen by remember { mutableStateOf(false) }

    var selectedFabric by remember {
        mutableStateOf(if (initialFabricId != null) FabricCatalog.findById(initialFabricId) else FabricCatalog.defaultFabric)
    }
    var selectedColorHex by remember { mutableStateOf(initialColorHex ?: "#831843") }
    var isColorPickerOpen by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf<String?>(null) }
    var tryOnResultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullScreenPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedOutfitId by remember { mutableStateOf<String?>(null) }

    var showAvatarPromptDialog by remember { mutableStateOf(false) }
    var showGarmentPromptDialog by remember { mutableStateOf(false) }

    // Cropper modal state
    var isCropperOpen by remember { mutableStateOf(false) }
    var rawGarmentBitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val saved = PhotoAvatarStore.saveAvatarFromBitmap(
                            context = context,
                            bitmap = bmp,
                            name = "My Portrait",
                            lighting = AvatarLighting.DAYLIGHT,
                            skinHex = "#D8B498",
                        )
                        activeAvatar = saved
                    }
                }
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

                    // Auto-save outfit to WardrobeRepository
                    val outfit = SavedTryOnOutfit(
                        title = "Color Swap ($selectedColorHex)",
                        fabricName = selectedFabric.name,
                        colorHex = selectedColorHex,
                        topwearCut = selectedSilhouette.displayName,
                        bottomwearCut = "Standard",
                        bottomwearColor = "#1F2937",
                        resultImagePath = null,
                        matchScorePercent = 95,
                    )
                    WardrobeRepository.addOutfit(context, outfit)

                    DrapeSnapRepository.saveSnap(
                        context = context,
                        bitmap = swapBmp,
                        colorHex = selectedColorHex,
                        colorName = "Custom Swap",
                        fabricId = selectedFabric.id,
                        fabricName = selectedFabric.name,
                        matchScorePercent = 95,
                        skinHex = "#D8B498",
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
                    withContext(Dispatchers.Main) { generationStatus = "Downloading fitted photo…" }
                    val urlStream = URL(finalResultUrl).openStream()
                    val downloadedBmp = BitmapFactory.decodeStream(urlStream)

                    // Auto-save outfit to WardrobeRepository and DrapeSnapRepository
                    val outfit = SavedTryOnOutfit(
                        title = "${selectedFabric.name} ${selectedSilhouette.displayName}",
                        fabricName = selectedFabric.name,
                        colorHex = selectedColorHex,
                        topwearCut = selectedSilhouette.displayName,
                        bottomwearCut = "Tailored",
                        bottomwearColor = "#1F2937",
                        resultImagePath = null,
                        matchScorePercent = 96,
                    )
                    WardrobeRepository.addOutfit(context, outfit)

                    DrapeSnapRepository.saveSnap(
                        context = context,
                        bitmap = downloadedBmp,
                        colorHex = selectedColorHex,
                        colorName = selectedSilhouette.displayName,
                        fabricId = selectedFabric.id,
                        fabricName = selectedFabric.name,
                        matchScorePercent = 96,
                        skinHex = "#D8B498",
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

    Surface(modifier = Modifier.fillMaxSize(), color = EditorialCream) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))

            Text("Try-On", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EditorialInk)

            Spacer(Modifier.height(14.dp))

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
                        onClick = { inputSource = tab },
                        text = {
                            Text(
                                tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) EditorialSienna else EditorialInk,
                                fontSize = 13.sp,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // TWO PROMINENT LARGE SELECTION BOXES
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // BOX 1: PERSON IMAGE
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(175.dp)
                        .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
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
                                    modifier = Modifier.clickable { avatarPicker.launch("image/*") },
                                )
                                Text(
                                    "✕",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171),
                                    modifier = Modifier.clickable { activeAvatar = null },
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { avatarPicker.launch("image/*") }
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📤", fontSize = 30.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Upload Person",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialInk,
                                )
                            }
                        }
                    }
                }

                // BOX 2: GARMENT / SILHOUETTE / INTEGRATED COLOR CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(185.dp)
                        .border(1.5.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
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
                                                rawGarmentBitmapToCrop = garmentBmp
                                                isCropperOpen = true
                                            },
                                        )
                                        Text(
                                            "Replace",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.clickable { garmentPicker.launch("image/*") },
                                        )
                                        Text(
                                            "✕",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171),
                                            modifier = Modifier.clickable { customGarmentUri = null },
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { garmentPicker.launch("image/*") }
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
                                            color = EditorialInk,
                                        )
                                        Text("Prompts to crop", fontSize = 10.sp, color = EditorialMuted)
                                    }
                                }
                            }
                        }

                        TryOnInputSource.STYLE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isSilhouetteModalOpen = true }
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
                                        color = EditorialInk,
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
                                        .clickable { isColorPickerOpen = true }
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
                                            .background(EditorialSand)
                                            .border(1.5.dp, EditorialSienna, CircleShape)
                                            .clickable { isColorPickerOpen = true },
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
                                                    .border(if (isSel) 2.5.dp else 1.dp, if (isSel) EditorialSienna else Color.LightGray, CircleShape)
                                                .clickable { selectedColorHex = hex },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // DETAIL CONTROLS ONLY FOR STYLE (COLOR & FABRIC) TAB
            if (inputSource == TryOnInputSource.STYLE) {
                // 1. COLOR SELECTION CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
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
                                    .background(EditorialSienna.copy(alpha = 0.10f))
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
                        Spacer(Modifier.height(10.dp))

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
                                    .clickable { isColorPickerOpen = true },
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
                                        .border(if (isSel) 3.dp else 1.dp, if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable { selectedColorHex = hex },
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 2. FABRIC MATERIAL TEXTURE CARD
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
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
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSel) EditorialSienna.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                                    modifier = Modifier
                                        .width(115.dp)
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(14.dp),
                                        )
                                        .clickable { selectedFabric = fab },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
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

            Spacer(Modifier.height(16.dp))

            // RESULT CARD & TRY-ON CTA
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
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
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = tryOnResultBitmap!!.asImageBitmap(),
                            contentDescription = "Try-On Result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { fullScreenPreviewBitmap = tryOnResultBitmap },
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        val activeAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                        val avatarBmp = if (activeAvatarFile?.exists() == true) BitmapFactory.decodeFile(activeAvatarFile.absolutePath) else null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EditorialSand.copy(alpha = 0.40f))
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
                                    Text("Upload photo to begin", style = MaterialTheme.typography.bodyMedium, color = EditorialInk, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

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
                                showAvatarPromptDialog = true
                                return@Button
                            }
                            if (needsGarment) {
                                showGarmentPromptDialog = true
                                return@Button
                            }
                            executeGeneration()
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text(ctaText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    if (tryOnResultBitmap != null) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            // DOWNLOAD / SAVE TO GALLERY BUTTON
                            OutlinedButton(
                                onClick = {
                                    tryOnResultBitmap?.let { bmp ->
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
                                Text("💾 Save", color = EditorialInk, fontWeight = FontWeight.SemiBold)
                            }

                            // RESET / TRY ANOTHER BUTTON
                            OutlinedButton(
                                onClick = { resetState() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Try Another", color = EditorialInk, fontWeight = FontWeight.SemiBold)
                            }

                            // VIEW IN LOOKS BUTTON
                            Button(
                                onClick = onNavigateToLooks,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialInk),
                                modifier = Modifier.weight(1.1f),
                            ) {
                                Text("View Looks", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // SILHOUETTE & CUT SELECTION MODAL
        if (isSilhouetteModalOpen) {
            AlertDialog(
                onDismissRequest = { isSilhouetteModalOpen = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Choose Silhouette & Cut", fontWeight = FontWeight.Bold, color = EditorialInk, style = MaterialTheme.typography.titleMedium)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(EditorialSand)
                                .clickable { isSilhouetteModalOpen = false },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("✕", fontSize = 12.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
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
                                    .background(if (isSel) EditorialSienna else EditorialCream)
                                    .clickable {
                                        selectedSilhouette = sil
                                        isSilhouetteModalOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(sil.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(sil.displayName, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else EditorialInk, fontSize = 13.sp)
                                    Text(sil.category.title, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(alpha = 0.8f) else EditorialMuted, fontSize = 10.sp)
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
                                    .background(if (isSel) EditorialSienna else EditorialCream)
                                    .clickable {
                                        selectedSilhouette = sil
                                        isSilhouetteModalOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(sil.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(sil.displayName, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else EditorialInk, fontSize = 13.sp)
                                    Text(sil.category.title, style = MaterialTheme.typography.labelSmall, color = if (isSel) Color.White.copy(alpha = 0.8f) else EditorialMuted, fontSize = 10.sp)
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
                title = { Text("Upload Person Photo", fontWeight = FontWeight.Bold, color = EditorialInk) },
                text = { Text("Please select a clear photo of yourself to begin virtual try-on.", color = EditorialMuted) },
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
                        Text("Cancel", color = EditorialInk)
                    }
                },
            )
        }

        if (showGarmentPromptDialog) {
            AlertDialog(
                onDismissRequest = { showGarmentPromptDialog = false },
                title = { Text("Upload Garment", fontWeight = FontWeight.Bold, color = EditorialInk) },
                text = { Text("Please upload a photo of the clothing item you want to try on.", color = EditorialMuted) },
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
                        Text("Cancel", color = EditorialInk)
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
