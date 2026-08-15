package com.drapeproof.mobile.looks

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SavedTryOnOutfit
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.data.WardrobeRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import java.io.File

@Composable
fun LooksScreen(
    onNavigateToTryOn: (fabricId: String, colorHex: String, garmentUri: Uri?) -> Unit,
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToCompare: () -> Unit,
) {
    val context = LocalContext.current
    var snaps by remember { mutableStateOf(DrapeSnapRepository.list(context)) }
    var savedOutfits by remember { mutableStateOf(WardrobeRepository.listOutfits(context)) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = EditorialCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(14.dp))

            Text(
                "Looks & Studio",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Text(
                "Virtual Try-On, Photo Compare & your saved lookbook",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(16.dp))

            // TOP SECTION: TWO MAJOR FEATURE TILES [ VIRTUAL TRY-ON ] & [ PHOTO COMPARE ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // TILE 1: VIRTUAL TRY-ON
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToTryOn("silk", "#831843", null) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialSienna.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("👗", fontSize = 22.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Virtual Try-On",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Try garments, store screenshots & instant color swap",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                            lineHeight = 16.sp,
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Open Studio →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                        )
                    }
                }

                // TILE 2: PHOTO COMPARE
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { onNavigateToCompare() },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EditorialPositive.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("📸", fontSize = 22.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "Photo Compare",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (snaps.isNotEmpty()) "${snaps.size} looks ready to compare side-by-side" else "Compare 2-4 drape snaps side-by-side",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                            lineHeight = 16.sp,
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            "Compare Looks →",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialSienna,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            // SAVED DRAPE SNAPS GALLERY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "CAPTURED DRAPE LOOKS (${snaps.size})",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EditorialSienna,
                    letterSpacing = 1.sp,
                )

                if (snaps.size >= 2) {
                    Text(
                        "⚖️ Compare All",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EditorialSienna,
                        modifier = Modifier.clickable { onNavigateToCompare() },
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
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🪞", fontSize = 32.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No Drape Snaps Saved Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                        )
                        Text(
                            "Snap looks with the camera shutter button in Drape Studio to build your comparison library.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    snaps.forEach { snap ->
                        val file = File(snap.imagePath)
                        val bmp = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

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
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = snap.fabricName,
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(RoundedCornerShape(12.dp))
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
                                    Spacer(Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Try-On →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialSienna,
                                            modifier = Modifier.clickable {
                                                onNavigateToTryOn(snap.fabricId, snap.colorHex, null)
                                            },
                                        )
                                        Text(
                                            "Delete",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialMuted,
                                            modifier = Modifier.clickable {
                                                DrapeSnapRepository.delete(context, snap.id)
                                                snaps = DrapeSnapRepository.list(context)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // SAVED VIRTUAL TRY-ON OUTFITS
            if (savedOutfits.isNotEmpty()) {
                Text(
                    "SAVED TRY-ON LOOKS (${savedOutfits.size})",
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
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(outfit.colorHex.asComposeColor()),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text("👗", fontSize = 24.sp)
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
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Try-On Again →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialSienna,
                                            modifier = Modifier.clickable {
                                                onNavigateToTryOn("silk", outfit.colorHex, null)
                                            },
                                        )
                                        Text(
                                            "Remove",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialMuted,
                                            modifier = Modifier.clickable {
                                                WardrobeRepository.removeOutfit(context, outfit.id)
                                                savedOutfits = WardrobeRepository.listOutfits(context)
                                            },
                                        )
                                    }
                                }
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
