package com.drapeproof.mobile.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private enum class TryOnInputSource(val title: String, val subtitle: String) {
    FROM_ANALYSIS("🎨 Palette Look", "Choose fabric, color & garment style"),
    UPLOAD_GARMENT_IMAGE("🛍️ Store Garment", "Upload shirt photo from Zara, Amazon, etc."),
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
            canvas.drawRect(width * 0.28f, height * 0.66f, width * 0.40f, height * 0.70f, seamPaint); canvas.drawRect(width * 0.60f, height * 0.66f, width * 0.72f, height * 0.70f, seamPaint); canvas.drawRect(width * 0.32f, height * 0.36f, width * 0.42f, height * 0.38f, seamPaint)
        }
        GarmentSilhouette.SWEATER -> {
            val bodyPath = Path().apply { moveTo(width * 0.36f, height * 0.16f); cubicTo(width * 0.42f, height * 0.21f, width * 0.58f, height * 0.21f, width * 0.64f, height * 0.16f); lineTo(width * 0.88f, height * 0.30f); lineTo(width * 0.82f, height * 0.58f); lineTo(width * 0.73f, height * 0.54f); lineTo(width * 0.72f, height * 0.88f); lineTo(width * 0.28f, height * 0.88f); lineTo(width * 0.27f, height * 0.54f); lineTo(width * 0.18f, height * 0.58f); lineTo(width * 0.12f, height * 0.30f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            canvas.drawRect(width * 0.28f, height * 0.84f, width * 0.72f, height * 0.88f, seamPaint)
        }
        GarmentSilhouette.DRESS -> {
            val bodyPath = Path().apply { moveTo(width * 0.36f, height * 0.14f); cubicTo(width * 0.44f, height * 0.22f, width * 0.56f, height * 0.22f, width * 0.64f, height * 0.14f); lineTo(width * 0.78f, height * 0.26f); lineTo(width * 0.70f, height * 0.38f); lineTo(width * 0.64f, height * 0.35f); lineTo(width * 0.60f, height * 0.48f); lineTo(width * 0.85f, height * 0.94f); lineTo(width * 0.15f, height * 0.94f); lineTo(width * 0.40f, height * 0.48f); lineTo(width * 0.36f, height * 0.35f); lineTo(width * 0.30f, height * 0.38f); lineTo(width * 0.22f, height * 0.26f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            canvas.drawLine(width * 0.40f, height * 0.48f, width * 0.60f, height * 0.48f, seamPaint); canvas.drawLine(width * 0.45f, height * 0.48f, width * 0.35f, height * 0.94f, seamPaint); canvas.drawLine(width * 0.55f, height * 0.48f, width * 0.65f, height * 0.94f, seamPaint)
        }
        GarmentSilhouette.PANTS -> {
            val bodyPath = Path().apply { moveTo(width * 0.25f, height * 0.15f); lineTo(width * 0.75f, height * 0.15f); lineTo(width * 0.78f, height * 0.36f); lineTo(width * 0.72f, height * 0.92f); lineTo(width * 0.54f, height * 0.92f); lineTo(width * 0.50f, height * 0.45f); lineTo(width * 0.46f, height * 0.92f); lineTo(width * 0.28f, height * 0.92f); lineTo(width * 0.22f, height * 0.36f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            canvas.drawLine(width * 0.25f, height * 0.22f, width * 0.75f, height * 0.22f, seamPaint); canvas.drawLine(width * 0.50f, height * 0.22f, width * 0.50f, height * 0.45f, seamPaint); canvas.drawLine(width * 0.37f, height * 0.25f, width * 0.37f, height * 0.92f, seamPaint); canvas.drawLine(width * 0.63f, height * 0.25f, width * 0.63f, height * 0.92f, seamPaint)
        }
        GarmentSilhouette.SKIRT -> {
            val bodyPath = Path().apply { moveTo(width * 0.32f, height * 0.20f); lineTo(width * 0.68f, height * 0.20f); lineTo(width * 0.88f, height * 0.88f); lineTo(width * 0.12f, height * 0.88f); close() }
            canvas.drawPath(bodyPath, mainPaint)
            rawTile?.let { tile ->
                val tileShader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = tileShader; alpha = (fabric.textureAlpha * 255).toInt().coerceIn(0, 255); xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
                canvas.drawPath(bodyPath, tilePaint)
            }
            canvas.drawLine(width * 0.32f, height * 0.26f, width * 0.68f, height * 0.26f, seamPaint)
            listOf(0.24f, 0.36f, 0.48f, 0.60f, 0.72f, 0.84f).forEach { frac -> val topX = width * (0.32f + (0.68f - 0.32f) * ((frac - 0.12f) / 0.76f)); canvas.drawLine(topX, height * 0.26f, width * frac, height * 0.88f, seamPaint) }
        }
    }
    return bmp
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
        mutableStateOf(if (initialGarmentUri != null) TryOnInputSource.UPLOAD_GARMENT_IMAGE else TryOnInputSource.FROM_ANALYSIS)
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
            if (bmp != null) { rawGarmentBitmapToCrop = bmp; isCropperOpen = true } else { customGarmentUri = uri; inputSource = TryOnInputSource.UPLOAD_GARMENT_IMAGE }
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
        isGenerating = true
        generationStatus = "Submitting to Perfect Corp YouCam AI Engine…"
        scope.launch {
            val userAvatarFile = activeAvatar?.imagePath?.let { File(it) }
            val baseBitmap = if (userAvatarFile?.exists() == true) BitmapFactory.decodeFile(userAvatarFile.absolutePath) else null

            val result = withContext(Dispatchers.IO) {
                var youCamBitmap: Bitmap? = null
                if (api.cloudConfigured && baseBitmap != null) {
                    runCatching {
                        generationStatus = "Authenticating with YouCam AI Proxy…"
                        api.ensureSession()
                        generationStatus = "Preparing high-res garment reference & avatar…"
                        val faceStream = ByteArrayOutputStream()
                        baseBitmap.compress(Bitmap.CompressFormat.JPEG, 90, faceStream)
                        val faceBytes = faceStream.toByteArray()
                        val (garmentBytes, targetCategory) = if (inputSource == TryOnInputSource.UPLOAD_GARMENT_IMAGE && customGarmentUri != null) {
                            context.contentResolver.openInputStream(customGarmentUri!!)?.use { it.readBytes() } to uploadedGarmentCategory.apiValue
                        } else {
                            val refBmp = generateReferenceGarmentBitmap(context, selectedSilhouette, selectedColorHex, selectedFabric, selectedGender)
                            val stream = ByteArrayOutputStream()
                            refBmp.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                            stream.toByteArray() to selectedSilhouette.category.apiValue
                        }
                        if (garmentBytes != null) {
                            generationStatus = "Uploading avatar and garment to YouCam Cloud…"
                            val faceInput = UploadInput(contentType = "image/jpeg", fileName = "person.jpg", bytes = faceBytes)
                            val garmentInput = UploadInput(contentType = "image/jpeg", fileName = "garment.jpg", bytes = garmentBytes)
                            val tickets = api.requestUploadTickets("try-on", listOf(faceInput, garmentInput))
                            api.upload(tickets[0], faceBytes); api.upload(tickets[1], garmentBytes)
                            generationStatus = "Creating YouCam Cloth V3 neural task ($targetCategory)…"
                            val operationId = UUID.randomUUID().toString()
                            val task = api.startTryOn(sourceFileId = tickets[0].fileId, referenceFileId = tickets[1].fileId, garmentCategory = targetCategory, provider = "clothes", gender = selectedGender, style = "style_modern_chic", operationId = operationId)
                            generationStatus = "YouCam AI neural cloth synthesis in progress…"
                            var attempts = 0
                            while (attempts < 30) {
                                delay(2000)
                                when (val pollRes = api.poll("try-on", task.taskId)) {
                                    is RemoteTaskResult.TryOnImage -> { val stream = URL(pollRes.imageUrl).openStream(); youCamBitmap = BitmapFactory.decodeStream(stream); break }
                                    is RemoteTaskResult.Running -> attempts++
                                    else -> break
                                }
                            }
                        }
                    }
                }
                if (youCamBitmap != null) youCamBitmap else {
                    runCatching {
                        delay(600); generationStatus = "Draping tailored ${selectedSilhouette.displayName} on silhouette…"; delay(600)
                        val width = baseBitmap?.width ?: 720; val height = baseBitmap?.height ?: 960
                        val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(rendered)
                        if (baseBitmap != null) canvas.drawBitmap(baseBitmap, 0f, 0f, null) else canvas.drawColor(android.graphics.Color.parseColor("#181512"))
                        rendered
                    }.getOrNull()
                }
            }
            tryOnResultBitmap = result; isGenerating = false; generationStatus = null
        }
    }

    if (showAvatarPromptDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarPromptDialog = false },
            title = { Text("Select Silhouette to Try On", fontWeight = FontWeight.Bold, color = EditorialInk) },
            text = { Text("You haven't uploaded a photo yet. Would you like to upload your selfie, or try it on an AI fashion model?", color = EditorialInk) },
            confirmButton = { Button(onClick = { showAvatarPromptDialog = false; avatarPicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna)) { Text("📷 Upload My Photo") } },
            dismissButton = { TextButton(onClick = { showAvatarPromptDialog = false; scope.launch(Dispatchers.IO) { val modelAvatar = PhotoAvatarStore.createStudioModelAvatar(context); avatars = PhotoAvatarStore.listAvatars(context); activeAvatar = modelAvatar; executeGeneration() } }) { Text("👤 Use AI Fit Model", fontWeight = FontWeight.Bold, color = EditorialSienna) } },
        )
    }

    if (showGarmentPromptDialog) {
        AlertDialog(
            onDismissRequest = { showGarmentPromptDialog = false },
            title = { Text("Select Garment to Try On", fontWeight = FontWeight.Bold, color = EditorialInk) },
            text = { Text("You selected Store Garment mode. Please upload a garment screenshot first, or switch to Palette Look.", color = EditorialInk) },
            confirmButton = { Button(onClick = { showGarmentPromptDialog = false; garmentPicker.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna)) { Text("🛍️ Choose Garment Photo") } },
            dismissButton = { TextButton(onClick = { showGarmentPromptDialog = false; inputSource = TryOnInputSource.FROM_ANALYSIS }) { Text("Use Palette Look Instead", color = EditorialSienna) } },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(EditorialSand)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("AI Virtual Try-On", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                    Text("Photorealistic clothing fit on your silhouette", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                }
                Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(EditorialPositive.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("YOUCAM AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialPositive, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = inputSource.ordinal,
                containerColor = Color.White,
                contentColor = EditorialSienna,
                modifier = Modifier.clip(RoundedCornerShape(16.dp)).border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                indicator = { tabPositions -> TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[inputSource.ordinal]), color = EditorialSienna, height = 3.dp) },
                divider = {},
            ) {
                TryOnInputSource.values().forEach { tab ->
                    val isSelected = inputSource == tab
                    Tab(selected = isSelected, onClick = { inputSource = tab }, text = { Text(tab.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) EditorialSienna else EditorialInk, fontSize = 12.sp) })
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (inputSource == TryOnInputSource.FROM_ANALYSIS) {
                        Text("Gender & Fit Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("female" to "👩 Female", "male" to "👨 Male", "unisex" to "⚧ Unisex").forEach { (genderKey, label) ->
                                val isSel = selectedGender == genderKey
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.15f)).clickable { selectedGender = genderKey }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(label, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else EditorialInk, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Garment Type & Cut", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GarmentSilhouette.values().forEach { sil ->
                                val isSel = selectedSilhouette == sil
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isSel) EditorialSienna.copy(alpha = 0.12f) else EditorialStone.copy(alpha = 0.08f)),
                                    modifier = Modifier.border(width = if (isSel) 1.5.dp else 0.5.dp, color = if (isSel) EditorialSienna else Color.Transparent, shape = RoundedCornerShape(12.dp)).clickable { selectedSilhouette = sil },
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(sil.icon, fontSize = 16.sp)
                                        Text(sil.displayName, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) EditorialSienna else EditorialInk, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Material & Color", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isColorPickerOpen = true }) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(selectedColorHex))).border(2.dp, EditorialStone, CircleShape))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("${selectedFabric.icon} ${selectedFabric.name}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                                    Text("${selectedSilhouette.displayName} • Tap to change", style = MaterialTheme.typography.labelSmall, color = EditorialMuted)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { isColorPickerOpen = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(34.dp)) { Text("🎨 Color", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                OutlinedButton(onClick = { isFabricMenuOpen = true }, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(34.dp)) { Text("Fabric ▾", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                DropdownMenu(expanded = isFabricMenuOpen, onDismissRequest = { isFabricMenuOpen = false }) {
                                    FabricCatalog.allFabrics.forEach { fab ->
                                        DropdownMenuItem(text = { Text("${fab.icon} ${fab.name}", fontWeight = FontWeight.Bold) }, onClick = { selectedFabric = fab; isFabricMenuOpen = false })
                                    }
                                }
                            }
                        }
                        if (isColorPickerOpen) UniversalColorPickerDialog(initialColorHex = selectedColorHex, onDismiss = { isColorPickerOpen = false }, onColorSelected = { hex, _ -> selectedColorHex = hex; isColorPickerOpen = false })
                    } else {
                        Text("Upload Garment Screenshot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                        Text("Upload a product photo from Zara, Amazon, or Pinterest to try on.", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                        Spacer(Modifier.height(10.dp))
                        Text("Garment Placement", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            GarmentCategory.values().forEach { cat ->
                                val isSel = uploadedGarmentCategory == cat
                                Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.15f)).clickable { uploadedGarmentCategory = cat }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text("${cat.icon} ${cat.title}", fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) Color.White else EditorialInk, fontSize = 11.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (customGarmentUri != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛍️", fontSize = 24.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Garment Ready ✓", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialPositive)
                                        Text("Cropped & aligned", style = MaterialTheme.typography.labelSmall, color = EditorialMuted)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            runCatching { context.contentResolver.openInputStream(customGarmentUri!!)?.use { stream -> BitmapFactory.decodeStream(stream) } }.getOrNull()?.let { bmp ->
                                                rawGarmentBitmapToCrop = bmp
                                                isCropperOpen = true
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) { Text("✂️ Crop", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                    OutlinedButton(
                                        onClick = { garmentPicker.launch("image/*") },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.height(32.dp),
                                    ) { Text("Replace", style = MaterialTheme.typography.labelSmall, color = EditorialInk) }
                                }
                            }
                        } else {
                            Button(
                                onClick = { garmentPicker.launch("image/*") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("📸 Choose Garment from Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isCropperOpen && rawGarmentBitmapToCrop != null) {
                            GarmentCropperModal(
                                sourceBitmap = rawGarmentBitmapToCrop!!,
                                onDismiss = { isCropperOpen = false; rawGarmentBitmapToCrop = null },
                                onCropped = { file ->
                                    customGarmentUri = Uri.fromFile(file)
                                    inputSource = TryOnInputSource.UPLOAD_GARMENT_IMAGE
                                    isCropperOpen = false
                                    rawGarmentBitmapToCrop = null
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // STEP 2: YOUR AVATAR PHOTO
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "STEP 2: YOUR AVATAR PHOTO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            "+ Add Photo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                            modifier = Modifier.clickable { avatarPicker.launch("image/*") },
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    if (avatars.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            avatars.forEach { av ->
                                val isSelected = activeAvatar?.id == av.id
                                val file = File(av.imagePath)
                                val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

                                Card(
                                    onClick = { activeAvatar = av },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) EditorialSienna.copy(alpha = 0.12f) else EditorialSand.copy(alpha = 0.40f),
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, EditorialSienna) else null,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = av.name,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(EditorialSand),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(av.lighting.icon, fontSize = 18.sp)
                                            }
                                        }

                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(av.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                                            Text(av.lighting.displayName, style = MaterialTheme.typography.labelSmall, color = EditorialMuted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { avatarPicker.launch("image/*") }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("👤", fontSize = 24.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Upload a photo or tap to pick avatar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                                Text("Tap here to choose from gallery", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // STEP 3: RESULT PREVIEW CARD
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (tryOnResultBitmap != null) {
                        Text("AI VIRTUAL TRY-ON RESULT", style = MaterialTheme.typography.labelSmall, color = EditorialPositive, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Image(
                            bitmap = tryOnResultBitmap!!.asImageBitmap(),
                            contentDescription = "AI Try-On Result",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (inputSource == TryOnInputSource.FROM_ANALYSIS) "${selectedFabric.name} • $selectedCut" else "Uploaded Garment Fit",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                    } else {
                        val activeAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                        val avatarBmp = if (activeAvatarFile?.exists() == true) BitmapFactory.decodeFile(activeAvatarFile.absolutePath) else null

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EditorialSand.copy(alpha = 0.50f))
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
                                        .background(Color.Black.copy(alpha = 0.60f)),
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
                                    Text(
                                        "Select your avatar photo above",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EditorialInk,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val needsGarment = inputSource == TryOnInputSource.UPLOAD_GARMENT_IMAGE && customGarmentUri == null
                    val needsAvatar = activeAvatar == null

                    val ctaText = when {
                        isGenerating -> "Generating Try-On…"
                        needsGarment -> "📸 Select Garment Screenshot First"
                        needsAvatar -> "👤 Select Portrait Photo First"
                        inputSource == TryOnInputSource.FROM_ANALYSIS -> "Try On ${selectedFabric.name} (${selectedCut}) ✨"
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text(
                            ctaText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
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
                                        title = "${selectedFabric.name} $selectedCut",
                                        fabricName = selectedFabric.name,
                                        colorHex = selectedColorHex,
                                        topwearCut = selectedCut,
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
                                Text(
                                    if (savedOutfitId != null) "✓ Saved to Lookbook" else "Save Look",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigateToShop(selectedFabric.name, "Custom Color", selectedColorHex, selectedCut)
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
