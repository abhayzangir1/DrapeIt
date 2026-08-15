package com.drapeproof.mobile.explore

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

enum class OccasionCategory(val label: String, val icon: String) {
    EVERYDAY("Everyday", "☕"),
    WORK("Work", "💼"),
    EVENING("Evening", "🥂"),
    CASUAL("Casual", "🌿"),
}

data class CuratedPaletteItem(
    val id: String,
    val name: String,
    val hex: String,
    val fabricId: String,
    val forSeason: String,
    val category: OccasionCategory,
)

val allCuratedPaletteItems = listOf(
    // WARM AUTUMN
    CuratedPaletteItem("wa_1", "Terracotta Cashmere", "#9A3412", "wool", "Warm Autumn", OccasionCategory.EVERYDAY),
    CuratedPaletteItem("wa_2", "Amber Silk Blouse", "#D97706", "silk", "Warm Autumn", OccasionCategory.WORK),
    CuratedPaletteItem("wa_3", "Cognac Leather Jacket", "#78350F", "leather", "Warm Autumn", OccasionCategory.CASUAL),
    CuratedPaletteItem("wa_4", "Spiced Ochre Velvet", "#B45309", "velvet", "Warm Autumn", OccasionCategory.EVENING),
    CuratedPaletteItem("wa_5", "Deep Olive Trench", "#166534", "cotton", "Warm Autumn", OccasionCategory.WORK),
    CuratedPaletteItem("wa_6", "Warm Saffron Linen", "#CA8A04", "linen", "Warm Autumn", OccasionCategory.CASUAL),

    // COOL WINTER
    CuratedPaletteItem("cw_1", "Royal Burgundy Satin", "#831843", "satin", "Cool Winter", OccasionCategory.EVENING),
    CuratedPaletteItem("cw_2", "Midnight Plum Silk", "#4C1D95", "silk", "Cool Winter", OccasionCategory.EVENING),
    CuratedPaletteItem("cw_3", "Cobalt Navy Blazer", "#1E3A8A", "wool", "Cool Winter", OccasionCategory.WORK),
    CuratedPaletteItem("cw_4", "Deep Emerald Velvet", "#065F46", "velvet", "Cool Winter", OccasionCategory.EVENING),
    CuratedPaletteItem("cw_5", "Anthracite Wool Coat", "#0F172A", "wool", "Cool Winter", OccasionCategory.WORK),
    CuratedPaletteItem("cw_6", "Ruby Rose Blouse", "#E11D48", "silk", "Cool Winter", OccasionCategory.EVERYDAY),

    // COOL SUMMER
    CuratedPaletteItem("cs_1", "Classic Slate Linen", "#475569", "linen", "Cool Summer", OccasionCategory.EVERYDAY),
    CuratedPaletteItem("cs_2", "Deep Teal Silk Top", "#0E7490", "silk", "Cool Summer", OccasionCategory.WORK),
    CuratedPaletteItem("cs_3", "Royal Blue Oxford", "#2563EB", "cotton", "Cool Summer", OccasionCategory.WORK),
    CuratedPaletteItem("cs_4", "Imperial Purple Velvet", "#6B21A8", "velvet", "Cool Summer", OccasionCategory.EVENING),
    CuratedPaletteItem("cs_5", "Forest Pine Cashmere", "#059669", "wool", "Cool Summer", OccasionCategory.CASUAL),

    // WARM SPRING
    CuratedPaletteItem("ws_1", "Coral Silk Shirt", "#F97316", "silk", "Warm Spring", OccasionCategory.EVERYDAY),
    CuratedPaletteItem("ws_2", "Goldenrod Linen Dress", "#EAB308", "linen", "Warm Spring", OccasionCategory.CASUAL),
    CuratedPaletteItem("ws_3", "Jade Green Satin", "#10B981", "satin", "Warm Spring", OccasionCategory.EVENING),
    CuratedPaletteItem("ws_4", "Warm Camel Coat", "#92400E", "wool", "Warm Spring", OccasionCategory.WORK),
)

@Composable
fun ExploreScreen(
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToTryOn: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf(OccasionCategory.EVERYDAY) }
    val storedProfile = remember { SkinProfileRepository.load(context) }
    var revealedHexItemId by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
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
                "Explore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Curated seasonal wardrobe palettes & textures",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))

            val currentProfile = storedProfile

            if (currentProfile == null || !currentProfile.isCalibrated) {
                // MINIMAL EMPTY STATE
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("✨", fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Setup Color Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Scan or calibrate skin tone to view personalized palettes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                        ) {
                            Text("Setup Profile", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
            } else {
                // COMPACT SEASON PROFILE CHIP
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(currentProfile.skinHex.asComposeColor())
                                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentProfile.season,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "${currentProfile.undertone} • ${currentProfile.bestMetals}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // OCCASION CATEGORY SELECTOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OccasionCategory.values().forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (isSel) EditorialSienna else MaterialTheme.colorScheme.outline.copy(alpha = 0.50f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                "${cat.icon} ${cat.label}",
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "CURATED LOOKBOOK GRID",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EditorialSienna,
                    letterSpacing = 1.2.sp,
                )

                Spacer(Modifier.height(10.dp))

                val matchingItems = allCuratedPaletteItems.filter {
                    it.forSeason.equals(currentProfile.season, ignoreCase = true) && it.category == selectedCategory
                }.ifEmpty {
                    allCuratedPaletteItems.filter { it.forSeason.equals(currentProfile.season, ignoreCase = true) }
                }.ifEmpty {
                    allCuratedPaletteItems.take(6)
                }

                // 2-COLUMN AESTHETIC LOOKBOOK GRID
                val chunkedPairs = matchingItems.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedPairs.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            pair.forEach { item ->
                                val fabric = FabricCatalog.findById(item.fabricId)
                                val eval = TrueColorHarmonyEngine.evaluate(
                                    skinHex = currentProfile.skinHex,
                                    fabricHex = item.hex,
                                    fabricId = item.fabricId,
                                )
                                val isHexRevealed = revealedHexItemId == item.id

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // TOP SWATCH CARD WITH MATCH BADGE
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(90.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(item.hex.asComposeColor())
                                                .clickable {
                                                    revealedHexItemId = if (isHexRevealed) null else item.id
                                                }
                                                .padding(8.dp),
                                        ) {
                                            // MATCH SCORE BADGE
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.65f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    "${eval.scorePercent}%",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                )
                                            }

                                            // TAP TO REVEAL HEX TOAST/PILL
                                            if (isHexRevealed) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.White.copy(alpha = 0.90f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        item.hex.uppercase(),
                                                        color = EditorialInk,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp,
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "${fabric.icon} ${fabric.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                        )

                                        Spacer(Modifier.height(10.dp))

                                        // ACTION BUTTONS
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            OutlinedButton(
                                                onClick = { onNavigateToDrape(item.fabricId, item.hex) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                            ) {
                                                Text("Drape", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                                            }

                                            Button(
                                                onClick = { onNavigateToTryOn(item.fabricId, item.hex) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                            ) {
                                                Text("Try-On", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
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

                Spacer(Modifier.height(24.dp))
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
