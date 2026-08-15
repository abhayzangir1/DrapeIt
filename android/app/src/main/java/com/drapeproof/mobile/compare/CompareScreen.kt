package com.drapeproof.mobile.compare

import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.data.DrapeSnap
import com.drapeproof.mobile.data.DrapeSnapRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning
import java.io.File

@Composable
fun CompareScreen(
    initialSelectedIds: List<String> = emptyList(),
    onBack: () -> Unit,
    onSelectLookForTryOn: (fabricId: String, colorHex: String) -> Unit,
) {
    val context = LocalContext.current
    var snaps by remember { mutableStateOf(DrapeSnapRepository.list(context)) }
    val selectedSnapIds = remember {
        mutableStateListOf<String>().apply {
            if (initialSelectedIds.isNotEmpty()) {
                addAll(initialSelectedIds)
            } else if (snaps.isNotEmpty()) {
                addAll(snaps.take(4).map { it.id })
            }
        }
    }

    val selectedSnaps = snaps.filter { it.id in selectedSnapIds }
    val winnerSnap = selectedSnaps.maxByOrNull { it.matchScorePercent }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(12.dp))

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
                        color = EditorialInk,
                    )
                    Text(
                        "Side-by-side drape evaluation",
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialMuted,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EditorialSand)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", color = EditorialInk, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (snaps.isEmpty()) {
                // Empty state when no photos captured in Drape studio
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
                        color = EditorialInk,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Capture drape portraits in the studio to compare fabrics and colors side-by-side.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditorialMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    ) {
                        Text("Open Drape Studio", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // SELECTION CAROUSEL / STRIP
                    Text(
                        "SELECTED LOOKS (${selectedSnapIds.size}/4)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        letterSpacing = 1.2.sp,
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        snaps.forEach { snap ->
                            val isSelected = snap.id in selectedSnapIds
                            val file = File(snap.imagePath)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) EditorialSienna else EditorialStone.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedSnapIds.size > 1) selectedSnapIds.remove(snap.id)
                                        } else {
                                            if (selectedSnapIds.size < 4) selectedSnapIds.add(snap.id)
                                        }
                                    },
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    if (file.exists()) {
                                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                        if (bmp != null) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = snap.fabricName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }

                                    // Color & Fabric Tag at bottom
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.70f))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "${snap.matchScorePercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }

                                    // Delete icon at top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(20.dp)
                                            .background(Color.Black.copy(alpha = 0.60f), CircleShape)
                                            .clickable {
                                                DrapeSnapRepository.delete(context, snap.id)
                                                selectedSnapIds.remove(snap.id)
                                                snaps = DrapeSnapRepository.list(context)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("✕", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // WINNER BANNER
                    winnerSnap?.let { winner ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = EditorialPositive.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("✨", fontSize = 24.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "${winner.fabricName} (${winner.colorName}) Wins (${winner.matchScorePercent}%)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EditorialInk,
                                    )
                                    Text(
                                        "Produces the cleanest contrast balance against your complexion.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EditorialMuted,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // DYNAMIC PHOTO COLLAGE (2-WAY SPLIT OR 2x2 QUAD GRID)
                    Text(
                        "Comparison Collage",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialMuted,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (selectedSnaps.size == 1) {
                        // 1 PHOTO: FULL IMMERSIVE VIEW
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
                        // 2 PHOTOS: EXPANSIVE SIDE-BY-SIDE VERTICAL SPLIT
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        // 3 OR 4 PHOTOS: 2x2 QUAD GRID WITH GENEROUS HEIGHT
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Row 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(230.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                selectedSnaps.take(2).forEach { snap ->
                                    CollagePhotoCard(
                                        snap = snap,
                                        isWinner = snap.id == winnerSnap?.id,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                    )
                                }
                            }

                            // Row 2
                            if (selectedSnaps.size > 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    selectedSnaps.drop(2).take(2).forEach { snap ->
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

                    Spacer(Modifier.height(20.dp))

                    // ACTION BUTTON
                    winnerSnap?.let { winner ->
                        Button(
                            onClick = { onSelectLookForTryOn(winner.fabricId, winner.colorHex) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        ) {
                            Text("📸 AI Virtual Try-On Winning Look →", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isWinner) 6.dp else 2.dp),
        modifier = modifier.border(
            width = if (isWinner) 2.5.dp else 1.dp,
            color = if (isWinner) EditorialPositive else EditorialStone.copy(alpha = 0.35f),
            shape = RoundedCornerShape(18.dp),
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
                // Fallback swatch background
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
