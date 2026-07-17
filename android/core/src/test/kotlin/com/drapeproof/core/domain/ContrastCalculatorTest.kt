package com.drapeproof.core.domain

import com.drapeproof.core.color.LabColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastCalculatorTest {
    private val baseline = FaceColorObservation(
        skin = LabColor(60.0, 12.0, 18.0),
        eye = LabColor(30.0, 5.0, 5.0),
        eyebrow = LabColor(25.0, 8.0, 8.0),
        lip = LabColor(48.0, 30.0, 16.0),
        cheek = LabColor(61.0, 13.0, 19.0),
        underChin = LabColor(52.0, 11.0, 16.0),
    )
    private val drape = baseline.copy(
        skin = LabColor(61.0, 13.0, 18.0),
        eye = LabColor(28.0, 4.0, 4.0),
        cheek = LabColor(62.0, 14.0, 19.0),
        underChin = LabColor(54.0, 12.0, 17.0),
    )

    @Test
    fun `three signals remain independent and face shift is measured only when allowed`() {
        val vector = ContrastCalculator.calculate(
            baseline = baseline,
            drape = drape,
            fabric = LabColor(30.0, 40.0, -20.0),
            allowApparentFaceShift = true,
        )
        assertEquals(LightnessDirection.FABRIC_DARKER, vector.clothSkinSeparation.lightnessDirection)
        assertEquals(-31.0, vector.clothSkinSeparation.deltaLStar, 0.0)
        assertTrue(vector.clothSkinSeparation.deltaE00 > 0.0)
        assertEquals(3, vector.featureDefinition.components.size)
        assertNotNull(vector.featureDefinition.currentMedianDeltaE00)
        assertNotNull(vector.featureDefinition.changeFromBaselineDeltaE00)
        assertTrue(vector.apparentFaceShift.measured)
        assertNotNull(vector.apparentFaceShift.aggregateDeltaE00)
        assertEquals(1.0, vector.apparentFaceShift.deltaLStar!!, 0.0)
    }

    @Test
    fun `failed pair gates suppress apparent face shift without suppressing other signals`() {
        val vector = ContrastCalculator.calculate(
            baseline = baseline,
            drape = drape,
            fabric = LabColor(61.5, 15.0, 20.0),
            allowApparentFaceShift = false,
        )
        assertFalse(vector.apparentFaceShift.measured)
        assertNotNull(vector.apparentFaceShift.unavailableReason)
        assertEquals(LightnessDirection.SIMILAR_LIGHTNESS, vector.clothSkinSeparation.lightnessDirection)
        assertNotNull(vector.featureDefinition.currentMedianDeltaE00)
    }

    @Test
    fun `missing feature and face ROIs are represented as unavailable not zero`() {
        val minimal = FaceColorObservation(skin = LabColor(50.0, 1.0, 2.0))
        val vector = ContrastCalculator.calculate(
            baseline = minimal,
            drape = minimal,
            fabric = LabColor(80.0, 1.0, 2.0),
            allowApparentFaceShift = true,
        )
        assertNull(vector.featureDefinition.currentMedianDeltaE00)
        assertNull(vector.featureDefinition.changeFromBaselineDeltaE00)
        assertFalse(vector.apparentFaceShift.measured)
    }
}
