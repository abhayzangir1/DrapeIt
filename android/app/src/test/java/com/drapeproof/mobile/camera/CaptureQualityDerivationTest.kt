package com.drapeproof.mobile.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureQualityDerivationTest {
    @Test
    fun `fabric induced face change is excluded from ambient drift`() {
        val result = baselineLuminanceCoefficientOfVariation(
            opening = listOf(0.50, 0.50),
            closing = listOf(0.50, 0.50),
        )

        assertEquals(0.0, result!!, 1e-12)
    }

    @Test
    fun `opening closing light drift is detected`() {
        val result = baselineLuminanceCoefficientOfVariation(
            opening = listOf(0.50),
            closing = listOf(0.55),
        )

        assertEquals(0.047619, result!!, 1e-6)
    }

    @Test
    fun `invalid luminance has no fabricated stability reading`() {
        assertNull(baselineLuminanceCoefficientOfVariation(listOf(0.0), listOf(Double.NaN)))
    }

    @Test
    fun `background clipping is absent from evidence roi calculation`() {
        val result = measurementRoiClippedFraction(
            openingCheekFractions = listOf(0.0),
            fabricCheekFractions = listOf(0.0),
            closingCheekFractions = listOf(0.0),
            fabricFractions = listOf(0.0, 0.0, 0.0),
        )

        assertEquals(0.0, result, 0.0)
    }

    @Test
    fun `clipped cheek or fabric controls the combined value`() {
        assertEquals(
            0.02,
            measurementRoiClippedFraction(listOf(0.0), listOf(0.02), listOf(0.0), listOf(0.0)),
            0.0,
        )
        assertEquals(
            0.03,
            measurementRoiClippedFraction(listOf(0.0), listOf(0.0), listOf(0.0), listOf(0.03)),
            0.0,
        )
    }

    @Test
    fun `clipping boundary is preserved for the core gate`() {
        assertEquals(
            0.0099,
            measurementRoiClippedFraction(listOf(0.0099), listOf(0.0), listOf(0.0), listOf(0.0)),
            0.0,
        )
        assertEquals(
            0.0100,
            measurementRoiClippedFraction(listOf(0.0100), listOf(0.0), listOf(0.0), listOf(0.0)),
            0.0,
        )
    }

    @Test
    fun `one clipped phase cannot be hidden by two clean phases`() {
        val result = measurementRoiClippedFraction(
            openingCheekFractions = List(18) { 0.0 },
            fabricCheekFractions = List(18) { 0.02 },
            closingCheekFractions = List(18) { 0.0 },
            fabricFractions = List(18) { 0.0 },
        )

        assertEquals(0.02, result, 0.0)
    }

    @Test
    fun `off-angle baseline cannot be hidden by centered drape`() {
        assertEquals(
            20.0,
            worstAbsolutePhaseMedian(listOf(20.0), listOf(0.0), listOf(-20.0)),
            0.0,
        )
    }

    @Test
    fun `scale drift checks closing as well as drape`() {
        assertEquals(
            0.05,
            maximumPhaseScaleChangeFraction(listOf(0.40), listOf(0.40), listOf(0.42)),
            1e-12,
        )
    }
}
