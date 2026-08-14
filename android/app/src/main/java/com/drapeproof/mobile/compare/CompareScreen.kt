package com.drapeproof.mobile.compare

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
import androidx.compose.ui.geometry.Offset
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
import com.drapeproof.mobile.fabric.FabricMaterial
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import com.drapeproof.mobile.ui.theme.EditorialWarning

private data class ComparePaletteItem(val name: String, val hex: String)

private val compareColorPalette = listOf(
    ComparePaletteItem("Royal Burgundy", "#831843"),
    ComparePaletteItem("Deep Olive", "#3F6212"),
    ComparePaletteItem("Cobalt Navy", "#1D4ED8"),
    ComparePaletteItem("Terracotta", "#B45309"),
    ComparePaletteItem("Charcoal Slate", "#374151"),
    ComparePaletteItem("Emerald Pine", "#047857"),
    ComparePaletteItem("Warm Ivory", "#F7EFE8"),
    ComparePaletteItem("Dusty Rose", "#FDA4AF"),
)

@Composable
fun CompareScreen(
    onBack: () -> Unit,
    onSelectLookForTryOn: (fabricId: String, colorHex: String) -> Unit,
) {
    val context = LocalContext.current
    val storedProfile = remember { SkinProfileRepository.load(context) }
    val effectiveSkinHex = storedProfile?.skinHex ?: "#D8B498"

    var selectedColor by remember { mutableStateOf(compareColorPalette[0]) }

    val candidateFabrics = listOf(
        FabricCatalog.allFabrics.first { it.id == "silk" },
        FabricCatalog.allFabrics.first { it.id == "cotton" },
        FabricCatalog.allFabrics.first { it.id == "linen" },
        FabricCatalog.allFabrics.first { it.id == "velvet" },
    )

    val evaluatedCandidates = candidateFabrics.map { fabric ->
        val result = TrueColorHarmonyEngine.evaluate(effectiveSkinHex, selectedColor.hex)
        val modScore = when (fabric.id) {
            "silk" -> (result.scorePercent + 3).coerceAtMost(98)
            "velvet" -> (result.scorePercent + 1).coerceAtMost(97)
            "linen" -> (result.scorePercent - 2).coerceAtLeast(15)
            else -> result.scorePercent
        }
        val materialInsight = when (fabric.id) {
            "silk" -> "Specular sheen adds high-contrast definition around the complexion."
            "velvet" -> "Deep pile absorbs light, giving rich, saturated tone depth."
            "linen" -> "Organic slub ridges create natural, soft diffuse separation."
            else -> "Clean matte weave provides balanced, everyday contrast."
        }
        Pair(fabric, Pair(modScore, materialInsight))
    }

    val winnerCandidate = evaluatedCandidates.maxByOrNull { it.second.first } ?: evaluatedCandidates.first()

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
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Material Compare",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = EditorialInk,
                    )
                    Text(
                        "See how fabric texture & luster alter the look",
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

            Spacer(Modifier.height(18.dp))

            Text(
                "Select Target Colorway",
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                compareColorPalette.forEach { item ->
                    val isSelected = selectedColor.hex == item.hex
                    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedColor = item }
                            .padding(2.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(item.hex.asComposeColor())
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) EditorialSienna else EditorialStone.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                ),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            item.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) EditorialSienna else EditorialMuted,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // WINNER BANNER
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EditorialSand.copy(alpha = 0.70f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✨", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "${winnerCandidate.first.name} Wins (${winnerCandidate.second.first}%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EditorialInk,
                        )
                        Text(
                            winnerCandidate.second.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = EditorialMuted,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "Fabric Candidates",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = EditorialMuted,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                evaluatedCandidates.forEach { (fabric, eval) ->
                    val score = eval.first
                    val insight = eval.second
                    val isWinner = fabric.id == winnerCandidate.first.id

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isWinner) 4.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isWinner) 2.dp else 1.dp,
                                color = if (isWinner) EditorialSienna else EditorialStone.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(20.dp),
                            ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(fabric.icon, fontSize = 24.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            fabric.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = EditorialInk,
                                        )
                                        Text(
                                            fabric.weaveType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EditorialMuted,
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (score >= 86) EditorialPositive.copy(alpha = 0.15f)
                                            else EditorialWarning.copy(alpha = 0.15f),
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        "$score%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (score >= 86) EditorialPositive else EditorialWarning,
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width
                                    val h = size.height
                                    val baseColor = selectedColor.hex.asComposeColor()

                                    drawRect(color = baseColor)

                                    when (fabric.id) {
                                        "silk", "satin" -> {
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.38f),
                                                        Color.Transparent,
                                                        Color.White.copy(alpha = 0.25f),
                                                        Color.Black.copy(alpha = 0.30f),
                                                    ),
                                                ),
                                            )
                                        }
                                        "velvet" -> {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.White.copy(alpha = 0.22f),
                                                        Color.Black.copy(alpha = 0.40f),
                                                        Color.White.copy(alpha = 0.25f),
                                                    ),
                                                ),
                                            )
                                        }
                                        "linen" -> {
                                            var y = 0f
                                            while (y < h) {
                                                drawLine(
                                                    color = Color.Black.copy(alpha = 0.14f),
                                                    start = Offset(0f, y),
                                                    end = Offset(w, y),
                                                    strokeWidth = 1.5.dp.toPx(),
                                                )
                                                y += 14.dp.toPx()
                                            }
                                        }
                                        else -> {
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.15f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.20f),
                                                    ),
                                                ),
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Text(
                                insight,
                                style = MaterialTheme.typography.bodySmall,
                                color = EditorialMuted,
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        SuitedColorsRepository.add(
                                            context,
                                            SavedSuitedColor(
                                                colorHex = selectedColor.hex,
                                                colorName = selectedColor.name,
                                                fabricId = fabric.id,
                                                fabricName = fabric.name,
                                                matchScorePercent = score,
                                                contrastLabel = if (score >= 86) "Strong Compatibility" else "Good Compatibility",
                                            ),
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("★ Bookmark", color = EditorialInk, style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = { onSelectLookForTryOn(fabric.id, selectedColor.hex) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("📸 Try-On →", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
