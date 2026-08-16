package com.drapeproof.mobile.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.ranking.ContrastIntent
import com.drapeproof.core.ranking.IntentRanker
import com.drapeproof.core.ranking.IntentRankingResult
import com.drapeproof.core.ranking.VariantContrastCandidate
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.data.LocalDrapeRecord
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.data.displayName
import com.drapeproof.mobile.network.DrapeProofApiClient
import com.drapeproof.mobile.network.HealthStatus
import com.drapeproof.mobile.ui.ScreenHeader
import com.drapeproof.mobile.ui.theme.DrapeCoral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private data class MatteVariant(val id: String, val name: String, val hex: String)

private val catalogVariants = listOf(
    MatteVariant("warm-sand", "Warm Sand", "#C8A47A"),
    MatteVariant("terracotta", "Terracotta", "#B85F45"),
    MatteVariant("sage", "Quiet Sage", "#71856E"),
    MatteVariant("cobalt", "Deep Cobalt", "#2F51A2"),
    MatteVariant("plum", "Mulberry Plum", "#74445F"),
    MatteVariant("charcoal", "Soft Charcoal", "#36383A"),
)

private const val CATALOG_SKU = "DP-FABRIC-01"

private sealed interface VtoAvailability {
    data object Idle : VtoAvailability
    data object Checking : VtoAvailability
    data class Available(val status: HealthStatus) : VtoAvailability
    data class Unavailable(val reason: String) : VtoAvailability
}

@Composable
fun CatalogScreen(onBack: () -> Unit, onOpenRecords: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { SkinProfileRepository.load(context) }
    var intent by remember { mutableStateOf(ContrastIntent.BALANCED) }
    var savedVariantId by remember { mutableStateOf<String?>(null) }
    var vtoAvailability by remember { mutableStateOf<VtoAvailability>(VtoAvailability.Idle) }

    val ranking = remember(profile, intent) { profile?.let { rankVariants(it, intent) } }
    val top = ranking?.topVariant?.candidate?.variantId?.let { id -> catalogVariants.first { it.id == id } }
    LaunchedEffect(intent) { savedVariantId = null }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Exact-color catalog",
            evidence = profile?.evidenceTier?.displayName()?.uppercase(),
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text("Choose the contrast you want.", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(7.dp))
            Text(
                "Six solid-matte colorways of one demo SKU are ranked against your latest cheek sample. This is a relative color decision, not a beauty score.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                IntentChip(ContrastIntent.SOFT, intent) { intent = it }
                IntentChip(ContrastIntent.BALANCED, intent) { intent = it }
                IntentChip(ContrastIntent.BOLD, intent) { intent = it }
            }
            Spacer(Modifier.height(12.dp))
            IntentMeaning(intent)
            Spacer(Modifier.height(18.dp))

            if (profile == null) {
                MissingProfileCard()
            } else {
                ProfileEvidenceCard(profile)
                Spacer(Modifier.height(14.dp))
                if (top != null) {
                    TopChoiceCard(top, ranking, profile)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            saveCatalogRecord(context, profile, intent, ranking, top)
                            savedVariantId = top.id
                        },
                        enabled = savedVariantId != top.id,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(if (savedVariantId == top.id) "Recommendation saved" else "Save as Drape Record") }
                }
            }

            var showCompareDialog by remember { mutableStateOf(false) }

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ALL SIX COLORWAYS", style = MaterialTheme.typography.labelSmall)
                if (ranking != null) {
                    TextButton(onClick = { showCompareDialog = true }) {
                        Text("Compare Top 3 Side-by-Side  ⇄")
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            catalogVariants.forEach { variant ->
                val ranked = ranking?.rankedVariants?.firstOrNull { it.candidate.variantId == variant.id }
                VariantRow(variant, ranked?.rank, ranked?.separationPercentile, ranked?.candidate?.separationDeltaE00)
                Spacer(Modifier.height(8.dp))
            }

            if (showCompareDialog && ranking != null) {
                CompareVariantsDialog(
                    ranking = ranking,
                    variants = catalogVariants,
                    intent = intent,
                    onDismiss = { showCompareDialog = false },
                )
            }

            Spacer(Modifier.height(24.dp))
            VtoEntryCard(
                state = vtoAvailability,
                onCheck = {
                    vtoAvailability = VtoAvailability.Checking
                    scope.launch {
                        vtoAvailability = withContext(Dispatchers.IO) {
                            runCatching { DrapeProofApiClient().health() }.fold(
                                onSuccess = { status ->
                                    if (status.ready && status.vtoProviderConfigured) {
                                        VtoAvailability.Available(status)
                                    } else {
                                        VtoAvailability.Unavailable(
                                            "The secure Worker or its ${status.vtoProvider} provider is not configured yet.",
                                        )
                                    }
                                },
                                onFailure = { VtoAvailability.Unavailable("The secure VTO service could not be reached.") },
                            )
                        }
                    }
                },
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenRecords, modifier = Modifier.fillMaxWidth()) {
                Text("Open Drape Records  →")
            }
            Spacer(Modifier.height(38.dp))
        }
    }
}

@Composable
private fun IntentChip(intent: ContrastIntent, selected: ContrastIntent, onClick: (ContrastIntent) -> Unit) {
    FilterChip(
        selected = intent == selected,
        onClick = { onClick(intent) },
        label = { Text(intent.label()) },
    )
}

@Composable
private fun IntentMeaning(intent: ContrastIntent) {
    val detail = when (intent) {
        ContrastIntent.SOFT -> "Targets a lower-separation colorway within this exact SKU."
        ContrastIntent.BALANCED -> "Targets the middle of this SKU's measured separation range."
        ContrastIntent.BOLD -> "Targets a higher-separation colorway within this exact SKU."
    }
    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.66f))
}

@Composable
private fun MissingProfileCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("A face sample is required", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Go back and run a real-cloth scan or Photo contrast. DrapeIt will not invent a personalized rank without your own measured skin sample.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ProfileEvidenceCard(profile: StoredSkinProfile) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(profile.skinHex.asComposeColor(), CircleShape))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Latest cheek sample • ${profile.skinHex}", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${profile.evidenceTier.displayName()} from ${profile.source}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
            }
        }
    }
}

@Composable
private fun TopChoiceCard(variant: MatteVariant, ranking: IntentRankingResult, profile: StoredSkinProfile) {
    val ranked = ranking.topVariant!!
    val foreground = variant.hex.readableForeground()
    Card(
        colors = CardDefaults.cardColors(containerColor = variant.hex.asComposeColor()),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("${ranking.intent.label().uppercase()} PICK", style = MaterialTheme.typography.labelSmall, color = foreground.copy(alpha = 0.78f))
            Spacer(Modifier.height(20.dp))
            Text(variant.name, style = MaterialTheme.typography.headlineLarge, color = foreground, fontWeight = FontWeight.Bold)
            Text("$CATALOG_SKU • ${variant.hex}", style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = 0.82f))
            Spacer(Modifier.height(16.dp))
            Text(
                String.format(
                    Locale.US,
                    "%.1f ΔE00 • %dth separation percentile",
                    ranked.candidate.separationDeltaE00,
                    (ranked.separationPercentile * 100).roundToInt(),
                ),
                style = MaterialTheme.typography.titleSmall,
                color = foreground,
            )
            Text(
                "Percentile is only among these six variants and inherits ${profile.evidenceTier.displayName().lowercase()} uncertainty.",
                style = MaterialTheme.typography.bodySmall,
                color = foreground.copy(alpha = 0.78f),
            )
        }
    }
}

@Composable
private fun VariantRow(variant: MatteVariant, rank: Int?, percentile: Double?, deltaE: Double?) {
    Card(shape = RoundedCornerShape(17.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).background(variant.hex.asComposeColor(), RoundedCornerShape(13.dp)))
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(variant.name, style = MaterialTheme.typography.titleSmall)
                Text("${variant.hex} • $CATALOG_SKU", style = MaterialTheme.typography.bodySmall)
            }
            if (rank != null && percentile != null && deltaE != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("#${rank}", style = MaterialTheme.typography.titleMedium, color = DrapeCoral)
                    Text(
                        String.format(Locale.US, "%.1f ΔE • P%d", deltaE, (percentile * 100).roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else {
                Text("UNRANKED", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VtoEntryCard(state: VtoAvailability, onCheck: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("YOUCAM VIRTUAL TRY-ON", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(6.dp))
            Text("A visual preview, kept separate from evidence", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(
                "VTO can help visualize a colorway, but generated pixels never change the measured contrast rank. Starting a try-on requires a configured secure Worker and spends YouCam units.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            when (state) {
                VtoAvailability.Idle -> OutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) {
                    Text("Check secure VTO readiness")
                }
                VtoAvailability.Checking -> OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Text("Checking Worker…")
                }
                is VtoAvailability.Available -> {
                    Text(
                        "Ready • ${state.status.vtoProvider} provider • Worker ${state.status.version}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        "Use the dedicated person + garment flow only after confirming the unit cost and reserve.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is VtoAvailability.Unavailable -> {
                    Text(state.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(onClick = onCheck) { Text("Check again") }
                }
            }
        }
    }
}

private fun rankVariants(profile: StoredSkinProfile, intent: ContrastIntent): IntentRankingResult {
    val skin = ColorConversions.hexToLab(profile.skinHex)
    val uncertainty = when (profile.evidenceTier) {
        EvidenceTier.CONTROLLED_PAIR -> 0.5
        EvidenceTier.SAME_SCENE -> 1.5
        EvidenceTier.SEPARATE_PHOTO_ESTIMATE -> 4.0
        EvidenceTier.PREVIEW_ONLY -> 8.0
    }
    return IntentRanker.rank(
        candidates = catalogVariants.map { variant ->
            VariantContrastCandidate(
                sku = CATALOG_SKU,
                variantId = variant.id,
                separationDeltaE00 = ColorDifference.ciede2000(skin, ColorConversions.hexToLab(variant.hex)),
                uncertaintyDeltaE00 = uncertainty,
                evidenceTier = profile.evidenceTier,
                eligible = profile.evidenceTier != EvidenceTier.PREVIEW_ONLY,
            )
        },
        intent = intent,
    )
}

private fun saveCatalogRecord(
    context: android.content.Context,
    profile: StoredSkinProfile,
    intent: ContrastIntent,
    ranking: IntentRankingResult,
    variant: MatteVariant,
) {
    val ranked = ranking.rankedVariants.first { it.candidate.variantId == variant.id }
    val skinLab = ColorConversions.hexToLab(profile.skinHex)
    val fabricLab = ColorConversions.hexToLab(variant.hex)
    DrapeRecordRepository.add(
        context,
        LocalDrapeRecord.create(
            source = "exact-SKU catalog ranking from ${profile.source}",
            evidenceTier = profile.evidenceTier,
            intent = intent,
            sku = CATALOG_SKU,
            variantId = variant.id,
            variantName = variant.name,
            skinHex = profile.skinHex,
            fabricHex = variant.hex,
            separationDeltaE00 = ranked.candidate.separationDeltaE00,
            deltaLStar = fabricLab.l - skinLab.l,
            limitations = listOf(
                "Catalog hex is a screen specification, not a measured physical fabric swatch.",
                "Recommendation is a percentile within six solid-matte variants of $CATALOG_SKU only.",
                "Virtual try-on, if used, is preview evidence and never changes this rank.",
            ),
        ),
    )
}

@Composable
private fun CompareVariantsDialog(
    ranking: IntentRankingResult,
    variants: List<MatteVariant>,
    intent: ContrastIntent,
    onDismiss: () -> Unit,
) {
    val topThree = ranking.rankedVariants.take(3)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Side-by-Side Comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Target Intent: ${intent.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    topThree.forEach { ranked ->
                        val variant = variants.first { it.id == ranked.candidate.variantId }
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(variant.hex))),
                                )
                                Spacer(Modifier.height(6.dp))
                                Text("#${ranked.rank} ${variant.name}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("ΔE ${"%.1f".format(ranked.candidate.separationDeltaE00)}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "p${(ranked.separationPercentile * 100).toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "Comparison Insights:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                val first = topThree.firstOrNull()
                val second = topThree.getOrNull(1)
                if (first != null && second != null) {
                    val firstVariant = variants.first { it.id == first.candidate.variantId }
                    val secondVariant = variants.first { it.id == second.candidate.variantId }
                    val diff = kotlin.math.abs(first.candidate.separationDeltaE00 - second.candidate.separationDeltaE00)
                    Text(
                        "${firstVariant.name} separates by ${"%.1f".format(diff)} ΔE00 more than ${secondVariant.name}, matching your ${intent.label().lowercase()} preference.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Close Comparison") }
            }
        }
    }
}

private fun ContrastIntent.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)

private fun String.asComposeColor(): Color {
    val value = removePrefix("#").toLong(16)
    return Color(
        red = ((value shr 16) and 0xFF).toInt(),
        green = ((value shr 8) and 0xFF).toInt(),
        blue = (value and 0xFF).toInt(),
    )
}

private fun String.readableForeground(): Color {
    val value = removePrefix("#").toLong(16)
    fun channel(shift: Int): Double {
        val encoded = ((value shr shift) and 0xFF).toDouble() / 255.0
        return if (encoded <= 0.04045) encoded / 12.92 else Math.pow((encoded + 0.055) / 1.055, 2.4)
    }
    val luminance = 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    return if (luminance > 0.38) Color(0xFF181817) else Color.White
}
