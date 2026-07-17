package com.drapeproof.core.domain

import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.color.LabColor
import com.drapeproof.core.statistics.RobustStatistics

enum class LightnessDirection {
    FABRIC_LIGHTER,
    FABRIC_DARKER,
    SIMILAR_LIGHTNESS,
}

enum class FacialFeature {
    EYE,
    EYEBROW,
    LIP,
}

data class FaceColorObservation(
    val skin: LabColor,
    val eye: LabColor? = null,
    val eyebrow: LabColor? = null,
    val lip: LabColor? = null,
    val cheek: LabColor? = null,
    val underChin: LabColor? = null,
)

data class ClothSkinSeparation(
    val deltaE00: Double,
    /** Fabric L* minus visible skin L*. */
    val deltaLStar: Double,
    val lightnessDirection: LightnessDirection,
)

data class FeatureContrastComponent(
    val feature: FacialFeature,
    val deltaE00AgainstSkin: Double,
)

data class FeatureDefinition(
    val currentMedianDeltaE00: Double?,
    val changeFromBaselineDeltaE00: Double?,
    val components: List<FeatureContrastComponent>,
)

data class ApparentFaceShift(
    val measured: Boolean,
    val aggregateDeltaE00: Double? = null,
    val cheekDeltaE00: Double? = null,
    val underChinDeltaE00: Double? = null,
    val deltaLStar: Double? = null,
    val unavailableReason: String? = null,
)

data class ContrastVector(
    val clothSkinSeparation: ClothSkinSeparation,
    val featureDefinition: FeatureDefinition,
    val apparentFaceShift: ApparentFaceShift,
)

object ContrastCalculator {
    private const val SIMILAR_LIGHTNESS_DELTA = 1.0

    /**
     * Computes the three signals independently. [allowApparentFaceShift] must only be true when
     * the paired-capture and closing-baseline gates passed.
     */
    fun calculate(
        baseline: FaceColorObservation?,
        drape: FaceColorObservation,
        fabric: LabColor,
        allowApparentFaceShift: Boolean,
    ): ContrastVector {
        val deltaL = fabric.l - drape.skin.l
        val separation = ClothSkinSeparation(
            deltaE00 = ColorDifference.ciede2000(drape.skin, fabric),
            deltaLStar = deltaL,
            lightnessDirection = when {
                deltaL > SIMILAR_LIGHTNESS_DELTA -> LightnessDirection.FABRIC_LIGHTER
                deltaL < -SIMILAR_LIGHTNESS_DELTA -> LightnessDirection.FABRIC_DARKER
                else -> LightnessDirection.SIMILAR_LIGHTNESS
            },
        )

        val currentComponents = featureComponents(drape)
        val currentMedian = currentComponents.takeIf(List<*>::isNotEmpty)?.let {
            RobustStatistics.median(it.map(FeatureContrastComponent::deltaE00AgainstSkin))
        }
        val baselineMedian = baseline?.let(::featureComponents)?.takeIf(List<*>::isNotEmpty)?.let {
            RobustStatistics.median(it.map(FeatureContrastComponent::deltaE00AgainstSkin))
        }
        val featureDefinition = FeatureDefinition(
            currentMedianDeltaE00 = currentMedian,
            changeFromBaselineDeltaE00 = if (currentMedian != null && baselineMedian != null) {
                currentMedian - baselineMedian
            } else {
                null
            },
            components = currentComponents,
        )

        return ContrastVector(
            clothSkinSeparation = separation,
            featureDefinition = featureDefinition,
            apparentFaceShift = faceShift(baseline, drape, allowApparentFaceShift),
        )
    }

    private fun featureComponents(observation: FaceColorObservation): List<FeatureContrastComponent> = buildList {
        observation.eye?.let {
            add(FeatureContrastComponent(FacialFeature.EYE, ColorDifference.ciede2000(observation.skin, it)))
        }
        observation.eyebrow?.let {
            add(FeatureContrastComponent(FacialFeature.EYEBROW, ColorDifference.ciede2000(observation.skin, it)))
        }
        observation.lip?.let {
            add(FeatureContrastComponent(FacialFeature.LIP, ColorDifference.ciede2000(observation.skin, it)))
        }
    }

    private fun faceShift(
        baseline: FaceColorObservation?,
        drape: FaceColorObservation,
        allowed: Boolean,
    ): ApparentFaceShift {
        if (!allowed) {
            return ApparentFaceShift(
                measured = false,
                unavailableReason = "Paired-capture quality gates did not pass",
            )
        }
        if (baseline == null) {
            return ApparentFaceShift(measured = false, unavailableReason = "A baseline observation is required")
        }
        val cheek = pairedDelta(baseline.cheek, drape.cheek)
        val underChin = pairedDelta(baseline.underChin, drape.underChin)
        val available = listOfNotNull(cheek, underChin)
        if (available.isEmpty()) {
            return ApparentFaceShift(measured = false, unavailableReason = "Cheek or under-chin ROIs are required")
        }
        return ApparentFaceShift(
            measured = true,
            aggregateDeltaE00 = RobustStatistics.median(available),
            cheekDeltaE00 = cheek,
            underChinDeltaE00 = underChin,
            deltaLStar = drape.skin.l - baseline.skin.l,
        )
    }

    private fun pairedDelta(first: LabColor?, second: LabColor?): Double? =
        if (first != null && second != null) ColorDifference.ciede2000(first, second) else null
}
