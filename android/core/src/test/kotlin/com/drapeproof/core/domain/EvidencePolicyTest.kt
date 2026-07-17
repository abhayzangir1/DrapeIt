package com.drapeproof.core.domain

import com.drapeproof.core.capture.QualityGateResult
import org.junit.Assert.assertEquals
import org.junit.Test

class EvidencePolicyTest {
    private val passingQuality = QualityGateResult(passed = true, failures = emptyList(), warnings = emptyList())
    private val failingQuality = QualityGateResult(passed = false, failures = emptyList(), warnings = emptyList())

    @Test
    fun `only a passing locked pair can become controlled evidence`() {
        val controlled = EvidenceInputs(true, true, true, true)
        assertEquals(EvidenceTier.CONTROLLED_PAIR, EvidencePolicy.highestSupported(controlled, passingQuality))
        assertEquals(EvidenceTier.SAME_SCENE, EvidencePolicy.highestSupported(controlled, failingQuality))
        assertEquals(EvidenceTier.SAME_SCENE, EvidencePolicy.highestSupported(controlled, null))
    }

    @Test
    fun `separate images can never be elevated above estimate`() {
        val separate = EvidenceInputs(
            hasOpeningAndClosingBaseline = true,
            faceAndFabricInSameScene = false,
            hasSeparateFaceAndProductPhotos = true,
            inputEligibleForMeasurement = true,
        )
        assertEquals(EvidenceTier.SEPARATE_PHOTO_ESTIMATE, EvidencePolicy.highestSupported(separate, passingQuality))
    }

    @Test
    fun `ineligible material is preview only regardless of capture data`() {
        val ineligible = EvidenceInputs(true, true, true, inputEligibleForMeasurement = false)
        assertEquals(EvidenceTier.PREVIEW_ONLY, EvidencePolicy.highestSupported(ineligible, passingQuality))
    }
}
