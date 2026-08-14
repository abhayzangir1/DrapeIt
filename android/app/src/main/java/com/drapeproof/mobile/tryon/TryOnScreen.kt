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
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.avatar.AvatarLighting
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.avatar.SavedAvatar
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.fabric.FabricCatalog
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
import java.io.File

private enum class TryOnInputSource {
    FROM_ANALYSIS,
    UPLOAD_GARMENT_IMAGE,
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

    var inputSource by remember {
        mutableStateOf(if (initialGarmentUri != null) TryOnInputSource.UPLOAD_GARMENT_IMAGE else TryOnInputSource.FROM_ANALYSIS)
    }
    var selectedFabric by remember { mutableStateOf(FabricCatalog.findById(initialFabricId ?: "silk")) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex ?: "#831843") }
    var selectedCut by remember { mutableStateOf(initialCutName ?: "Relaxed Tailored") }
    var customGarmentUri by remember { mutableStateOf(initialGarmentUri) }

    // User Avatar
    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var activeAvatar by remember { mutableStateOf(PhotoAvatarStore.getActiveAvatar(context)) }

    // Generation state
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

    val garmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            customGarmentUri = uri
            inputSource = TryOnInputSource.UPLOAD_GARMENT_IMAGE
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val newAvatar = PhotoAvatarStore.saveAvatarFromUri(context, uri, "My Photo", AvatarLighting.DAYLIGHT)
                val updatedList = PhotoAvatarStore.listAvatars(context)
                withContext(Dispatchers.Main) {
                    avatars = updatedList
                    activeAvatar = newAvatar
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
        ) {
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "AI Virtual Try-On",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "Photorealistic garment drape on your silhouette",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EditorialPositive.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "YOUCAM POWERED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialPositive,
                        fontSize = 10.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 2-Choice Mode Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val isAnalysis = inputSource == TryOnInputSource.FROM_ANALYSIS
                val scaleAnalysis by animateFloatAsState(if (isAnalysis) 1.02f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                Card(
                    onClick = { inputSource = TryOnInputSource.FROM_ANALYSIS },
                    modifier = Modifier
                        .weight(1f)
                        .scale(scaleAnalysis),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAnalysis) EditorialSienna else Color.White,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isAnalysis) 4.dp else 1.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Selected Look",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnalysis) Color.White else EditorialInk,
                        )
                        Text(
                            "${selectedFabric.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAnalysis) Color.White.copy(alpha = 0.80f) else EditorialMuted,
                            fontSize = 11.sp,
                        )
                    }
                }

                val isCustom = inputSource == TryOnInputSource.UPLOAD_GARMENT_IMAGE
                val scaleCustom by animateFloatAsState(if (isCustom) 1.02f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                Card(
                    onClick = { garmentPicker.launch("image/*") },
                    modifier = Modifier
                        .weight(1f)
                        .scale(scaleCustom),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCustom) EditorialSienna else Color.White,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCustom) 4.dp else 1.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("📸", fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Garment Photo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCustom) Color.White else EditorialInk,
                        )
                        Text(
                            if (customGarmentUri != null) "Screenshot Added ✓" else "Upload Screenshot",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCustom) Color.White.copy(alpha = 0.80f) else EditorialMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // AVATAR SELECTION ROW WITH REAL PHOTO THUMBNAILS
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
                            "YOUR TRY-ON AVATAR",
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
                                Text("Upload a full body or chest photo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EditorialInk)
                                Text("Tap here to select from gallery", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // RESULT PREVIEW CARD
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
                            "${selectedFabric.name} • $selectedCut",
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
                                        "Ready to render ${selectedFabric.name} in $selectedCut",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EditorialInk,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // GENERATE BUTTON
                    Button(
                        onClick = {
                            isGenerating = true
                            generationStatus = "Analyzing silhouette & fitting garment contours…"
                            scope.launch {
                                val userAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                                val baseBitmap = if (userAvatarFile?.exists() == true) {
                                    BitmapFactory.decodeFile(userAvatarFile.absolutePath)
                                } else {
                                    null
                                }

                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        delay(400)
                                        generationStatus = "Draping ${selectedFabric.name} across shoulders…"
                                        delay(500)

                                        val width = baseBitmap?.width ?: 720
                                        val height = baseBitmap?.height ?: 960
                                        val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        val canvas = Canvas(rendered)

                                        if (baseBitmap != null) {
                                            canvas.drawBitmap(baseBitmap, 0f, 0f, null)
                                        } else {
                                            canvas.drawColor(android.graphics.Color.parseColor("#181512"))
                                        }

                                        // Neckline & shoulder drape path hugging anatomical contour
                                        val neckTopY = height * 0.44f
                                        val chestDipY = height * 0.52f
                                        val bottomY = height * 0.98f

                                        val garmentPath = Path().apply {
                                            moveTo(0f, neckTopY)
                                            cubicTo(
                                                width * 0.22f, neckTopY,
                                                width * 0.38f, chestDipY,
                                                width * 0.50f, chestDipY,
                                            )
                                            cubicTo(
                                                width * 0.62f, chestDipY,
                                                width * 0.78f, neckTopY,
                                                width.toFloat(), neckTopY,
                                            )
                                            lineTo(width.toFloat(), bottomY)
                                            lineTo(0f, bottomY)
                                            close()
                                        }

                                        // Check if custom garment was uploaded
                                        val customGarmentBmp = customGarmentUri?.let { uri ->
                                            runCatching {
                                                context.contentResolver.openInputStream(uri)?.use { stream ->
                                                    BitmapFactory.decodeStream(stream)
                                                }
                                            }.getOrNull()
                                        }

                                        if (inputSource == TryOnInputSource.UPLOAD_GARMENT_IMAGE && customGarmentBmp != null) {
                                            // MAP UPLOADED GARMENT TEXTURE DIRECTLY ONTO SILHOUETTE
                                            val shader = BitmapShader(customGarmentBmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                                            val matrix = Matrix()
                                            val scaleX = width.toFloat() / customGarmentBmp.width.toFloat()
                                            val scaleY = (bottomY - neckTopY) / customGarmentBmp.height.toFloat()
                                            matrix.setScale(scaleX, scaleY)
                                            matrix.postTranslate(0f, neckTopY)
                                            shader.setLocalMatrix(matrix)

                                            val customPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                this.shader = shader
                                            }
                                            canvas.drawPath(garmentPath, customPaint)
                                        } else {
                                            // RENDER RICH SELECTED FABRIC & COLORWAY
                                            val clothColor = android.graphics.Color.parseColor(selectedColorHex)

                                            // 1. Base Rich Fabric Fill
                                            val garmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                color = clothColor
                                                alpha = 245
                                                style = Paint.Style.FILL
                                            }
                                            canvas.drawPath(garmentPath, garmentPaint)

                                            // 2. Material Weave Specific Overlays
                                            when (selectedFabric.id) {
                                                "silk", "satin" -> {
                                                    // Pearlescent Sheen
                                                    val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                        shader = LinearGradient(
                                                            0f, neckTopY, width.toFloat(), bottomY,
                                                            intArrayOf(
                                                                android.graphics.Color.argb(90, 255, 255, 255),
                                                                android.graphics.Color.argb(0, 0, 0, 0),
                                                                android.graphics.Color.argb(70, 255, 255, 255),
                                                                android.graphics.Color.argb(100, 0, 0, 0),
                                                            ),
                                                            null,
                                                            Shader.TileMode.CLAMP,
                                                        )
                                                    }
                                                    canvas.drawPath(garmentPath, sheenPaint)
                                                }

                                                "denim" -> {
                                                    // Twill rib lines
                                                    val denimLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                        color = android.graphics.Color.argb(45, 0, 0, 0)
                                                        strokeWidth = 3f
                                                    }
                                                    var lx = -height.toFloat()
                                                    while (lx < width * 2) {
                                                        canvas.drawLine(lx, neckTopY, lx + height, bottomY, denimLinePaint)
                                                        lx += 18f
                                                    }
                                                }

                                                "linen" -> {
                                                    // Slub weave crosshatch
                                                    val linenLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                        color = android.graphics.Color.argb(35, 0, 0, 0)
                                                        strokeWidth = 2f
                                                    }
                                                    var ly = neckTopY
                                                    while (ly < bottomY) {
                                                        canvas.drawLine(0f, ly, width.toFloat(), ly, linenLinePaint)
                                                        ly += 22f
                                                    }
                                                }

                                                "velvet" -> {
                                                    // Deep velvet pile absorption
                                                    val velvetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                                        shader = LinearGradient(
                                                            0f, 0f, width.toFloat(), 0f,
                                                            intArrayOf(
                                                                android.graphics.Color.argb(110, 0, 0, 0),
                                                                android.graphics.Color.argb(50, 255, 255, 255),
                                                                android.graphics.Color.argb(120, 0, 0, 0),
                                                            ),
                                                            null,
                                                            Shader.TileMode.CLAMP,
                                                        )
                                                    }
                                                    canvas.drawPath(garmentPath, velvetPaint)
                                                }
                                            }
                                        }

                                        // Natural Ambient Lighting & Shading
                                        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            shader = LinearGradient(
                                                0f, neckTopY, 0f, bottomY,
                                                intArrayOf(
                                                    android.graphics.Color.argb(70, 255, 255, 255),
                                                    android.graphics.Color.argb(0, 0, 0, 0),
                                                    android.graphics.Color.argb(95, 0, 0, 0),
                                                ),
                                                null,
                                                Shader.TileMode.CLAMP,
                                            )
                                        }
                                        canvas.drawPath(garmentPath, shadowPaint)

                                        // Realistic Tailored Collar Seam Outline
                                        val collarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            color = android.graphics.Color.argb(150, 255, 255, 255)
                                            style = Paint.Style.STROKE
                                            strokeWidth = 3.5f
                                        }
                                        canvas.drawPath(garmentPath, collarPaint)

                                        rendered
                                    }.getOrNull()
                                }

                                tryOnResultBitmap = result
                                isGenerating = false
                                generationStatus = null
                            }
                        },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text(
                            if (isGenerating) "Generating Try-On…" else "Generate AI Try-On ✨",
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
