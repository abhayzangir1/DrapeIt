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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

private enum class OccasionCategory(val label: String, val icon: String) {
    EVERYDAY("Everyday", "🌿"),
    WORK("Work", "💼"),
    EVENING("Evening", "✨"),
    WEEKEND("Casual", "☕"),
}

private data class SeasonalPaletteItem(
    val name: String,
    val hex: String,
    val fabricId: String,
    val category: OccasionCategory,
    val forSeason: String,
)

private val allCuratedPaletteItems = listOf(
    // Deep Autumn items
    SeasonalPaletteItem("Royal Burgundy", "#831843", "silk", OccasionCategory.EVENING, "Deep Autumn"),
    SeasonalPaletteItem("Cognac Saddle", "#78350F", "leather", OccasionCategory.EVERYDAY, "Deep Autumn"),
    SeasonalPaletteItem("Deep Olive", "#3F6212", "linen", OccasionCategory.WEEKEND, "Deep Autumn"),
    SeasonalPaletteItem("Amber Ochre", "#D97706", "cotton", OccasionCategory.WORK, "Deep Autumn"),
    SeasonalPaletteItem("Obsidian Noir", "#0F172A", "satin", OccasionCategory.EVENING, "Deep Autumn"),
    SeasonalPaletteItem("Terracotta Clay", "#B45309", "cotton", OccasionCategory.EVERYDAY, "Deep Autumn"),

    // True Winter items
    SeasonalPaletteItem("Cobalt Navy", "#1D4ED8", "wool", OccasionCategory.WORK, "True Winter"),
    SeasonalPaletteItem("Emerald Pine", "#047857", "velvet", OccasionCategory.EVENING, "True Winter"),
    SeasonalPaletteItem("Royal Velvet", "#4C1D95", "velvet", OccasionCategory.EVENING, "True Winter"),
    SeasonalPaletteItem("Midnight Charcoal", "#1F2937", "cashmere", OccasionCategory.WORK, "True Winter"),
    SeasonalPaletteItem("Pure Obsidian", "#0F172A", "satin", OccasionCategory.EVENING, "True Winter"),
    SeasonalPaletteItem("Crisp Denim", "#2563EB", "denim", OccasionCategory.WEEKEND, "True Winter"),

    // Warm Spring items
    SeasonalPaletteItem("Sunlit Coral", "#EA580C", "silk", OccasionCategory.EVENING, "Warm Spring"),
    SeasonalPaletteItem("Warm Camel", "#CA8A04", "wool", OccasionCategory.WORK, "Warm Spring"),
    SeasonalPaletteItem("Moss Olive", "#65A30D", "linen", OccasionCategory.EVERYDAY, "Warm Spring"),
    SeasonalPaletteItem("Warm Ivory", "#F7EFE8", "silk", OccasionCategory.WORK, "Warm Spring"),
    SeasonalPaletteItem("Spiced Ochre", "#D97706", "cotton", OccasionCategory.WEEKEND, "Warm Spring"),

    // Soft Summer items
    SeasonalPaletteItem("Dusty Rose", "#BE185D", "silk", OccasionCategory.EVENING, "Soft Summer"),
    SeasonalPaletteItem("Slate Navy", "#475569", "wool", OccasionCategory.WORK, "Soft Summer"),
    SeasonalPaletteItem("French Blue", "#2563EB", "linen", OccasionCategory.WEEKEND, "Soft Summer"),
    SeasonalPaletteItem("Soft Mauve", "#9333EA", "velvet", OccasionCategory.EVENING, "Soft Summer"),
    SeasonalPaletteItem("Heather Gray", "#64748B", "cotton", OccasionCategory.EVERYDAY, "Soft Summer"),
)

@Composable
fun ExploreScreen(
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToTryOn: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
) {
    val context = LocalContext.current
    var storedProfile by remember { mutableStateOf(SkinProfileRepository.load(context)) }

    LaunchedEffect(Unit) {
        storedProfile = SkinProfileRepository.load(context)
    }

    var selectedCategory by remember { mutableStateOf(OccasionCategory.EVERYDAY) }

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
                "Explore",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )

            Spacer(Modifier.height(14.dp))

            val currentProfile = storedProfile

            if (currentProfile == null || !currentProfile.isCalibrated) {
                // MINIMAL EMPTY STATE
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(18.dp)),
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
                            color = EditorialInk,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Scan or upload a photo to view personalized palettes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier.fillMaxWidth(),
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
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(14.dp)),
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
                                .border(1.dp, EditorialStone, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentProfile.season,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                            Text(
                                "${currentProfile.undertone} • ${currentProfile.bestMetals}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EditorialMuted,
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) EditorialSienna else Color.White)
                                .border(1.dp, if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                "${cat.icon} ${cat.label}",
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else EditorialInk,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "CURATED PALETTE",
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
                    allCuratedPaletteItems.take(4)
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    matchingItems.forEach { item ->
                        val fabric = FabricCatalog.findById(item.fabricId)
                        val eval = TrueColorHarmonyEngine.evaluate(currentProfile.skinHex, item.hex)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(item.hex.asComposeColor())
                                            .border(1.5.dp, EditorialStone, CircleShape),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialInk,
                                        )
                                        Text(
                                            "${fabric.icon} ${fabric.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = EditorialMuted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EditorialPositive.copy(alpha = 0.10f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                    ) {
                                        Text("${eval.scorePercent}%", color = EditorialPositive, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onNavigateToDrape(item.fabricId, item.hex) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        Text("Drape", color = EditorialInk, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = { onNavigateToTryOn(item.fabricId, item.hex) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp),
                                    ) {
                                        Text("Try-On", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
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
