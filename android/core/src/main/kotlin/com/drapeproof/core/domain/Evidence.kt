package com.drapeproof.core.domain

import com.drapeproof.core.capture.QualityGateResult

enum class EvidenceTier {
    CONTROLLED_PAIR,
    SAME_SCENE,
    SEPARATE_PHOTO_ESTIMATE,
    PREVIEW_ONLY,
}

enum class ProductProvenance {
    PHYSICAL_REFERENCE,
    EXACT_CATALOG_VARIANT,
    UNVERIFIED_IMAGE,
}

data class EvidenceInputs(
    val hasOpeningAndClosingBaseline: Boolean,
    val faceAndFabricInSameScene: Boolean,
    val hasSeparateFaceAndProductPhotos: Boolean,
    val inputEligibleForMeasurement: Boolean,
)

/** Prevents a lower-evidence input path from ever being promoted by UI code. */
object EvidencePolicy {
    fun highestSupported(inputs: EvidenceInputs, quality: QualityGateResult?): EvidenceTier = when {
        inputs.inputEligibleForMeasurement &&
            inputs.hasOpeningAndClosingBaseline &&
            inputs.faceAndFabricInSameScene &&
            quality?.passed == true -> EvidenceTier.CONTROLLED_PAIR

        inputs.inputEligibleForMeasurement && inputs.faceAndFabricInSameScene -> EvidenceTier.SAME_SCENE
        inputs.inputEligibleForMeasurement && inputs.hasSeparateFaceAndProductPhotos ->
            EvidenceTier.SEPARATE_PHOTO_ESTIMATE

        else -> EvidenceTier.PREVIEW_ONLY
    }
}
