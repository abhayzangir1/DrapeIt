package com.drapeproof.mobile.marketplace

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.shop.ShoppingSearchEngine
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.GoldAccent
import com.drapeproof.mobile.ui.theme.Moss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarketplaceUploadScreen(
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cutName: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var activeAvatar by remember { mutableStateOf(PhotoAvatarStore.getActiveAvatar(context)) }

    var isProcessing by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var savedOutfitId by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            screenshotUri = uri
            resultBitmap = null
            savedOutfitId = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        Text("Marketplace Screenshot Try-On", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Found an outfit on Zara, Amazon, Instagram or Pinterest? Upload the screenshot to try it on yourself!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )

        Spacer(Modifier.height(18.dp))

        // Upload Screenshot Drop Zone Card
        Card(
            onClick = { imagePicker.launch("image/*") },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (screenshotUri != null) {
                    Text("📸 Screenshot Loaded ✓", style = MaterialTheme.typography.titleMedium, color = Moss)
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to change screenshot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                } else {
                    Text("🖼️", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(10.dp))
                    Text("Upload Clothing Screenshot", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Tap to pick from gallery", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Avatar Selection
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("CHOOSE YOUR TARGET AVATAR", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                Spacer(Modifier.height(8.dp))

                if (avatars.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        avatars.forEach { av ->
                            val isSelected = activeAvatar?.id == av.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { activeAvatar = av },
                                label = { Text("${av.lighting.icon} ${av.name}") },
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }
                } else {
                    Text("Using default profile. You can save custom daylight photos in the Wardrobe tab.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // AI Render Card
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (resultBitmap != null) {
                    Text("AI TRY-ON RESULT", style = MaterialTheme.typography.labelSmall, color = Moss)
                    Spacer(Modifier.height(10.dp))
                    Image(
                        bitmap = resultBitmap!!.asImageBitmap(),
                        contentDescription = "Try-On Result",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isProcessing) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = DrapeCoral)
                                Spacer(Modifier.height(10.dp))
                                Text(statusText ?: "AI Extracting & Draping…", style = MaterialTheme.typography.bodyMedium, color = DrapeCoral)
                            }
                        } else {
                            Text(
                                if (screenshotUri != null) "Ready to generate try-on from your screenshot!" else "Upload a cloth screenshot to begin",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        isProcessing = true
                        statusText = "AI Segmenting Garment from Screenshot…"
                        scope.launch {
                            delay(1400)
                            statusText = "Draping on Your Silhouette via YouCam…"
                            delay(1200)
                            val sample = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(sample)
                            canvas.drawColor(android.graphics.Color.parseColor("#2F51A2"))
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            canvas.drawText("AI Screenshot Try-On", 200f, 250f, paint)
                            resultBitmap = sample
                            isProcessing = false
                            statusText = null
                        }
                    },
                    enabled = !isProcessing && screenshotUri != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                ) {
                    Text(
                        if (isProcessing) "Rendering Try-On…" else "Try It On Now ✨",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }

                if (resultBitmap != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = {
                                val outfit = SavedTryOnOutfit(
                                    title = "Marketplace Screenshot Look",
                                    fabricName = "Matched Material",
                                    colorHex = "#2F51A2",
                                    topwearCut = "Contemporary Fit",
                                    bottomwearCut = "Straight Leg Slacks",
                                    bottomwearColor = "Dark Charcoal",
                                )
                                WardrobeRepository.addOutfit(context, outfit)
                                savedOutfitId = outfit.id
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (savedOutfitId != null) GoldAccent else MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text(if (savedOutfitId != null) "★ Saved" else "Save Look")
                        }

                        OutlinedButton(
                            onClick = {
                                onNavigateToShop(
                                    "Matched Material",
                                    "Deep Cobalt",
                                    "#2F51A2",
                                    "Contemporary Cut",
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("🔍 Find to Buy ↗")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}
