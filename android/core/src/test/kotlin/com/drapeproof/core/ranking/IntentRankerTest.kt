package com.drapeproof.core.ranking

import com.drapeproof.core.domain.EvidenceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentRankerTest {
    private val candidates = listOf(
        candidate("a", 10.0),
        candidate("b", 20.0),
        candidate("c", 30.0),
        candidate("d", 40.0),
        candidate("e", 50.0),
        candidate("f", 60.0),
    )

    @Test
    fun `soft balanced and bold choose low middle and high separation`() {
        assertEquals("b", IntentRanker.rank(candidates, ContrastIntent.SOFT).topVariant?.candidate?.variantId)
        assertEquals("c", IntentRanker.rank(candidates, ContrastIntent.BALANCED).topVariant?.candidate?.variantId)
        assertEquals("e", IntentRanker.rank(candidates, ContrastIntent.BOLD).topVariant?.candidate?.variantId)
    }

    @Test
    fun `rank output is deterministic and distances are nondecreasing`() {
        val result = IntentRanker.rank(candidates.shuffled(), ContrastIntent.BALANCED)
        assertEquals(RecommendationStatus.READY, result.status)
        assertEquals((1..6).toList(), result.rankedVariants.map { it.rank })
        assertEquals(
            result.rankedVariants.map { it.distanceFromIntentTarget }.sorted(),
            result.rankedVariants.map { it.distanceFromIntentTarget },
        )
    }

    @Test
    fun `ties share midrank percentile and lower uncertainty wins`() {
        val tied = listOf(
            candidate("low", 10.0),
            candidate("first", 20.0, uncertainty = 0.7),
            candidate("second", 20.0, uncertainty = 0.2),
            candidate("high", 40.0),
        )
        val result = IntentRanker.rank(tied, ContrastIntent.BALANCED)
        val tiedRankings = result.rankedVariants.filter { it.candidate.variantId in setOf("first", "second") }
        assertEquals(tiedRankings[0].separationPercentile, tiedRankings[1].separationPercentile, 0.0)
        assertEquals("second", result.topVariant?.candidate?.variantId)
    }

    @Test
    fun `fewer than three eligible variants yields explanation state not recommendation`() {
        val result = IntentRanker.rank(
            listOf(candidate("a", 10.0), candidate("b", 20.0), candidate("c", 30.0, eligible = false)),
            ContrastIntent.SOFT,
        )
        assertEquals(RecommendationStatus.INSUFFICIENT_ELIGIBLE_VARIANTS, result.status)
        assertEquals(2, result.eligibleVariantCount)
        assertTrue(result.rankedVariants.isEmpty())
    }

    @Test
    fun `mixing SKUs is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            IntentRanker.rank(
                listOf(candidate("a", 1.0), candidate("b", 2.0), candidate("c", 3.0).copy(sku = "other")),
                ContrastIntent.SOFT,
            )
        }
    }

    private fun candidate(
        id: String,
        separation: Double,
        uncertainty: Double = 0.0,
        eligible: Boolean = true,
    ) = VariantContrastCandidate(
        sku = "scarf-01",
        variantId = id,
        separationDeltaE00 = separation,
        uncertaintyDeltaE00 = uncertainty,
        evidenceTier = EvidenceTier.CONTROLLED_PAIR,
        eligible = eligible,
    )
}
