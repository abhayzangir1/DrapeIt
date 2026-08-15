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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
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
) {
    SHIRT("shirt", "Button-Down Shirt", "👔", GarmentCategory.UPPER_BODY),
    TSHIRT("tshirt", "Crewneck T-Shirt", "👕", GarmentCategory.UPPER_BODY),
    BLAZER("blazer", "Tailored Blazer", "🧥", GarmentCategory.UPPER_BODY),
    SWEATER("sweater", "Knit Sweater", "🧶", GarmentCategory.UPPER_BODY),
    DRESS("dress", "A-Line Dress", "👗", GarmentCategory.FULL_BODY),
    PANTS("pants", "Tailored Trousers", "👖", GarmentCategory.LOWER_BODY),
    SKIRT("skirt", "Pleated Midi Skirt", "🩳", GarmentCategory.LOWER_BODY),
}

enum class TryOnInputSource(val title: String, val subtitle: String) {
    STORE_GARMENT("🛍️ Store Garment", "Upload person & garment photos"),
    FROM_ANALYSIS("🎨 Color & Fabric", "Choose fabric, color & silhouette"),
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
    val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(80, 0, 0, 0); style = Paint.Style.STROKE; strokeWidth = 3f }
    val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(230, 245, 245, 240); style = Paint.Style.FILL }
    val buttonRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.argb(130, 80, 80, 80); style = Paint.Style.STROKE; strokeWidth = 2f }
    when (silhouette) {
        GarmentSilhouette.SHIRT -> {
            val bodyPath = Path().apply { moveTo(width * 0.36f, height * 0.16f); lineTo(width * 0.12f, height * 0.32f); lineTo(width * 0.18f, height * 0.46f); lineTo(width * 0.28f, height * 0.42f); lineTo(width * 0.26f, height * 0.88f); lineTo(width * 0.74f, height * 0.88f); lineTo(width * 0.72f, height * 0.42f); lineTo(width * 0.82f, height * 0.46f); lineTo(width * 0.88f, height * 0.32f); lineTo(width * 0.64f, height * 0.16f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            val leftCollar = Path().apply { moveTo(width * 0.36f, height * 0.15f); lineTo(width * 0.50f, height * 0.26f); lineTo(width * 0.42f, height * 0.30f); lineTo(width * 0.32f, height * 0.20f); close() }
            val rightCollar = Path().apply { moveTo(width * 0.64f, height * 0.15f); lineTo(width * 0.50f, height * 0.26f); lineTo(width * 0.58f, height * 0.30f); lineTo(width * 0.68f, height * 0.20f); close() }
            val collarFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInt; style = Paint.Style.FILL }
            canvas.drawPath(leftCollar, collarFill); canvas.drawPath(rightCollar, collarFill); canvas.drawPath(leftCollar, seamPaint); canvas.drawPath(rightCollar, seamPaint)
            canvas.drawLine(width * 0.50f, height * 0.26f, width * 0.50f, height * 0.88f, seamPaint)
            listOf(0.36f, 0.48f, 0.60f, 0.72f, 0.84f).forEach { bY -> canvas.drawCircle(width * 0.50f, height * bY, 6f, buttonPaint); canvas.drawCircle(width * 0.50f, height * bY, 6f, buttonRimPaint) }
        }
        GarmentSilhouette.TSHIRT -> {
            val bodyPath = Path().apply { moveTo(width * 0.35f, height * 0.16f); cubicTo(width * 0.42f, height * 0.23f, width * 0.58f, height * 0.23f, width * 0.65f, height * 0.16f); lineTo(width * 0.88f, height * 0.28f); lineTo(width * 0.80f, height * 0.44f); lineTo(width * 0.72f, height * 0.39f); lineTo(width * 0.71f, height * 0.86f); lineTo(width * 0.29f, height * 0.86f); lineTo(width * 0.28f, height * 0.39f); lineTo(width * 0.20f, height * 0.44f); lineTo(width * 0.12f, height * 0.28f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            val ribPath = Path().apply { moveTo(width * 0.35f, height * 0.16f); cubicTo(width * 0.42f, height * 0.23f, width * 0.58f, height * 0.23f, width * 0.65f, height * 0.16f); cubicTo(width * 0.58f, height * 0.25f, width * 0.42f, height * 0.25f, width * 0.35f, height * 0.16f); close() }
            canvas.drawPath(ribPath, seamPaint); canvas.drawLine(width * 0.28f, height * 0.39f, width * 0.35f, height * 0.16f, seamPaint); canvas.drawLine(width * 0.72f, height * 0.39f, width * 0.65f, height * 0.16f, seamPaint)
        }
        GarmentSilhouette.BLAZER -> {
            val bodyPath = Path().apply { moveTo(width * 0.34f, height * 0.15f); lineTo(width * 0.10f, height * 0.28f); lineTo(width * 0.16f, height * 0.60f); lineTo(width * 0.25f, height * 0.55f); lineTo(width * 0.24f, height * 0.90f); lineTo(width * 0.76f, height * 0.90f); lineTo(width * 0.75f, height * 0.55f); lineTo(width * 0.84f, height * 0.60f); lineTo(width * 0.90f, height * 0.28f); lineTo(width * 0.66f, height * 0.15f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            val leftLapel = Path().apply { moveTo(width * 0.34f, height * 0.15f); lineTo(width * 0.40f, height * 0.28f); lineTo(width * 0.33f, height * 0.32f); lineTo(width * 0.48f, height * 0.56f); lineTo(width * 0.44f, height * 0.56f); lineTo(width * 0.28f, height * 0.28f); close() }
            val rightLapel = Path().apply { moveTo(width * 0.66f, height * 0.15f); lineTo(width * 0.60f, height * 0.28f); lineTo(width * 0.67f, height * 0.32f); lineTo(width * 0.52f, height * 0.56f); lineTo(width * 0.56f, height * 0.56f); lineTo(width * 0.72f, height * 0.28f); close() }
            canvas.drawPath(leftLapel, mainPaint); canvas.drawPath(rightLapel, mainPaint); canvas.drawPath(leftLapel, seamPaint); canvas.drawPath(rightLapel, seamPaint)
            canvas.drawCircle(width * 0.50f, height * 0.60f, 7f, buttonPaint); canvas.drawCircle(width * 0.50f, height * 0.60f, 7f, buttonRimPaint); canvas.drawCircle(width * 0.50f, height * 0.70f, 7f, buttonPaint); canvas.drawCircle(width * 0.50f, height * 0.70f, 7f, buttonRimPaint)
        }
        GarmentSilhouette.SWEATER -> {
            val bodyPath = Path().apply { moveTo(width * 0.36f, height * 0.16f); cubicTo(width * 0.42f, height * 0.21f, width * 0.58f, height * 0.21f, width * 0.64f, height * 0.16f); lineTo(width * 0.88f, height * 0.30f); lineTo(width * 0.82f, height * 0.58f); lineTo(width * 0.73f, height * 0.54f); lineTo(width * 0.72f, height * 0.88f); lineTo(width * 0.28f, height * 0.88f); lineTo(width * 0.27f, height * 0.54f); lineTo(width * 0.18f, height * 0.58f); lineTo(width * 0.12f, height * 0.30f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
        }
        GarmentSilhouette.DRESS -> {
            val bodyPath = Path().apply { moveTo(width * 0.36f, height * 0.14f); cubicTo(width * 0.44f, height * 0.22f, width * 0.56f, height * 0.22f, width * 0.64f, height * 0.14f); lineTo(width * 0.78f, height * 0.26f); lineTo(width * 0.70f, height * 0.38f); lineTo(width * 0.64f, height * 0.35f); lineTo(width * 0.60f, height * 0.48f); lineTo(width * 0.85f, height * 0.94f); lineTo(width * 0.15f, height * 0.94f); lineTo(width * 0.40f, height * 0.48f); lineTo(width * 0.36f, height * 0.35f); lineTo(width * 0.30f, height * 0.38f); lineTo(width * 0.22f, height * 0.26f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
        }
        GarmentSilhouette.PANTS -> {
            val bodyPath = Path().apply { moveTo(width * 0.25f, height * 0.15f); lineTo(width * 0.75f, height * 0.15f); lineTo(width * 0.78f, height * 0.36f); lineTo(width * 0.72f, height * 0.92f); lineTo(width * 0.54f, height * 0.92f); lineTo(width * 0.50f, height * 0.45f); lineTo(width * 0.46f, height * 0.92f); lineTo(width * 0.28f, height * 0.92f); lineTo(width * 0.22f, height * 0.36f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
        }
        GarmentSilhouette.SKIRT -> {
            val bodyPath = Path().apply { moveTo(width * 0.32f, height * 0.20f); lineTo(width * 0.68f, height * 0.20f); lineTo(width * 0.88f, height * 0.88f); lineTo(width * 0.12f, height * 0.88f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
        }
    }
    return bmp
}

fun recolorTopwearBitmap(source: Bitmap, targetHex: String): Bitmap {
    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    canvas.drawBitmap(source, 0f, 0f, null)

    val colorInt = android.graphics.Color.parseColor(targetHex)
    val r = (colorInt shr 16 and 0xFF) / 255f
    val g = (colorInt shr 8 and 0xFF) / 255f
    val b = (colorInt and 0xFF) / 255f

    // Soft multiply color tint over the lower 55% torso area
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    r * 1.2f, 0f, 0f, 0f, 0f,
                    0f, g * 1.2f, 0f, 0f, 0f,
                    0f, 0f, b * 1.2f, 0f, 0f,
                    0f, 0f, 0f, 0.85f, 0f,
                ),
            ),
        )
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    val torsoRect = android.graphics.RectF(0f, source.height * 0.45f, source.width.toFloat(), source.height.toFloat())
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
    var selectedCut by remember { mutableStateOf(initialCutName ?: "Relaxed Tailored") }
    var selectedGender by remember { mutableStateOf("female") }
    var selectedSilhouette by remember { mutableStateOf(GarmentSilhouette.SHIRT) }
    var uploadedGarmentCategory by remember { mutableStateOf(GarmentCategory.UPPER_BODY) }

    var customGarmentUri by remember { mutableStateOf(initialGarmentUri) }
    var isFabricMenuOpen by remember { mutableStateOf(false) }
    var isColorPickerOpen by remember { mutableStateOf(false) }

    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var activeAvatar by remember { mutableStateOf(PhotoAvatarStore.getActiveAvatar(context)) }

    var showAvatarPromptDialog by remember { mutableStateOf(false) }
    var showGarmentPromptDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (avatars.isEmpty()) {
            val snaps = DrapeSnapRepository.list(context)
            snaps.firstOrNull()?.let { snap ->
                val avatar = PhotoAvatarStore.saveAvatarFromUri(context, Uri.fromFile(File(snap.imagePath)), "Captured Portrait", AvatarLighting.DAYLIGHT)
                avatars = PhotoAvatarStore.listAvatars(context)
                activeAvatar = avatar
            }
        }
    }

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
            if (bmp != null) { rawGarmentBitmapToCrop = bmp; isCropperOpen = true } else { customGarmentUri = uri; inputSource = TryOnInputSource.STORE_GARMENT }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val newAvatar = PhotoAvatarStore.saveAvatarFromUri(context, uri, "My Photo", AvatarLighting.DAYLIGHT)
                val updatedList = PhotoAvatarStore.listAvatars(context)
                withContext(Dispatchers.Main) { avatars = updatedList; activeAvatar = newAvatar }
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
                    Pair(bytes, uploadedGarmentCategory.apiValue)
                } else {
                    val refBmp = generateReferenceGarmentBitmap(
                        context = context,
                        silhouette = selectedSilhouette,
                        colorHex = selectedColorHex,
                        fabric = selectedFabric,
                        gender = selectedGender,
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
                    gender = selectedGender,
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
            Text("Photorealistic garment deformation & color fitting", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)

            Spacer(Modifier.height(14.dp))

            // MODE SWITCHER TABS
            SecondaryTabRow(
                selectedTabIndex = inputSource.ordinal,
                containerColor = Color.Transparent,
                contentColor = EditorialSienna,
                indicator = { TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(inputSource.ordinal), color = EditorialSienna, height = 3.dp) },
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

            Spacer(Modifier.height(14.dp))

            // TWO TILES: [ TILE 1: YOUR PHOTO ] & [ TILE 2: GARMENT / COLOR ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // TILE 1: YOUR PHOTO (With Replace / Remove)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("1. Your Photo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna)
                        Spacer(Modifier.height(8.dp))

                        val activeAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                        val avatarBmp = if (activeAvatarFile?.exists() == true) BitmapFactory.decodeFile(activeAvatarFile.absolutePath) else null

                        if (avatarBmp != null) {
                            Image(
                                bitmap = avatarBmp.asImageBitmap(),
                                contentDescription = "Active Avatar",
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(
                                    onClick = { avatarPicker.launch("image/*") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("Replace", fontSize = 10.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { activeAvatar = null },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("✕", fontSize = 10.sp, color = EditorialWarning, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EditorialSand.copy(alpha = 0.5f))
                                    .clickable { avatarPicker.launch("image/*") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("👤\n+ Upload", textAlign = TextAlign.Center, fontSize = 11.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // TILE 2: GARMENT / COLOR TILE
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            when (inputSource) {
                                TryOnInputSource.STORE_GARMENT -> "2. Garment Photo"
                                TryOnInputSource.FROM_ANALYSIS -> "2. Silhouette & Cut"
                                TryOnInputSource.COLOR_SWAP -> "2. New Color"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                        )
                        Spacer(Modifier.height(8.dp))

                        when (inputSource) {
                            TryOnInputSource.STORE_GARMENT -> {
                                val garmentBmp = if (customGarmentUri != null) {
                                    runCatching { context.contentResolver.openInputStream(customGarmentUri!!)?.use { BitmapFactory.decodeStream(it) } }.getOrNull()
                                } else null

                                if (garmentBmp != null) {
                                    Image(
                                        bitmap = garmentBmp.asImageBitmap(),
                                        contentDescription = "Garment",
                                        modifier = Modifier
                                            .size(74.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                rawGarmentBitmapToCrop = garmentBmp
                                                isCropperOpen = true
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp),
                                        ) {
                                            Text("✂️ Crop", fontSize = 10.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { garmentPicker.launch("image/*") },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp),
                                        ) {
                                            Text("Replace", fontSize = 10.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { customGarmentUri = null },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(28.dp),
                                        ) {
                                            Text("✕", fontSize = 10.sp, color = EditorialWarning, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(74.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(EditorialSand.copy(alpha = 0.5f))
                                            .clickable { garmentPicker.launch("image/*") },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("🛍️\n+ Garment", textAlign = TextAlign.Center, fontSize = 11.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            TryOnInputSource.FROM_ANALYSIS -> {
                                Box(
                                    modifier = Modifier
                                        .size(74.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedColorHex.asComposeColor()),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(selectedSilhouette.icon, fontSize = 32.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(selectedSilhouette.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EditorialInk)
                            }

                            TryOnInputSource.COLOR_SWAP -> {
                                Box(
                                    modifier = Modifier
                                        .size(74.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(selectedColorHex.asComposeColor())
                                        .clickable { isColorPickerOpen = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("🎨", fontSize = 28.sp)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Tap to Pick Color", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EditorialInk)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // DETAILED CONTROLS BASED ON ACTIVE MODE
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    when (inputSource) {
                        TryOnInputSource.STORE_GARMENT -> {
                            Text("Garment Placement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                GarmentCategory.values().forEach { cat ->
                                    val isSel = uploadedGarmentCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.15f))
                                            .clickable { uploadedGarmentCategory = cat }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("${cat.icon} ${cat.title}", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else EditorialInk, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        TryOnInputSource.FROM_ANALYSIS -> {
                            Text("Garment Silhouette", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Spacer(Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GarmentSilhouette.values().forEach { sil ->
                                    val isSel = selectedSilhouette == sil
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) EditorialSienna else EditorialSand.copy(alpha = 0.40f))
                                            .clickable { selectedSilhouette = sil }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sil.icon, fontSize = 14.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text(sil.displayName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else EditorialInk, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isColorPickerOpen = true }) {
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(selectedColorHex.asComposeColor()).border(2.dp, EditorialStone, CircleShape))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("${selectedFabric.icon} ${selectedFabric.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                                        Text("Change Color / Fabric", style = MaterialTheme.typography.labelSmall, color = EditorialMuted)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { isColorPickerOpen = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(32.dp)) { Text("🎨 Color", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                    OutlinedButton(onClick = { isFabricMenuOpen = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(32.dp)) { Text("Fabric ▾", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                    DropdownMenu(expanded = isFabricMenuOpen, onDismissRequest = { isFabricMenuOpen = false }) {
                                        FabricCatalog.allFabrics.forEach { fab ->
                                            DropdownMenuItem(text = { Text("${fab.icon} ${fab.name}", fontWeight = FontWeight.Bold) }, onClick = { selectedFabric = fab; isFabricMenuOpen = false })
                                        }
                                    }
                                }
                            }
                        }

                        TryOnInputSource.COLOR_SWAP -> {
                            Text("Quick Color Palette", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                listOf("#831843", "#1D4ED8", "#047857", "#78350F", "#B45309", "#0F172A", "#D97706", "#475569", "#EA580C").forEach { hex ->
                                    val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(hex.asComposeColor())
                                            .border(if (isSel) 3.dp else 1.dp, if (isSel) EditorialSienna else EditorialStone, CircleShape)
                                            .clickable { selectedColorHex = hex },
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(EditorialSand.copy(alpha = 0.5f))
                                        .border(1.dp, EditorialStone, CircleShape)
                                        .clickable { isColorPickerOpen = true },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("🎨", fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (isColorPickerOpen) {
                UniversalColorPickerDialog(
                    initialColorHex = selectedColorHex,
                    onDismiss = { isColorPickerOpen = false },
                    onColorSelected = { hex, _ -> selectedColorHex = hex; isColorPickerOpen = false },
                )
            }

            Spacer(Modifier.height(16.dp))

            // RESULT CARD
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
                        Text("AI VIRTUAL TRY-ON RESULT", style = MaterialTheme.typography.labelSmall, color = EditorialPositive, fontWeight = FontWeight.Bold)
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
                                .height(300.dp)
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
                                    Text("Select or upload your photo above", style = MaterialTheme.typography.bodyMedium, color = EditorialInk, fontWeight = FontWeight.Medium)
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
                        needsGarment -> "📸 Select Garment Photo Above"
                        needsAvatar -> "👤 Select Your Photo Above"
                        inputSource == TryOnInputSource.FROM_ANALYSIS -> "Try On ${selectedSilhouette.displayName} ✨"
                        else -> "Try On Uploaded Garment ✨"
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
                            .height(50.dp),
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

            Spacer(Modifier.height(24.dp))
        }

        if (showAvatarPromptDialog) {
            AlertDialog(
                onDismissRequest = { showAvatarPromptDialog = false },
                title = { Text("Photo Required", fontWeight = FontWeight.Bold) },
                text = { Text("Please upload or select your portrait photo first to generate your try-on.") },
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
                title = { Text("Garment Screenshot Required", fontWeight = FontWeight.Bold) },
                text = { Text("Please select a screenshot or product image of the garment you wish to try on.") },
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
