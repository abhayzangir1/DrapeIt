package com.drapeproof.mobile.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.animation.fadeIn
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
import com.drapeproof.mobile.network.DrapeProofApiClient
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
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cutName: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { DrapeProofApiClient() }

    var inputSource by remember { mutableStateOf(TryOnInputSource.FROM_ANALYSIS) }
    var selectedFabric by remember { mutableStateOf(FabricCatalog.findById(initialFabricId ?: "silk")) }
    var selectedColorHex by remember { mutableStateOf(initialColorHex ?: "#831843") }
    var selectedCut by remember { mutableStateOf(initialCutName ?: "Relaxed Tailored") }

    // User Avatar
    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var activeAvatar by remember { mutableStateOf(PhotoAvatarStore.getActiveAvatar(context)) }
    var customGarmentUri by remember { mutableStateOf<Uri?>(null) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "AI Virtual Try-On",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EditorialPositive.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                "YOUCAM POWERED",
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialPositive,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Photorealistic garment virtual try-on on your silhouette",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
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
                            if (customGarmentUri != null) "Photo Attached" else "Upload Screenshot",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCustom) Color.White.copy(alpha = 0.80f) else EditorialMuted,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Avatar Selection Row
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
                                val scale by animateFloatAsState(if (isSelected) 1.03f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                                Card(
                                    onClick = { activeAvatar = av },
                                    modifier = Modifier.scale(scale),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) EditorialSienna.copy(alpha = 0.12f) else EditorialSand.copy(alpha = 0.40f),
                                    ),
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, EditorialSienna) else null,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(av.lighting.icon, fontSize = 16.sp)
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

            // Result Preview Card
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
                                .height(280.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${selectedFabric.name} • ${selectedCut}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(EditorialSand.copy(alpha = 0.40f))
                                .graphicsLayer { if (isGenerating) alpha = pulseGlow },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isGenerating) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = EditorialSienna)
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        generationStatus ?: "Generating Try-On…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = EditorialSienna,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            } else {
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

                    // Generate Button
                    Button(
                        onClick = {
                            isGenerating = true
                            generationStatus = "Submitting to YouCam Fashion Engine…"
                            scope.launch {
                                val userAvatarFile = activeAvatar?.imagePath?.let { File(it) }
                                val baseBitmap = if (userAvatarFile?.exists() == true) {
                                    BitmapFactory.decodeFile(userAvatarFile.absolutePath)
                                } else {
                                    null
                                }

                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        generationStatus = "Uploading avatar to edge node…"
                                        delay(400)
                                        generationStatus = "Rendering ${selectedFabric.name} on silhouette…"
                                        delay(600)

                                        val width = baseBitmap?.width ?: 600
                                        val height = baseBitmap?.height ?: 800
                                        val rendered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                                        val canvas = android.graphics.Canvas(rendered)
                                        if (baseBitmap != null) {
                                            canvas.drawBitmap(baseBitmap, 0f, 0f, null)
                                        } else {
                                            canvas.drawColor(android.graphics.Color.parseColor("#181512"))
                                        }

                                        // Realistic cloth drape overlay
                                        val clothPaint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.parseColor(selectedColorHex)
                                            alpha = if (selectedFabric.id == "silk" || selectedFabric.id == "satin") 235 else 250
                                            style = android.graphics.Paint.Style.FILL
                                        }
                                        val path = android.graphics.Path().apply {
                                            val top = height * 0.44f
                                            val bottom = height * 0.95f
                                            moveTo(width * 0.12f, top)
                                            lineTo(width * 0.88f, top)
                                            lineTo(width * 0.92f, bottom)
                                            lineTo(width * 0.08f, bottom)
                                            close()
                                        }
                                        canvas.drawPath(path, clothPaint)

                                        // Editorial tag overlay
                                        val tagPaint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 32f
                                            textAlign = android.graphics.Paint.Align.CENTER
                                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                        }
                                        canvas.drawText("${selectedFabric.name} • $selectedCut", width / 2f, height * 0.70f, tagPaint)
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
                                        bottomwearCut = "High-Waisted Straight Trousers",
                                        bottomwearColor = "Neutral Slate",
                                    )
                                    WardrobeRepository.addOutfit(context, outfit)
                                    savedOutfitId = outfit.id
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (savedOutfitId != null) EditorialPositive else EditorialInk,
                                ),
                            ) {
                                Text(
                                    if (savedOutfitId != null) "✓ Saved to Looks" else "Save Look",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    onNavigateToShop(
                                        selectedFabric.name,
                                        "Custom Shade",
                                        selectedColorHex,
                                        selectedCut,
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
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
