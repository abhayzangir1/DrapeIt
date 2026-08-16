package com.drapeproof.mobile.compare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.DrapeSnap
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.ui.sound.SoundEffectManager
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialWarning
import com.drapeproof.mobile.util.ImageExportUtils
import java.io.File

@Composable
fun CompareScreen(
    initialSelectedIds: List<String> = emptyList(),
    onBack: () -> Unit,
    onSelectLookForTryOn: (fabricId: String, colorHex: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val currentView = LocalView.current
    var snaps by remember { mutableStateOf(DrapeSnapRepository.list(context)) }
    val selectedSnapIds = remember {
        mutableStateListOf<String>().apply {
            if (initialSelectedIds.isNotEmpty()) {
                addAll(initialSelectedIds)
            } else {
                addAll(snaps.take(2).map { it.id })
            }
        }
    }

    var isExporting by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    val selectedSnaps = snaps.filter { it.id in selectedSnapIds }
    val winnerSnap = selectedSnaps.maxByOrNull { it.matchScorePercent }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(18.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Compare",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Side-by-side drape evaluation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .clickable {
                            SoundEffectManager.playTap(currentView)
                            onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(18.dp))

            if (snaps.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("📸", fontSize = 48.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "No Looks to Compare",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Capture drape portraits in the studio to compare fabrics and colors side-by-side.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            SoundEffectManager.playTap(currentView)
                            onBack()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text("Open Drape Studio", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    // SELECTION STRIP (CHOOSE 2-4 LOOKS TO COMPARE)
                    Text(
                        "SELECT LOOKS TO COMPARE (UP TO 4)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        snaps.forEach { snap ->
                            val isSelected = snap.id in selectedSnapIds
                            val file = File(snap.imagePath)
                            val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(android.graphics.Color.parseColor(snap.colorHex)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.40f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        SoundEffectManager.playTap(currentView)
                                        if (isSelected) {
                                            selectedSnapIds.remove(snap.id)
                                        } else {
                                            if (selectedSnapIds.size < 4) {
                                                selectedSnapIds.add(snap.id)
                                            }
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
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(EditorialSienna),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✓", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // WINNER BANNER
                    winnerSnap?.let { winner ->
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = EditorialPositive.copy(alpha = 0.12f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EditorialPositive.copy(alpha = 0.30f), RoundedCornerShape(20.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("✨", fontSize = 24.sp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "${winner.fabricName} (${winner.colorName}) Wins (${winner.matchScorePercent}%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Produces the cleanest contrast balance against your complexion.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // DYNAMIC PHOTO COLLAGE (2-WAY SPLIT OR 2x2 QUAD GRID)
                    Text(
                        "COMPARISON COLLAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(10.dp))

                    if (selectedSnaps.size == 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                        ) {
                            CollagePhotoCard(
                                snap = selectedSnaps[0],
                                isWinner = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else if (selectedSnaps.size == 2) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            selectedSnaps.forEach { snap ->
                                CollagePhotoCard(
                                    snap = snap,
                                    isWinner = snap.id == winnerSnap?.id,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                selectedSnaps.take(2).forEach { snap ->
                                    CollagePhotoCard(
                                        snap = snap,
                                        isWinner = snap.id == winnerSnap?.id,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }
                            if (selectedSnaps.size > 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    selectedSnaps.drop(2).forEach { snap ->
                                        CollagePhotoCard(
                                            snap = snap,
                                            isWinner = snap.id == winnerSnap?.id,
                                            modifier = Modifier.weight(1f).fillMaxHeight(),
                                        )
                                    }
                                    if (selectedSnaps.size == 3) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ACTION BUTTONS: [ Export Collage ] + [ Try-On Winner ]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedSnaps.isEmpty()) return@OutlinedButton
                                SoundEffectManager.playTap(currentView)
                                isExporting = true
                                val bitmaps = selectedSnaps.mapNotNull { snap ->
                                    val f = File(snap.imagePath)
                                    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
                                }
                                if (bitmaps.isNotEmpty()) {
                                    val collage = ImageExportUtils.createSideBySideCollage(
                                        context = context,
                                        bitmaps = bitmaps,
                                        labels = selectedSnaps.map { "${it.fabricName} (${it.matchScorePercent}%)" },
                                    )
                                    ImageExportUtils.saveImageToGallery(
                                        context = context,
                                        bitmap = collage,
                                        title = "DrapeIt_Comparison_${System.currentTimeMillis()}",
                                    )
                                    exportStatusMessage = "Collage saved to Photo Gallery!"
                                    SoundEffectManager.playSuccess(currentView)
                                }
                                isExporting = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Text("💾 Export Collage", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                        }

                        winnerSnap?.let { winner ->
                            Button(
                                onClick = {
                                    SoundEffectManager.playTap(currentView)
                                    onSelectLookForTryOn(winner.fabricId, winner.colorHex)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text("Try On Winner", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (exportStatusMessage != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            exportStatusMessage!!,
                            color = EditorialPositive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollagePhotoCard(
    snap: DrapeSnap,
    isWinner: Boolean,
    modifier: Modifier = Modifier,
) {
    val file = File(snap.imagePath)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isWinner) 6.dp else 2.dp),
        modifier = modifier.border(
            width = if (isWinner) 2.5.dp else 1.dp,
            color = if (isWinner) EditorialPositive else Color.White.copy(alpha = 0.25f),
            shape = RoundedCornerShape(20.dp),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val bmp = remember(snap.imagePath) {
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }

            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = snap.fabricName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(android.graphics.Color.parseColor(snap.colorHex))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        snap.fabricName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // Winner Ribbon
            if (isWinner) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(EditorialPositive)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text("✨ WINNER", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp)
                }
            }

            // Bottom Caption Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            snap.fabricName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp,
                        )
                        Text(
                            snap.colorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 9.sp,
                        )
                    }
                    Text(
                        "${snap.matchScorePercent}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (snap.matchScorePercent >= 86) EditorialPositive else EditorialWarning,
                    )
                }
            }
        }
    }
}
