package com.drapeproof.mobile.explore

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning

private enum class OccasionPreset(val label: String, val icon: String, val focusAdvice: String) {
    EVERYDAY("Everyday", "🌿", "Comfortable, breathable neutrals with balanced contrast."),
    OFFICE("Office & Work", "💼", "Structured tailoring in deep navy, charcoal, and crisp olive."),
    EVENING("Evening Occasion", "✨", "Luminous jewel tones in mulberry silk and lustrous velvet."),
    FORMAL("Formal / Gala", "🎩", "High-contrast blacks, royal burgundies, and crisp whites."),
}

private data class CuratedColorItem(val name: String, val hex: String, val recommendedFabricId: String, val occasionTag: OccasionPreset)

private val curatedExplorePalette = listOf(
    CuratedColorItem("Royal Burgundy", "#831843", "silk", OccasionPreset.EVENING),
    CuratedColorItem("Deep Olive", "#3F6212", "linen", OccasionPreset.EVERYDAY),
    CuratedColorItem("Cobalt Navy", "#1D4ED8", "wool", OccasionPreset.OFFICE),
    CuratedColorItem("Terracotta Clay", "#B45309", "cotton", OccasionPreset.EVERYDAY),
    CuratedColorItem("Emerald Pine", "#047857", "velvet", OccasionPreset.EVENING),
    CuratedColorItem("Midnight Charcoal", "#1F2937", "cashmere", OccasionPreset.FORMAL),
    CuratedColorItem("Warm Ivory", "#F7EFE8", "silk", OccasionPreset.OFFICE),
    CuratedColorItem("Dusty Rose", "#FDA4AF", "linen", OccasionPreset.EVERYDAY),
)

@Composable
fun ExploreScreen(
    onNavigateToDrape: (fabricId: String, colorHex: String) -> Unit,
    onNavigateToTryOn: (fabricId: String, colorHex: String) -> Unit,
) {
    val context = LocalContext.current
    val storedProfile = remember { SkinProfileRepository.load(context) }
    val effectiveSkinHex = storedProfile?.skinHex ?: "#D8B498"

    var selectedOccasion by remember { mutableStateOf(OccasionPreset.EVERYDAY) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    val filteredColors = curatedExplorePalette.filter { it.occasionTag == selectedOccasion }

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
                "Curated colorways & fabrics matched to your complexion",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(18.dp))

            // OCCASION SELECTOR TABS
            Text(
                "Occasion Intent",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = EditorialMuted,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OccasionPreset.values().forEach { occasion ->
                    val isSelected = selectedOccasion == occasion
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedOccasion = occasion },
                        label = { Text("${occasion.icon} ${occasion.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EditorialSienna,
                            selectedLabelColor = Color.White,
                            containerColor = EditorialSand.copy(alpha = 0.5f),
                            labelColor = EditorialInk,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Occasion Advice Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = EditorialSand.copy(alpha = 0.60f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    selectedOccasion.focusAdvice,
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialInk,
                    modifier = Modifier.padding(14.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // TOP MATCHED COMBINATIONS
            Text(
                "Your Winning Matches",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = EditorialInk,
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filteredColors.forEach { item ->
                    val harmony = TrueColorHarmonyEngine.evaluate(effectiveSkinHex, item.hex)
                    val fabric = FabricCatalog.allFabrics.firstOrNull { it.id == item.recommendedFabricId } ?: FabricCatalog.defaultFabric

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(item.hex.asComposeColor())
                                            .border(1.dp, EditorialStone, CircleShape),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialInk,
                                        )
                                        Text(
                                            "Paired with ${fabric.name} ${fabric.icon}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EditorialMuted,
                                        )
                                    }
                                }

                                // Score Pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (harmony.scorePercent >= 86) EditorialPositive.copy(alpha = 0.15f)
                                            else EditorialWarning.copy(alpha = 0.15f),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "${harmony.scorePercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (harmony.scorePercent >= 86) EditorialPositive else EditorialWarning,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                harmony.summaryFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { onNavigateToDrape(fabric.id, item.hex) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("🪞 Live Drape", color = EditorialInk, style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { onNavigateToTryOn(fabric.id, item.hex) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("📸 AI Try-On →", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
