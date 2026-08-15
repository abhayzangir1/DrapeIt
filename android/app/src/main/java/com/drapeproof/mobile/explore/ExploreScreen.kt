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
    EVERYDAY("Everyday Basics", "🌿"),
    WORK("Work & Tailoring", "💼"),
    EVENING("Evening & Party", "✨"),
    WEEKEND("Weekend Casual", "☕"),
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
                "Explore Palettes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Text(
                "Curated occasion colors & fabrics personalized to your profile",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(16.dp))

            val currentProfile = storedProfile

            if (currentProfile == null || !currentProfile.isCalibrated) {
                // EMPTY STATE: PROMPT USER TO SETUP PROFILE
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(22.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("✨", fontSize = 46.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Setup Your Color Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Scan your face once or upload a photo to unlock your personal seasonal wardrobe, signature flattering shades, and custom fabric pairings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EditorialMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = onNavigateToProfile,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("📸 Scan Face to Setup Profile", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
                // CALIBRATED STATE: SEASON PROFILE BANNER
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.30f), RoundedCornerShape(18.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(currentProfile.skinHex.asComposeColor())
                                .border(2.dp, EditorialSand, CircleShape),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    currentProfile.season,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EditorialInk,
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EditorialPositive.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Text("YOUR SEASON", color = EditorialPositive, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                }
                            }
                            Text(
                                "Undertone: ${currentProfile.undertone} • ${currentProfile.bestMetals}",
                                style = MaterialTheme.typography.bodySmall,
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) EditorialSienna else Color.White)
                                .border(1.dp, if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
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

                Spacer(Modifier.height(16.dp))

                // Filter items by user season and selected occasion
                val matchingItems = allCuratedPaletteItems.filter {
                    it.forSeason.equals(currentProfile.season, ignoreCase = true) && it.category == selectedCategory
                }.ifEmpty {
                    // Fallback to all items for the user's season if none match that specific occasion category
                    allCuratedPaletteItems.filter { it.forSeason.equals(currentProfile.season, ignoreCase = true) }
                }.ifEmpty {
                    // Or default palette items
                    allCuratedPaletteItems.take(4)
                }

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    matchingItems.forEach { item ->
                        val fabric = FabricCatalog.findById(item.fabricId)
                        val eval = TrueColorHarmonyEngine.evaluate(currentProfile.skinHex, item.hex)

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(item.hex.asComposeColor())
                                                .border(2.dp, EditorialStone.copy(alpha = 0.4f), CircleShape),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                                            Text("${fabric.icon} ${fabric.name} • ${item.hex}", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EditorialPositive.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text("${eval.scorePercent}% Match", color = EditorialPositive, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    eval.summaryFeedback,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EditorialInk.copy(alpha = 0.85f),
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { onNavigateToDrape(item.fabricId, item.hex) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("🪞 Try in Drape", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = { onNavigateToTryOn(item.fabricId, item.hex) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text("👗 AI Try-On", color = EditorialInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
