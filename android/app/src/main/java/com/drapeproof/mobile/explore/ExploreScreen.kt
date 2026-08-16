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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.profile.ProfileSettingsModal
import com.drapeproof.mobile.ui.sound.SoundEffectManager
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialSienna

enum class OccasionCategory(val label: String, val icon: String) {
    EVERYDAY("Daily Wear", "👕"),
    WORK("Formal Work", "💼"),
    EVENING("Evening Gala", "✨"),
    CASUAL("Weekend Casual", "☕"),
}

data class CuratedPaletteItem(
    val id: String,
    val title: String,
    val hex: String,
    val fabricId: String,
    val forSeason: String,
    val category: OccasionCategory,
)

data class OccasionStyleTemplate(
    val title: String,
    val fabricId: String,
)

private val WORK_TEMPLATES = listOf(
    OccasionStyleTemplate("Tailored Wool Blazer", "wool"),
    OccasionStyleTemplate("Crisp Oxford Poplin", "cotton"),
    OccasionStyleTemplate("Structured Tweed Coat", "tweed"),
    OccasionStyleTemplate("Executive Linen Trousers", "linen"),
    OccasionStyleTemplate("Fine Merino Overcoat", "wool"),
    OccasionStyleTemplate("Tailored Silk Blouse", "silk"),
)

private val EVENING_TEMPLATES = listOf(
    OccasionStyleTemplate("Lustrous Silk Gown", "silk"),
    OccasionStyleTemplate("Plush Velvet Dinner Wrap", "velvet"),
    OccasionStyleTemplate("Imperial Satin Top", "satin"),
    OccasionStyleTemplate("Midnight Cashmere Stole", "wool"),
    OccasionStyleTemplate("Glossy Velvet Tuxedo", "velvet"),
    OccasionStyleTemplate("Duchess Satin Slip", "satin"),
)

private val CASUAL_TEMPLATES = listOf(
    OccasionStyleTemplate("Cozy Knit Sweater", "knit"),
    OccasionStyleTemplate("Washed Denim Overshirt", "denim"),
    OccasionStyleTemplate("Breezy Weekend Linen", "linen"),
    OccasionStyleTemplate("Relaxed Corduroy Jacket", "corduroy"),
    OccasionStyleTemplate("Soft Cotton Henley", "cotton"),
    OccasionStyleTemplate("Chunky Knit Cardigan", "knit"),
)

private val EVERYDAY_TEMPLATES = listOf(
    OccasionStyleTemplate("Signature Cotton Top", "cotton"),
    OccasionStyleTemplate("Everyday Linen Button-Down", "linen"),
    OccasionStyleTemplate("Lightweight Knit Pullover", "knit"),
    OccasionStyleTemplate("Daylight Silk Blouse", "silk"),
    OccasionStyleTemplate("Classic Jersey Tee", "cotton"),
    OccasionStyleTemplate("Versatile Twill Overshirt", "cotton"),
)

val allCuratedPaletteItems = listOf(
    // WARM AUTUMN
    CuratedPaletteItem("wa_1", "Crimson Silk Blouse", "#831843", "silk", "Warm Autumn", OccasionCategory.EVERYDAY),
    CuratedPaletteItem("wa_2", "Warm Cognac Blazer", "#78350F", "wool", "Warm Autumn", OccasionCategory.WORK),
    CuratedPaletteItem("wa_3", "Amber Ochre Sweater", "#D97706", "knit", "Warm Autumn", OccasionCategory.CASUAL),
    CuratedPaletteItem("wa_4", "Deep Forest Velvet", "#065F46", "velvet", "Warm Autumn", OccasionCategory.EVENING),
    CuratedPaletteItem("wa_5", "Terracotta Linen Dress", "#9A3412", "linen", "Warm Autumn", OccasionCategory.CASUAL),

    // DEEP WINTER
    CuratedPaletteItem("dw_1", "Midnight Navy Tuxedo", "#0F172A", "wool", "Deep Winter", OccasionCategory.WORK),
    CuratedPaletteItem("dw_2", "Imperial Ruby Satin", "#E11D48", "satin", "Deep Winter", OccasionCategory.EVENING),
    CuratedPaletteItem("dw_3", "Cobalt Tweed Coat", "#1E3A8A", "tweed", "Deep Winter", OccasionCategory.WORK),
    CuratedPaletteItem("dw_4", "Plum Corduroy Jacket", "#4C1D95", "corduroy", "Deep Winter", OccasionCategory.CASUAL),
    CuratedPaletteItem("dw_5", "Charcoal Plain Knit", "#334155", "knit", "Deep Winter", OccasionCategory.EVERYDAY),

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
    val currentView = LocalView.current
    var selectedCategory by remember { mutableStateOf(OccasionCategory.EVERYDAY) }
    val storedProfile = remember { SkinProfileRepository.load(context) }
    var revealedHexItemId by remember { mutableStateOf<String?>(null) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(18.dp))

            // TOP HEADER BAR WITH SETTINGS ICON
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Explore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Curated seasonal wardrobe palettes & textures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                        .clickable {
                            SoundEffectManager.playTap(currentView)
                            isSettingsOpen = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚙️", fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(22.dp))

            val currentProfile = storedProfile

            if (currentProfile == null || !currentProfile.isCalibrated) {
                // MINIMAL EMPTY STATE
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(22.dp)),
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
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
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                SoundEffectManager.playTap(currentView)
                                onNavigateToProfile()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                        ) {
                            Text("Setup Profile", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            } else {
                // COMPACT SEASON PROFILE CHIP
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(currentProfile.skinHex.asComposeColor())
                                .border(2.dp, EditorialGold, CircleShape),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                currentProfile.season,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${currentProfile.undertone} • ${currentProfile.bestMetals}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // OCCASION CATEGORY SELECTOR
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OccasionCategory.values().forEach { cat ->
                        val isSel = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (isSel) EditorialGold.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    RoundedCornerShape(14.dp),
                                )
                                .clickable {
                                    SoundEffectManager.playTap(currentView)
                                    selectedCategory = cat
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
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

                Spacer(Modifier.height(24.dp))

                Text(
                    "CURATED LOOKBOOK GRID",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EditorialSienna,
                    letterSpacing = 1.2.sp,
                )

                Spacer(Modifier.height(14.dp))

                val matchingItems = if (currentProfile != null && currentProfile.bestColors.isNotEmpty()) {
                    val templates = when (selectedCategory) {
                        OccasionCategory.WORK -> WORK_TEMPLATES
                        OccasionCategory.EVENING -> EVENING_TEMPLATES
                        OccasionCategory.CASUAL -> CASUAL_TEMPLATES
                        OccasionCategory.EVERYDAY -> EVERYDAY_TEMPLATES
                    }
                    val palette = currentProfile.bestColors
                    templates.mapIndexed { index, template ->
                        val colorHex = palette[(index + selectedCategory.ordinal * 2) % palette.size]
                        CuratedPaletteItem(
                            id = "${selectedCategory.name.lowercase()}_$index",
                            title = template.title,
                            hex = colorHex,
                            fabricId = template.fabricId,
                            forSeason = currentProfile.season,
                            category = selectedCategory
                        )
                    }
                } else {
                    allCuratedPaletteItems.filter {
                        it.forSeason.equals(currentProfile?.season, ignoreCase = true) && it.category == selectedCategory
                    }.ifEmpty {
                        allCuratedPaletteItems.filter { it.category == selectedCategory }
                    }.ifEmpty {
                        allCuratedPaletteItems.take(6)
                    }
                }

                // 2-COLUMN AESTHETIC LOOKBOOK GRID
                val chunkedPairs = matchingItems.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    chunkedPairs.forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), RoundedCornerShape(20.dp)),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        // TOP SWATCH CARD WITH MATCH BADGE
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(95.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(item.hex.asComposeColor())
                                                .clickable {
                                                    SoundEffectManager.playTap(currentView)
                                                    revealedHexItemId = if (isHexRevealed) null else item.id
                                                }
                                                .padding(8.dp),
                                        ) {
                                            // MATCH SCORE BADGE
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.70f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                            ) {
                                                Text(
                                                    "${eval.scorePercent}% Match",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }

                                            // REVEALED HEX PILL
                                            if (isHexRevealed) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color.Black.copy(alpha = 0.75f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                                ) {
                                                    Text(
                                                        item.hex.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        // TITLE & FABRIC
                                        Text(
                                            item.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "${fabric.icon} ${fabric.name}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        // ACTION BUTTONS: [ Drape ] [ Try On ]
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        SoundEffectManager.playTap(currentView)
                                                        onNavigateToDrape(item.fabricId, item.hex)
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "Drape",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(EditorialSienna)
                                                    .clickable {
                                                        SoundEffectManager.playTap(currentView)
                                                        onNavigateToTryOn(item.fabricId, item.hex)
                                                    }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    "Try On",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))
        }

        // SETTINGS MODAL
        if (isSettingsOpen) {
            ProfileSettingsModal(
                onDismiss = { isSettingsOpen = false },
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
