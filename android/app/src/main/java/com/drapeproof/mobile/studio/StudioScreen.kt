package com.drapeproof.mobile.studio

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.color.TrueColorHarmonyEngine
import com.drapeproof.mobile.avatar.AvatarLighting
import com.drapeproof.mobile.avatar.PhotoAvatarStore
import com.drapeproof.mobile.avatar.SavedAvatar
import com.drapeproof.mobile.data.SavedSuitedColor
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.data.SuitedColorsRepository
import com.drapeproof.mobile.fabric.FabricCatalog
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.silhouette.BodyShape
import com.drapeproof.mobile.silhouette.CutRecommendation
import com.drapeproof.mobile.silhouette.SilhouetteEngine
import com.drapeproof.mobile.silhouette.UserBodyProfile
import com.drapeproof.mobile.silhouette.UserProfileStore
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.GoldAccent
import com.drapeproof.mobile.ui.theme.Moss
import java.io.File
import kotlin.math.abs

private data class ColorSwatchItem(val name: String, val hex: String, val category: String)

private val categorizedColors = listOf(
    // Neutrals
    ColorSwatchItem("Warm Ivory", "#F5EFEB", "Neutrals"),
    ColorSwatchItem("Soft Sand", "#C8A47A", "Neutrals"),
    ColorSwatchItem("Ecru Beige", "#E3DAC9", "Neutrals"),
    ColorSwatchItem("Charcoal", "#36383A", "Neutrals"),
    ColorSwatchItem("Pure White", "#FFFFFF", "Neutrals"),
    ColorSwatchItem("Midnight Black", "#111827", "Neutrals"),

    // Earth Tones
    ColorSwatchItem("Terracotta", "#B85F45", "Earth Tones"),
    ColorSwatchItem("Quiet Sage", "#71856E", "Earth Tones"),
    ColorSwatchItem("Warm Ochre", "#C68B45", "Earth Tones"),
    ColorSwatchItem("Olive Green", "#556B2F", "Earth Tones"),
    ColorSwatchItem("Rust Copper", "#8B3A1C", "Earth Tones"),

    // Vibrant & Rich
    ColorSwatchItem("Deep Cobalt", "#2F51A2", "Vibrant & Rich"),
    ColorSwatchItem("Mulberry Plum", "#74445F", "Vibrant & Rich"),
    ColorSwatchItem("Emerald Green", "#1B5E20", "Vibrant & Rich"),
    ColorSwatchItem("Royal Navy", "#1E3A8A", "Vibrant & Rich"),
    ColorSwatchItem("Coral Rose", "#FF5A5F", "Vibrant & Rich"),

    // Pastels
    ColorSwatchItem("Dusty Rose", "#DDA7A5", "Pastels"),
    ColorSwatchItem("Sky Blue", "#A7C7E7", "Pastels"),
    ColorSwatchItem("Buttercream", "#FFFDD0", "Pastels"),
    ColorSwatchItem("Lavender Mist", "#E6E6FA", "Pastels"),
    ColorSwatchItem("Mint Green", "#C1E1C1", "Pastels"),
)

private enum class StudioStep(val title: String, val icon: String) {
    FABRIC("Fabric Material", "🌿"),
    COLOR("Colorway & Harmony", "🎨"),
    SILHOUETTE("Silhouette & Cut", "📐"),
}

@Composable
fun StudioScreen(
    onStartCameraScan: () -> Unit,
    onOpenPhotoAnalysis: () -> Unit,
    onNavigateToTryOn: (fabricId: String, colorHex: String, cutName: String) -> Unit,
    onNavigateToShop: (fabricName: String, colorName: String, colorHex: String, cutName: String) -> Unit,
) {
    val context = LocalContext.current
    var activeStep by remember { mutableStateOf(StudioStep.FABRIC) }

    // User Avatar & Real Sampled Skin
    var avatars by remember { mutableStateOf(PhotoAvatarStore.listAvatars(context)) }
    var activeAvatar by remember { mutableStateOf(PhotoAvatarStore.getActiveAvatar(context)) }
    val storedSkin = remember { SkinProfileRepository.load(context) }
    var effectiveSkinHex by remember { mutableStateOf(activeAvatar?.skinHex ?: storedSkin?.skinHex ?: "#D8B498") }

    // Fabric & Color Selection
    var selectedFabric by remember { mutableStateOf(FabricCatalog.defaultFabric) }
    var selectedColorCategory by remember { mutableStateOf("Neutrals") }
    var selectedColor by remember { mutableStateOf(categorizedColors.first()) }
    var customHex by remember { mutableStateOf<String?>(null) }
    var customHue by remember { mutableStateOf(24f) }

    // Body Silhouette
    var userProfile by remember { mutableStateOf(UserProfileStore.load(context)) }
    var selectedShape by remember { mutableStateOf(userProfile.bodyShape) }

    // Real Colorimetry Calculation
    val activeColorHex = customHex ?: selectedColor.hex
    val matchScore = remember(effectiveSkinHex, activeColorHex) {
        calculateMatchScore(effectiveSkinHex, activeColorHex)
    }
    val recommendation = remember(selectedShape, userProfile.heightCategory, selectedFabric) {
        SilhouetteEngine.recommend(selectedShape, userProfile.heightCategory, selectedFabric)
    }

    // Smooth Interpolations
    val animatedColor by animateColorAsState(
        targetValue = activeColorHex.asComposeColor(),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "ChestSwatchColor",
    )
    val animatedScore by animateIntAsState(
        targetValue = matchScore.scorePercent,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ScoreCounter",
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val saved = PhotoAvatarStore.saveAvatarFromUri(
                context = context,
                sourceUri = it,
                name = "Daylight Photo (${avatars.size + 1})",
                lighting = AvatarLighting.DAYLIGHT,
                skinHex = effectiveSkinHex,
            )
            if (saved != null) {
                avatars = PhotoAvatarStore.listAvatars(context)
                activeAvatar = saved
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
    ) {
        // Minimalist Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Color & Style Studio", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Real skin tone harmony & proportion styling",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
            }

            // Quick Camera / Photo Scan trigger
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onStartCameraScan,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                ) {
                    Text("📸 Scan Face", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // CENTERPIECE: The Clean Interactive Look Canvas
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Top HUD Bar: Real Skin Sample + Match Score Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Skin Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(effectiveSkinHex.asComposeColor())
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Skin: $effectiveSkinHex",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Real-Time Match Score Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (animatedScore >= 90) Moss.copy(alpha = 0.14f) else DrapeCoral.copy(alpha = 0.14f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "$animatedScore% Match • ${matchScore.harmonyLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (animatedScore >= 90) Moss else DrapeCoral,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Avatar / Live Chest Drape Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Chest Swatch / Drape Visualizer
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(animatedColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(selectedFabric.icon, fontSize = 28.sp)
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "${selectedFabric.name} in ${selectedColor.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Cut: ${recommendation.recommendedTop.displayName} • ${recommendation.recommendedBottom.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(6.dp))
                        // 1-Tap Save Star Button
                        val isFav = SuitedColorsRepository.isFavorite(context, activeColorHex, selectedFabric.id)
                        Text(
                            if (isFav) "★ Saved to Suited Palette" else "+ Tap to Save This Match",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFav) GoldAccent else DrapeCoral,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                if (isFav) {
                                    val found = SuitedColorsRepository.list(context).firstOrNull { it.colorHex.equals(activeColorHex, true) }
                                    found?.let { SuitedColorsRepository.remove(context, it.id) }
                                } else {
                                    SuitedColorsRepository.add(
                                        context,
                                        SavedSuitedColor(
                                            colorHex = activeColorHex,
                                            colorName = selectedColor.name,
                                            fabricId = selectedFabric.id,
                                            fabricName = selectedFabric.name,
                                            matchScorePercent = matchScore.scorePercent,
                                            contrastLabel = matchScore.harmonyLabel,
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Step Navigation Pills (Single Clean Segmented Control)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StudioStep.values().forEach { step ->
                val isSelected = activeStep == step
                val scale by animateFloatAsState(if (isSelected) 1.03f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                FilterChip(
                    selected = isSelected,
                    onClick = { activeStep = step },
                    label = { Text("${step.icon} ${step.title}") },
                    modifier = Modifier
                        .weight(1f)
                        .scale(scale),
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // FOCUSED CONTROL DRAWER (Changes dynamically based on activeStep)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                when (activeStep) {
                    StudioStep.FABRIC -> {
                        Text("SELECT FABRIC MATERIAL", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${selectedFabric.name}: ${selectedFabric.description}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(12.dp))

                        // Horizontal Fabrics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FabricCatalog.allFabrics.forEach { fab ->
                                val isSelected = selectedFabric.id == fab.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFabric = fab },
                                    label = { Text("${fab.icon} ${fab.name}") },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DrapeCoral.copy(alpha = 0.15f),
                                        selectedLabelColor = DrapeCoral,
                                    ),
                                )
                            }
                        }
                    }

                    StudioStep.COLOR -> {
                        Text("SELECT COLOR PALETTE", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                        Spacer(Modifier.height(10.dp))

                        // Category Pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("Neutrals", "Earth Tones", "Vibrant & Rich", "Pastels", "Custom").forEach { cat ->
                                FilterChip(
                                    selected = selectedColorCategory == cat,
                                    onClick = { selectedColorCategory = cat },
                                    label = { Text(cat) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (selectedColorCategory == "Custom") {
                            Text("Custom Hue Slider: ${customHue.toInt()}°", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = customHue,
                                onValueChange = {
                                    customHue = it
                                    val rgb = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.65f, 0.85f))
                                    customHex = String.format("#%06X", 0xFFFFFF and rgb)
                                    selectedColor = ColorSwatchItem("Custom Shade", customHex!!, "Custom")
                                },
                                valueRange = 0f..360f,
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                categorizedColors.filter { it.category == selectedColorCategory }.forEach { item ->
                                    val isSelected = activeColorHex.equals(item.hex, ignoreCase = true)
                                    val swatchScale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                selectedColor = item
                                                customHex = null
                                            }
                                            .padding(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .scale(swatchScale)
                                                .clip(CircleShape)
                                                .background(item.hex.asComposeColor())
                                                .border(
                                                    if (isSelected) 3.dp else 1.dp,
                                                    if (isSelected) DrapeCoral else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                    CircleShape,
                                                ),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(item.name, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }

                    StudioStep.SILHOUETTE -> {
                        Text("BODY SHAPE & CUT PROPORTIONS", style = MaterialTheme.typography.labelSmall, color = DrapeCoral)
                        Spacer(Modifier.height(8.dp))

                        // Silhouette Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            BodyShape.values().forEach { sh ->
                                FilterChip(
                                    selected = selectedShape == sh,
                                    onClick = { selectedShape = sh },
                                    label = { Text("${sh.icon} ${sh.displayName}") },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(12.dp),
                        ) {
                            Text(
                                "Topwear Fit: ${recommendation.recommendedTop.displayName} (${recommendation.recommendedTop.description})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Bottomwear Pairing: ${recommendation.recommendedBottom.displayName} — ${recommendation.bottomColorAdvice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // PRIMARY ACTION BUTTONS
        Button(
            onClick = {
                onNavigateToTryOn(
                    selectedFabric.id,
                    activeColorHex,
                    recommendation.recommendedTop.displayName,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
        ) {
            Text(
                "✨ AI Virtual Try-On This Look  →",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                onNavigateToShop(
                    selectedFabric.name,
                    selectedColor.name,
                    activeColorHex,
                    recommendation.recommendedTop.displayName,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("🛒 Find to Buy in Stores")
        }

        Spacer(Modifier.height(28.dp))
    }
}

private fun calculateMatchScore(skinHex: String, fabricHex: String): com.drapeproof.core.color.HarmonyAnalysisResult {
    return TrueColorHarmonyEngine.evaluate(skinHex, fabricHex)
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
