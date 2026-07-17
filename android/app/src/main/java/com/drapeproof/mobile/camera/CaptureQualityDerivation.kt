package com.drapeproof.mobile.camera

import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Estimates room-light stability only from the two no-fabric baselines. Fabric-phase face
 * luminance is intentionally excluded because a real drape-induced change is the signal.
 */
internal fun baselineLuminanceCoefficientOfVariation(
    opening: List<Double>,
    closing: List<Double>,
): Double? {
    val values = (opening + closing).filter { it.isFinite() && it > 0.0 }
    if (values.isEmpty()) return null
    val mean = values.average()
    if (!mean.isFinite() || mean <= 0.0) return null
    return sqrt(values.sumOf { (it - mean).pow(2) } / values.size) / mean
}

/**
 * Combines only the sampled evidence ROIs: cheek anchors in every phase and fabric pixels in
 * the drape phase. Background, hair, windows, and other unused frame pixels cannot fail this gate.
 */
internal fun measurementRoiClippedFraction(
    openingCheekFractions: List<Double>,
    fabricCheekFractions: List<Double>,
    closingCheekFractions: List<Double>,
    fabricFractions: List<Double>,
): Double {
    val summaries = listOfNotNull(
        openingCheekFractions.validMedianOrNull(),
        fabricCheekFractions.validMedianOrNull(),
        closingCheekFractions.validMedianOrNull(),
        fabricFractions.validMedianOrNull(),
    )
    return summaries.maxOrNull() ?: 1.0
}

/** Robustly checks every phase without letting two clean phases hide one off-angle phase. */
internal fun worstAbsolutePhaseMedian(vararg phases: List<Double>): Double =
    phases.mapNotNull { it.validMedianOrNull(allowSigned = true) }
        .maxOfOrNull { kotlin.math.abs(it) } ?: 180.0

/** Uses the opening no-fabric scale as the anchor and checks both later phases. */
internal fun maximumPhaseScaleChangeFraction(
    opening: List<Double>,
    fabric: List<Double>,
    closing: List<Double>,
): Double {
    val openingMedian = opening.validMedianOrNull() ?: return 1.0
    if (openingMedian <= 0.0) return 1.0
    val laterMedians = listOfNotNull(fabric.validMedianOrNull(), closing.validMedianOrNull())
    if (laterMedians.size != 2) return 1.0
    return laterMedians.maxOf { kotlin.math.abs(it - openingMedian) / openingMedian }
}

private fun List<Double>.validMedianOrNull(allowSigned: Boolean = false): Double? {
    val values = filter {
        it.isFinite() && (allowSigned || it >= 0.0)
    }.sorted()
    if (values.isEmpty()) return null
    val middle = values.size / 2
    return if (values.size % 2 == 1) values[middle] else (values[middle - 1] + values[middle]) / 2.0
}
