package com.drapeproof.mobile.looks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.ui.components.FullScreenImageViewerModal
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.util.ImageExportUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LooksScreen(
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToTryOn: (fabricId: String, colorHex: String, snapId: String?) -> Unit,
    onNavigateToCompare: (selectedSnapIds: List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var snaps by remember { mutableStateOf(DrapeSnapRepository.list(context)) }
    var savedOutfits by remember { mutableStateOf(WardrobeRepository.listOutfits(context)) }
    val selectedCompareIds = remember { mutableStateListOf<String>() }

    var snapToDeleteId by remember { mutableStateOf<String?>(null) }
    var outfitToDeleteId by remember { mutableStateOf<String?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var fullScreenImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var fullScreenTitle by remember { mutableStateOf("Look Preview") }

    fun refreshData() {
        snaps = DrapeSnapRepository.list(context)
        savedOutfits = WardrobeRepository.listOutfits(context)
    }

    fun toggleSelection(snapId: String) {
        if (snapId in selectedCompareIds) {
            selectedCompareIds.remove(snapId)
        } else {
            if (selectedCompareIds.size >= 4) {
                alertMessage = "You can select up to 4 looks for side-by-side comparison."
                scope.launch {
                    delay(2500)
                    alertMessage = null
                }
            } else {
                selectedCompareIds.add(snapId)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 90.dp),
            ) {
                Spacer(Modifier.height(14.dp))

                // TOP HEADER BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Looks",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Your personal wardrobe gallery",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (selectedCompareIds.isNotEmpty()) {
                        Text(
                            "Clear (${selectedCompareIds.size})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCompareIds.clear() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // SAVED DRAPE PICTURES SECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "DRAPES (${snaps.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )

                    if (snaps.isNotEmpty()) {
                        Text(
                            if (selectedCompareIds.size == snaps.size) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable {
                                if (selectedCompareIds.size == snaps.size) {
                                    selectedCompareIds.clear()
                                } else {
                                    selectedCompareIds.clear()
                                    snaps.take(4).forEach { selectedCompareIds.add(it.id) }
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (snaps.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🪞", fontSize = 38.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "No Saved Looks Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Capture drape portraits or generate virtual try-ons to review your wardrobe gallery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigateToDrape("silk", "#831843") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            ) {
                                Text("Open Drape Studio", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // 2-COLUMN WARDROBE DRAPES GRID
                    val chunkedSnaps = snaps.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        chunkedSnaps.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { snap ->
                                    val file = File(snap.imagePath)
                                    val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                                    val isSelected = snap.id in selectedCompareIds

                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) EditorialSienna.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                                                shape = RoundedCornerShape(18.dp),
                                            ),
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            // THUMBNAIL WITH TAP-TO-FULLSCREEN & ACTIONS
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(snap.colorHex.asComposeColor())
                                                    .clickable {
                                                        if (bmp != null) {
                                                            fullScreenImageBitmap = bmp
                                                            fullScreenTitle = "${snap.fabricName} • ${snap.colorName}"
                                                        }
                                                    },
                                            ) {
                                                if (bmp != null) {
                                                    Image(
                                                        bitmap = bmp.asImageBitmap(),
                                                        contentDescription = snap.fabricName,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop,
                                                    )
                                                }

                                                // TOP HARMONY PILL
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(6.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.65f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        "${snap.matchScorePercent}%",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                    )
                                                }

                                                // QUICK SAVE BUTTON TOP RIGHT
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.65f))
                                                        .clickable {
                                                            bmp?.let { snapBmp ->
                                                                ImageExportUtils.saveImageToGallery(
                                                                    context = context,
                                                                    bitmap = snapBmp,
                                                                    title = "DrapeIt_${snap.id}",
                                                                )
                                                            }
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text("💾", fontSize = 12.sp)
                                                }
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                "${snap.fabricName} • ${snap.colorName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            // COMPARE & DELETE BUTTONS
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) EditorialSienna else MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { toggleSelection(snap.id) }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                ) {
                                                    Text(
                                                        if (isSelected) "✓ Picked" else "+ Compare",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 10.sp,
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { snapToDeleteId = snap.id },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text("🗑️", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // SAVED VIRTUAL TRY-ON OUTFITS
                if (savedOutfits.isNotEmpty()) {
                    Text(
                        "TRY-ONS (${savedOutfits.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    val chunkedOutfits = savedOutfits.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        chunkedOutfits.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                pair.forEach { outfit ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(outfit.colorHex.asComposeColor()),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("👔", fontSize = 28.sp)
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                outfit.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                "${outfit.fabricName} • ${outfit.topwearCut}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(EditorialSienna.copy(alpha = 0.10f))
                                                        .clickable {
                                                            onNavigateToTryOn("silk", outfit.colorHex, null)
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 3.dp),
                                                ) {
                                                    Text("Try-On →", color = EditorialSienna, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        .clickable { outfitToDeleteId = outfit.id },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text("🗑️", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            // FLOATING ALERT MESSAGE (When <2 items selected)
            if (alertMessage != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.88f))
                        .border(1.dp, EditorialSienna, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        alertMessage!!,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // BOTTOM PINNED COMPARE BUTTON BAR
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = {
                        if (selectedCompareIds.size < 2) {
                            alertMessage = "Select at least 2 pictures using '+ Compare' to compare side-by-side."
                            scope.launch {
                                delay(2500)
                                alertMessage = null
                            }
                        } else {
                            val itemsToCompare = selectedCompareIds.toList()
                            selectedCompareIds.clear()
                            onNavigateToCompare(itemsToCompare)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCompareIds.size >= 2) EditorialSienna else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Text(
                        if (selectedCompareIds.size >= 2) {
                            "⚡ Compare ${selectedCompareIds.size} Looks Side-by-Side"
                        } else {
                            "⚡ Compare Looks (Select 2 to 4)"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // FULL SCREEN IMAGE LIGHTBOX
        if (fullScreenImageBitmap != null) {
            FullScreenImageViewerModal(
                bitmap = fullScreenImageBitmap!!,
                title = fullScreenTitle,
                onDismiss = { fullScreenImageBitmap = null },
            )
        }

        // DELETE SNAP CONFIRMATION DIALOG
        if (snapToDeleteId != null) {
            AlertDialog(
                onDismissRequest = { snapToDeleteId = null },
                title = { Text("Delete Look?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("This captured drape photo will be permanently removed from your private wardrobe.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            DrapeSnapRepository.delete(context, snapToDeleteId!!)
                            selectedCompareIds.remove(snapToDeleteId!!)
                            snapToDeleteId = null
                            refreshData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { snapToDeleteId = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                },
            )
        }

        // DELETE OUTFIT CONFIRMATION DIALOG
        if (outfitToDeleteId != null) {
            AlertDialog(
                onDismissRequest = { outfitToDeleteId = null },
                title = { Text("Delete Outfit?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = { Text("This saved try-on outfit will be removed from your collection.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            WardrobeRepository.removeOutfit(context, outfitToDeleteId!!)
                            outfitToDeleteId = null
                            refreshData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { outfitToDeleteId = null }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
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
