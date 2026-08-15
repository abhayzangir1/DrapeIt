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
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.fabric.FabricTextureShader
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.network.RemoteTaskResult
import com.drapeproof.mobile.network.UploadInput
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
    UPPER_BODY("Upper Body", "upper_body", "👚"),
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
    STORE_GARMENT("🛍️ Store Garment", "Upload person & dress"),
    FROM_ANALYSIS("🎨 Color & Fabric", "Choose silhouette, color & fabric"),
    COLOR_SWAP("🪄 Instant Color Swap", "Swap colors on your current outfit"),
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
    val colorInt = android.graphics.Color.parseColor(colorHex)
    val rawTile = FabricTextureShader.getOrLoadRawBitmap(context, fabric.id)
    val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt; style = Paint.Style.FILL }

    when (silhouette.category) {
        GarmentCategory.UPPER_BODY -> {
            val garmentPath = Path().apply {
                moveTo(width * 0.32f, height * 0.16f)
                lineTo(width * 0.14f, height * 0.32f)
                lineTo(width * 0.24f, height * 0.44f)
                lineTo(width * 0.30f, height * 0.38f)
                lineTo(width * 0.30f, height * 0.88f)
                lineTo(width * 0.70f, height * 0.88f)
                lineTo(width * 0.70f, height * 0.38f)
                lineTo(width * 0.76f, height * 0.44f)
                lineTo(width * 0.86f, height * 0.32f)
                lineTo(width * 0.68f, height * 0.16f)
                quadTo(width * 0.50f, height * 0.24f, width * 0.32f, height * 0.16f)
                close()
            }
            canvas.drawPath(garmentPath, mainPaint)
            rawTile?.let { tile ->
                val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                    alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                canvas.drawPath(garmentPath, tPaint)
            }
        }
        GarmentCategory.FULL_BODY -> {
            val dressPath = Path().apply {
                moveTo(width * 0.32f, height * 0.14f)
                lineTo(width * 0.18f, height * 0.30f)
                lineTo(width * 0.26f, height * 0.38f)
                lineTo(width * 0.32f, height * 0.34f)
                lineTo(width * 0.30f, height * 0.50f)
                lineTo(width * 0.12f, height * 0.94f)
                lineTo(width * 0.88f, height * 0.94f)
                lineTo(width * 0.70f, height * 0.50f)
                lineTo(width * 0.68f, height * 0.34f)
                lineTo(width * 0.74f, height * 0.38f)
                lineTo(width * 0.82f, height * 0.30f)
                lineTo(width * 0.68f, height * 0.14f)
                quadTo(width * 0.50f, height * 0.22f, width * 0.32f, height * 0.14f)
                close()
            }
            canvas.drawPath(dressPath, mainPaint)
            rawTile?.let { tile ->
                val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                    alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                canvas.drawPath(dressPath, tPaint)
            }
        }
        GarmentCategory.LOWER_BODY -> {
            val pantsPath = Path().apply {
                moveTo(width * 0.26f, height * 0.18f)
                lineTo(width * 0.74f, height * 0.18f)
                lineTo(width * 0.82f, height * 0.92f)
                lineTo(width * 0.54f, height * 0.92f)
                lineTo(width * 0.50f, height * 0.48f)
                lineTo(width * 0.46f, height * 0.92f)
                lineTo(width * 0.18f, height * 0.92f)
                close()
            }
            canvas.drawPath(pantsPath, mainPaint)
            rawTile?.let { tile ->
                val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.shader = shader
                    alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255)
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                }
                canvas.drawPath(pantsPath, tPaint)
            }
        }
    }
    return bmp
}

fun recolorTopwearBitmap(source: Bitmap, targetHex: String): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(source, 0f, 0f, null)

    val targetColor = android.graphics.Color.parseColor(targetHex)
    val r = android.graphics.Color.red(targetColor) / 255f
    val g = android.graphics.Color.green(targetColor) / 255f
    val b = android.graphics.Color.blue(targetColor) / 255f

    val colorMatrix = ColorMatrix(
        floatArrayOf(
            r * 1.5f, 0f, 0f, 0f, 0f,
            0f, g * 1.5f, 0f, 0f, 0f,
            0f, 0f, b * 1.5f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(colorMatrix)
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    val torsoRect = android.graphics.Rect(
        (source.width * 0.15f).toInt(),
        (source.height * 0.42f).toInt(),
        (source.width * 0.85f).toInt(),
        (source.height * 0.88f).toInt(),
    )
    canvas.drawRect(torsoRect, paint)
    return result
}

@Composable
fun TryOnScreen(
    initialFabricId: String? = null,
    initialColorHex: String? = null,
    initialCutName: String? = null,
    initialGarmentUri: Uri? = null,
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cutName: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { DrapeProofApiClient() }

    var inputSource by remember {
        mutableStateOf(if (initialGarmentUri != null) TryOnInputSource.STORE_GARMENT else TryOnInputSource.STORE_GARMENT)
    }
    var selectedFabric by remember { mutableStateOf(FabricCatalog.findById(initialFabricId ?: "silk")) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex ?: "#831843") }
    var selectedSilhouette by remember { mutableStateOf(GarmentSilhouette.SHIRT) }

    var customGarmentUri by remember { mutableStateOf(initialGarmentUri) }
    var isColorPickerOpen by remember { mutableStateOf(false) }
    var isSilhouetteModalOpen by remember { mutableStateOf(false) }

    var activeAvatar by remember { mutableStateOf<SavedAvatar?>(PhotoAvatarStore.getActiveAvatar(context)) }

    var showAvatarPromptDialog by remember { mutableStateOf(false) }
    var showGarmentPromptDialog by remember { mutableStateOf(false) }

    var isGenerating by remember { mutableStateOf(false) }
    var generationStatus by remember { mutableStateOf<String?>(null) }
    var tryOnResultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedOutfitId by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "TryOnPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PulseGlow",
    )

    var rawGarmentBitmapToCrop by remember { mutableStateOf<Bitmap?>(null) }
    var isCropperOpen by remember { mutableStateOf(false) }

    val garmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bmp = runCatching { context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) } }.getOrNull()
            if (bmp != null) {
                rawGarmentBitmapToCrop = bmp
                isCropperOpen = true
            } else {
                customGarmentUri = uri
                inputSource = TryOnInputSource.STORE_GARMENT
            }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val newAvatar = PhotoAvatarStore.saveAvatarFromUri(context, uri, "My Photo", AvatarLighting.DAYLIGHT)
                withContext(Dispatchers.Main) { activeAvatar = newAvatar }
            }
        }
    }

    fun executeGeneration() {
        val avatar = activeAvatar
        if (avatar == null) {
            showAvatarPromptDialog = true
            return
        }

        if (inputSource == TryOnInputSource.COLOR_SWAP) {
            val avatarFile = File(avatar.imagePath)
            if (avatarFile.exists()) {
                val bmp = BitmapFactory.decodeFile(avatarFile.absolutePath)
                if (bmp != null) {
                    tryOnResultBitmap = recolorTopwearBitmap(bmp, selectedColorHex)
                    generationStatus = null
                    isGenerating = false
                    return
                }
            }
        }

        if (inputSource == TryOnInputSource.STORE_GARMENT && customGarmentUri == null) {
            showGarmentPromptDialog = true
            return
        }

        isGenerating = true
        generationStatus = "Preparing portrait & garment…"
        tryOnResultBitmap = null

        scope.launch(Dispatchers.IO) {
            try {
                val ok = api.ensureSession("drapeit-client-2026")
                if (!ok) {
                    withContext(Dispatchers.Main) {
                        isGenerating = false
                        generationStatus = "Could not connect to API session."
                    }
                    return@launch
                }

                val avatarFile = File(avatar.imagePath)
                val faceBytes = avatarFile.readBytes()

                withContext(Dispatchers.Main) { generationStatus = "Synthesizing garment reference…" }
                val (refBytes, targetCategory) = if (inputSource == TryOnInputSource.STORE_GARMENT && customGarmentUri != null) {
                    val stream = context.contentResolver.openInputStream(customGarmentUri!!)
                    val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
                    Pair(bytes, "upper_body")
                } else {
                    val refBmp = generateReferenceGarmentBitmap(
                        context = context,
                        silhouette = selectedSilhouette,
                        colorHex = selectedColorHex,
                        fabric = selectedFabric,
                        gender = selectedSilhouette.genderTarget,
                    )
                    val baos = ByteArrayOutputStream()
                    refBmp.compress(Bitmap.CompressFormat.JPEG, 92, baos)
                    Pair(baos.toByteArray(), selectedSilhouette.category.apiValue)
                }

                withContext(Dispatchers.Main) { generationStatus = "Uploading portrait & garment…" }
                val tickets = api.requestUploadTickets(
                    feature = "try-on",
                    inputs = listOf(
                        UploadInput(contentType = "image/jpeg", fileName = "person.jpg", bytes = faceBytes),
                        UploadInput(contentType = "image/jpeg", fileName = "garment.jpg", bytes = refBytes),
                    ),
                )
                api.upload(tickets[0], faceBytes)
                api.upload(tickets[1], refBytes)

                val faceFileId = tickets[0].fileId
                val garmentFileId = tickets[1].fileId

                withContext(Dispatchers.Main) { generationStatus = "Fitting garment with YouCam AI…" }
                val task = api.startTryOn(
                    sourceFileId = faceFileId,
                    referenceFileId = garmentFileId,
                    garmentCategory = targetCategory,
                    provider = "clothes",
                    gender = selectedSilhouette.genderTarget,
                    operationId = UUID.randomUUID().toString(),
                )

                var attempts = 0
                var finalResultUrl: String? = null
                while (attempts < 30) {
                    delay(2000)
                    attempts++
                    withContext(Dispatchers.Main) { generationStatus = "AI neural rendering (${attempts * 2}s)…" }
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

            Text("Virtual Try-On", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
            Text("AI neural fitting & photorealistic drape on your portrait", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)

            Spacer(Modifier.height(14.dp))

            // MODE SWITCHER TABS
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
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // TWO PROMINENT LARGE UPLOAD / CHOICE BOXES (NO CLUTTER)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // BOX 1: PERSON IMAGE (BIG PROMINENT BOX)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(175.dp)
                        .border(1.5.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
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
                                    .background(Color.Black.copy(alpha = 0.70f))
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Text(
                                    "Replace",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.clickable { avatarPicker.launch("image/*") },
                                )
                                Text(
                                    "Remove ✕",
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
                                Text("👤", fontSize = 34.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (inputSource == TryOnInputSource.STORE_GARMENT) "Upload Person's Image" else "Upload Person",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialInk,
                                )
                                Text("Tap to select", fontSize = 10.sp, color = EditorialMuted)
                            }
                        }
                    }
                }

                // BOX 2: DRESS / SILHOUETTE / COLOR (BIG PROMINENT BOX)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(175.dp)
                        .border(1.5.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    when (inputSource) {
                        TryOnInputSource.STORE_GARMENT -> {
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
                                            .background(Color.Black.copy(alpha = 0.70f))
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
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
                                        Text("👗", fontSize = 34.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            "Upload Dress",
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

                        TryOnInputSource.FROM_ANALYSIS -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isSilhouetteModalOpen = true }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(selectedSilhouette.icon, fontSize = 36.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Choose Silhouette & Cut",
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EditorialInk,
                                    )
                                    Text(selectedSilhouette.displayName, fontSize = 11.sp, color = EditorialSienna, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        TryOnInputSource.COLOR_SWAP -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { isColorPickerOpen = true }
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(selectedColorHex.asComposeColor())
                                            .border(2.dp, EditorialSienna, CircleShape),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "Target Color",
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EditorialInk,
                                    )
                                    Text(selectedColorHex.uppercase(), fontSize = 11.sp, color = EditorialMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // DETAIL CONTROLS FOR COLOR & FABRIC (CLEAR VISUAL HIERARCHY)
            if (inputSource == TryOnInputSource.FROM_ANALYSIS || inputSource == TryOnInputSource.COLOR_SWAP) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. COLOR SELECTION ROW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("1. Select Color", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Text(selectedColorHex.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna)
                        }
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EditorialSand)
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
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(if (isSel) 3.dp else 1.dp, if (isSel) EditorialSienna else Color.LightGray, CircleShape)
                                        .clickable { selectedColorHex = hex },
                                )
                            }
                        }

                        // 2. FABRIC SELECTION ROW (ONLY FOR FROM_ANALYSIS)
                        if (inputSource == TryOnInputSource.FROM_ANALYSIS) {
                            Spacer(Modifier.height(16.dp))
                            Text("2. Select Fabric Texture", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FabricCatalog.allFabrics.forEach { fab ->
                                    val isSel = selectedFabric.id == fab.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) EditorialSienna else EditorialSand.copy(alpha = 0.40f))
                                            .border(1.dp, if (isSel) EditorialSienna else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { selectedFabric = fab }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Text(
                                            "${fab.icon} ${fab.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else EditorialInk,
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (tryOnResultBitmap != null) {
                        Text("✨ AI VIRTUAL TRY-ON RESULT", style = MaterialTheme.typography.labelSmall, color = EditorialPositive, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = tryOnResultBitmap!!.asImageBitmap(),
                            contentDescription = "Try-On Result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(16.dp)),
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
                                    Text("👗", fontSize = 42.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Upload person image above to begin", style = MaterialTheme.typography.bodyMedium, color = EditorialInk, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    val needsGarment = inputSource == TryOnInputSource.STORE_GARMENT && customGarmentUri == null
                    val needsAvatar = activeAvatar == null

                    val ctaText = when {
                        isGenerating -> "Generating Try-On…"
                        inputSource == TryOnInputSource.COLOR_SWAP -> "🪄 Instant Color Swap"
                        needsAvatar -> "👤 Upload Person Above"
                        needsGarment -> "👗 Upload Dress Above"
                        inputSource == TryOnInputSource.FROM_ANALYSIS -> "Try On ${selectedSilhouette.displayName} ✨"
                        else -> "Try On Garment ✨"
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
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
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
                            Button(
                                onClick = {
                                    val outfit = SavedTryOnOutfit(
                                        title = "${selectedFabric.name} ${selectedSilhouette.displayName}",
                                        fabricName = selectedFabric.name,
                                        colorHex = selectedColorHex,
                                        topwearCut = selectedSilhouette.displayName,
                                        bottomwearCut = "Relaxed",
                                        bottomwearColor = "#1F2937",
                                        resultImagePath = null,
                                        matchScorePercent = 94,
                                    )
                                    WardrobeRepository.addOutfit(context, outfit)
                                    savedOutfitId = outfit.id
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (savedOutfitId != null) EditorialPositive else EditorialInk,
                                ),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(if (savedOutfitId != null) "✓ Saved to Lookbook" else "Save Look", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigateToShop(selectedFabric.name, "Custom Color", selectedColorHex, selectedSilhouette.displayName)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Find Retailers →", color = EditorialInk, fontWeight = FontWeight.Bold)
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
                    inputSource = TryOnInputSource.STORE_GARMENT
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
                title = { Text("Photo Required", fontWeight = FontWeight.Bold) },
                text = { Text("Please upload your portrait photo to generate your try-on.") },
                confirmButton = {
                    Button(onClick = { showAvatarPromptDialog = false; avatarPicker.launch("image/*") }) {
                        Text("Upload Photo")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showAvatarPromptDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        if (showGarmentPromptDialog) {
            AlertDialog(
                onDismissRequest = { showGarmentPromptDialog = false },
                title = { Text("Garment Required", fontWeight = FontWeight.Bold) },
                text = { Text("Please upload a dress or garment photo to try on.") },
                confirmButton = {
                    Button(onClick = { showGarmentPromptDialog = false; garmentPicker.launch("image/*") }) {
                        Text("Choose Garment")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showGarmentPromptDialog = false }) {
                        Text("Cancel")
                    }
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
