package com.drapeproof.mobile.looks

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun LooksScreen(
    onNavigateToTryOn: (fabricId: String, colorHex: String, garmentUri: Uri?) -> Unit,
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToCompare: (selectedIds: List<String>) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var snaps by remember { mutableStateOf(DrapeSnapRepository.list(context)) }
    var savedOutfits by remember { mutableStateOf(WardrobeRepository.listOutfits(context)) }
    val selectedCompareIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        snaps = DrapeSnapRepository.list(context)
        savedOutfits = WardrobeRepository.listOutfits(context)
        selectedCompareIds.clear()
    }

    var alertMessage by remember { mutableStateOf<String?>(null) }
    var snapToDeleteId by remember { mutableStateOf<String?>(null) }
    var outfitToDeleteId by remember { mutableStateOf<String?>(null) }

    fun toggleSelection(id: String) {
        if (id in selectedCompareIds) {
            selectedCompareIds.remove(id)
        } else {
            if (selectedCompareIds.size >= 4) {
                alertMessage = "You can compare up to 4 pictures at a time."
                scope.launch {
                    delay(2500)
                    alertMessage = null
                }
            } else {
                selectedCompareIds.add(id)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
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

                // TOP HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Looks",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )

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
                        letterSpacing = 1.sp,
                    )

                    if (snaps.isNotEmpty()) {
                        Text(
                            if (selectedCompareIds.size == snaps.size) "Deselect All" else "Select All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EditorialMuted,
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("🪞", fontSize = 38.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "No Captured Pictures Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Snap portraits in the Drape tab to compare fabrics and colors side-by-side.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigateToDrape("silk", "#831843") },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            ) {
                                Text("🪞 Open Drape Studio", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        snaps.forEach { snap ->
                            val file = File(snap.imagePath)
                            val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                            val isSelected = snap.id in selectedCompareIds

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) EditorialSienna.copy(alpha = 0.06f) else Color.White,
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) EditorialSienna else EditorialStone.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(18.dp),
                                    )
                                    .clickable { toggleSelection(snap.id) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // Thumbnail
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = snap.fabricName,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(14.dp)),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(snap.colorHex.asComposeColor()),
                                        )
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${snap.fabricName} • ${snap.colorName}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialInk,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "Harmony Match: ${snap.matchScorePercent}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EditorialPositive,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            // ADD / REMOVE COMPARE PILL
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) EditorialSienna else EditorialSand)
                                                    .clickable { toggleSelection(snap.id) }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    if (isSelected) "✓ In Compare" else "+ Compare",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else EditorialInk,
                                                    fontSize = 11.sp,
                                                )
                                            }

                                            // TRY-ON EXACT COLOR CTA
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(EditorialSand.copy(alpha = 0.5f))
                                                    .clickable {
                                                        onNavigateToTryOn(snap.fabricId, snap.colorHex, null)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                            ) {
                                                Text(
                                                    "Try-On Color ✨",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EditorialSienna,
                                                    fontSize = 11.sp,
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // DOWNLOAD / SAVE BUTTON
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(EditorialSand.copy(alpha = 0.40f))
                                                .clickable {
                                                    bmp?.let { snapBitmap ->
                                                        com.drapeproof.mobile.util.ImageExportUtils.saveImageToGallery(
                                                            context = context,
                                                            bitmap = snapBitmap,
                                                            title = "DrapeIt_Look_${snap.id}",
                                                        )
                                                    }
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("💾", fontSize = 14.sp)
                                        }

                                        // VERTICALLY CENTERED TRASH DELETE BUTTON
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(EditorialSand.copy(alpha = 0.40f))
                                                .clickable { snapToDeleteId = snap.id },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text("🗑️", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // SAVED VIRTUAL TRY-ON OUTFITS
                if (savedOutfits.isNotEmpty()) {
                    Text(
                        "TRY-ONS (${savedOutfits.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        savedOutfits.forEach { outfit ->
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(outfit.colorHex.asComposeColor()),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("👔", fontSize = 24.sp)
                                    }

                                    Spacer(Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            outfit.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialInk,
                                        )
                                        Text(
                                            "${outfit.fabricName} • ${outfit.topwearCut}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EditorialMuted,
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(EditorialSand.copy(alpha = 0.5f))
                                                .clickable {
                                                    onNavigateToTryOn("silk", outfit.colorHex, null)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                "Try-On Again →",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EditorialSienna,
                                                fontSize = 11.sp,
                                            )
                                        }
                                    }

                                    // VERTICALLY CENTERED TRASH DELETE BUTTON
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(EditorialSand.copy(alpha = 0.40f))
                                            .clickable { outfitToDeleteId = outfit.id },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("🗑️", fontSize = 16.sp)
                                    }
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
                        containerColor = if (selectedCompareIds.size >= 2) EditorialSienna else EditorialInk.copy(alpha = 0.75f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        if (selectedCompareIds.size >= 2) {
                            "⚡ Compare ${selectedCompareIds.size} Looks Side-by-Side"
                        } else {
                            "⚡ Compare Looks (Select at least 2)"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
            }

            // DELETE CONFIRMATION DIALOG FOR DRAPE SNAPS
            if (snapToDeleteId != null) {
                AlertDialog(
                    onDismissRequest = { snapToDeleteId = null },
                    title = { Text("Delete Look?", fontWeight = FontWeight.Bold, color = EditorialInk) },
                    text = { Text("Are you sure you want to delete this look? This cannot be undone.", color = EditorialMuted) },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = snapToDeleteId!!
                                selectedCompareIds.remove(id)
                                DrapeSnapRepository.delete(context, id)
                                snaps = DrapeSnapRepository.list(context)
                                snapToDeleteId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialWarning),
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { snapToDeleteId = null }) {
                            Text("Cancel", color = EditorialInk)
                        }
                    },
                )
            }

            // DELETE CONFIRMATION DIALOG FOR OUTFITS
            if (outfitToDeleteId != null) {
                AlertDialog(
                    onDismissRequest = { outfitToDeleteId = null },
                    title = { Text("Remove Outfit?", fontWeight = FontWeight.Bold, color = EditorialInk) },
                    text = { Text("Are you sure you want to remove this saved outfit?", color = EditorialMuted) },
                    confirmButton = {
                        Button(
                            onClick = {
                                val id = outfitToDeleteId!!
                                WardrobeRepository.removeOutfit(context, id)
                                savedOutfits = WardrobeRepository.listOutfits(context)
                                outfitToDeleteId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialWarning),
                        ) {
                            Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { outfitToDeleteId = null }) {
                            Text("Cancel", color = EditorialInk)
                        }
                    },
                )
            }
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
