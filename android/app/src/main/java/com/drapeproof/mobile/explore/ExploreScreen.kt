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
    CuratedColorItem("Cognac Saddle", "#78350F", "leather", OccasionPreset.EVERYDAY),
    CuratedColorItem("Highland Tweed", "#4B5563", "tweed", OccasionPreset.OFFICE),
    CuratedColorItem("Deep Olive", "#3F6212", "linen", OccasionPreset.EVERYDAY),
    CuratedColorItem("Obsidian Noir", "#0F172A", "satin", OccasionPreset.FORMAL),
    CuratedColorItem("Amber Ochre", "#D97706", "corduroy", OccasionPreset.EVERYDAY),
    CuratedColorItem("Cobalt Navy", "#1D4ED8", "wool", OccasionPreset.OFFICE),
    CuratedColorItem("Champagne Rose", "#E2B8B3", "chiffon", OccasionPreset.EVENING),
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
    var selectedOccasion by remember { mutableStateOf(OccasionPreset.EVERYDAY) }

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
                "Curated occasion colorways & fabric pairings",
                style = MaterialTheme.typography.bodySmall,
                color = EditorialMuted,
            )

            Spacer(Modifier.height(16.dp))

            if (storedProfile == null) {
                // UNCALIBRATED STATE: PROMPT USER TO CAPTURE FIRST
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("🪞", fontSize = 40.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Calibrate in Drape Studio First",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EditorialInk,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Measure your skin undertone and snap a photo in the Drape studio to unlock personalized compatibility rankings tailored to your exact complexion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onNavigateToDrape("silk", "#831843") },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        ) {
                            Text("🪞 Open Drape Studio", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Universal Seasonal Palettes Preview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EditorialInk,
                )
                Spacer(Modifier.height(10.dp))
            } else {
                // CALIBRATED STATE: OCCASION PRESET TABS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OccasionPreset.values().forEach { preset ->
                        val isSelected = selectedOccasion == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) EditorialSienna else Color.White)
                                .border(1.dp, if (isSelected) EditorialSienna else EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .clickable { selectedOccasion = preset }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(preset.icon, fontSize = 14.sp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    preset.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else EditorialInk,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Occasion Guidance Pill
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialSand.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        selectedOccasion.focusAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = EditorialInk,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            // COLORWAY CARDS LIST
            val itemsToDisplay = if (storedProfile == null) curatedExplorePalette.take(4) else curatedExplorePalette.filter { it.occasionTag == selectedOccasion }

            itemsToDisplay.forEach { item ->
                val fabric = FabricCatalog.allFabrics.find { it.id == item.recommendedFabricId } ?: FabricCatalog.defaultFabric
                val skinHex = storedProfile?.skinHex ?: "#D8B498"
                val score = remember(skinHex, item.hex) {
                    TrueColorHarmonyEngine.evaluate(skinHex, item.hex)
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, EditorialStone.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Swatch circle
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(item.hex.asComposeColor())
                                .border(2.dp, EditorialStone, CircleShape),
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                            Text(
                                "Best with ${fabric.icon} ${fabric.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                            )
                            if (storedProfile != null) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "Match: ${score.scorePercent}% • ${score.harmonyLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (score.scorePercent >= 86) EditorialPositive else EditorialWarning,
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { onNavigateToDrape(fabric.id, item.hex) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                modifier = Modifier.height(32.dp),
                            ) {
                                Text("Drape", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onNavigateToTryOn(fabric.id, item.hex) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(32.dp),
                            ) {
                                Text("Try-On", color = EditorialInk, style = MaterialTheme.typography.labelSmall)
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
