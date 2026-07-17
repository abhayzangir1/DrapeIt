package com.drapeproof.core.ranking

import com.drapeproof.core.domain.EvidenceTier
import kotlin.math.abs

enum class ContrastIntent(val targetPercentile: Double) {
    SOFT(0.20),
    BALANCED(0.50),
    BOLD(0.80),
}

data class VariantContrastCandidate(
    val sku: String,
    val variantId: String,
    val separationDeltaE00: Double,
    val uncertaintyDeltaE00: Double = 0.0,
    val evidenceTier: EvidenceTier,
    val eligible: Boolean = true,
) {
    init {
        require(sku.isNotBlank() && variantId.isNotBlank()) { "SKU and variant ID are required" }
        require(separationDeltaE00.isFinite() && separationDeltaE00 >= 0.0) {
            "Separation must be a finite non-negative value"
        }
        require(uncertaintyDeltaE00.isFinite() && uncertaintyDeltaE00 >= 0.0) {
            "Uncertainty must be a finite non-negative value"
        }
    }
}

data class RankedVariant(
    val candidate: VariantContrastCandidate,
    val separationPercentile: Double,
    val distanceFromIntentTarget: Double,
    val rank: Int,
)

enum class RecommendationStatus {
    READY,
    INSUFFICIENT_ELIGIBLE_VARIANTS,
}

data class IntentRankingResult(
    val intent: ContrastIntent,
    val status: RecommendationStatus,
    val eligibleVariantCount: Int,
    val rankedVariants: List<RankedVariant>,
) {
    val topVariant: RankedVariant? get() = rankedVariants.firstOrNull()
}

object IntentRanker {
    const val MINIMUM_ELIGIBLE_VARIANTS = 3

    /** Ranks variants of one SKU by their percentile distance from the selected user intent. */
    fun rank(
        candidates: List<VariantContrastCandidate>,
        intent: ContrastIntent,
    ): IntentRankingResult {
        val eligible = candidates.filter(VariantContrastCandidate::eligible)
        require(eligible.map(VariantContrastCandidate::sku).distinct().size <= 1) {
            "Intent ranking only compares color variants of the same SKU"
        }
        if (eligible.size < MINIMUM_ELIGIBLE_VARIANTS) {
            return IntentRankingResult(
                intent = intent,
                status = RecommendationStatus.INSUFFICIENT_ELIGIBLE_VARIANTS,
                eligibleVariantCount = eligible.size,
                rankedVariants = emptyList(),
            )
        }

        val percentiles = midrankPercentiles(eligible)
        val ordered = eligible.sortedWith(
            compareBy<VariantContrastCandidate> {
                abs(percentiles.getValue(it.variantId) - intent.targetPercentile)
            }.thenBy(VariantContrastCandidate::uncertaintyDeltaE00)
                .thenBy(VariantContrastCandidate::variantId),
        )
        return IntentRankingResult(
            intent = intent,
            status = RecommendationStatus.READY,
            eligibleVariantCount = eligible.size,
            rankedVariants = ordered.mapIndexed { index, candidate ->
                val percentile = percentiles.getValue(candidate.variantId)
                RankedVariant(
                    candidate = candidate,
                    separationPercentile = percentile,
                    distanceFromIntentTarget = abs(percentile - intent.targetPercentile),
                    rank = index + 1,
                )
            },
        )
    }

    private fun midrankPercentiles(candidates: List<VariantContrastCandidate>): Map<String, Double> {
        val sorted = candidates.sortedWith(
            compareBy<VariantContrastCandidate>(VariantContrastCandidate::separationDeltaE00)
                .thenBy(VariantContrastCandidate::variantId),
        )
        val result = mutableMapOf<String, Double>()
        var start = 0
        while (start < sorted.size) {
            var end = start
            while (
                end + 1 < sorted.size &&
                sorted[end + 1].separationDeltaE00 == sorted[start].separationDeltaE00
            ) {
                end++
            }
            val percentile = ((start + end) / 2.0) / sorted.lastIndex
            for (index in start..end) result[sorted[index].variantId] = percentile
            start = end + 1
        }
        return result
    }
}
