package com.drapeproof.core.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QualityGateEvaluatorTest {
    @Test
    fun `valid controlled capture passes every gate`() {
        val result = QualityGateEvaluator.evaluate(validMetrics())
        assertTrue(result.passed)
        assertTrue(result.failures.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `inclusive maximum gates pass but clipping is strictly below one percent`() {
        val boundary = validMetrics().copy(
            faceYawDegrees = -5.0,
            facePitchDegrees = 5.0,
            faceRollDegrees = -5.0,
            faceScaleChangeFraction = -0.03,
            ambientLightCoefficientOfVariation = 0.03,
            acceptedFrames = 15,
            temporalRoiMedianDeltaE = 1.5,
            openingClosingBaselineDeltaE = 2.0,
        )
        assertTrue(QualityGateEvaluator.evaluate(boundary).passed)

        val clipped = QualityGateEvaluator.evaluate(boundary.copy(clippedPixelFraction = 0.01))
        assertFalse(clipped.passed)
        assertEquals(QualityGateCode.PIXEL_CLIPPING, clipped.failures.single().code)
    }

    @Test
    fun `missing ambient sensor is warning and not measurement failure`() {
        val result = QualityGateEvaluator.evaluate(validMetrics().copy(ambientLightCoefficientOfVariation = null))
        assertTrue(result.passed)
        assertEquals(listOf(QualityGateCode.AMBIENT_SENSOR_UNAVAILABLE), result.warnings.map { it.code })
    }

    @Test
    fun `all independent failure conditions are retained for recapture guidance`() {
        val result = QualityGateEvaluator.evaluate(
            validMetrics().copy(
                faceYawDegrees = 6.0,
                facePitchDegrees = -6.0,
                faceRollDegrees = 6.0,
                faceScaleChangeFraction = 0.031,
                clippedPixelFraction = 0.02,
                ambientLightCoefficientOfVariation = 0.031,
                acceptedFrames = 14,
                temporalRoiMedianDeltaE = 1.51,
                openingClosingBaselineDeltaE = 2.01,
                neutralExpression = false,
                eyesOpen = false,
                occlusionFree = false,
                sharpEnough = false,
                flickerDetected = true,
                exposureControlled = false,
                whiteBalanceControlled = false,
                fabricRegionValid = false,
            ),
        )
        assertFalse(result.passed)
        assertEquals(
            QualityGateCode.entries - QualityGateCode.AMBIENT_SENSOR_UNAVAILABLE,
            result.failures.map { it.code },
        )
    }

    private fun validMetrics() = CaptureQualityMetrics(
        faceYawDegrees = 1.0,
        facePitchDegrees = -1.0,
        faceRollDegrees = 0.5,
        faceScaleChangeFraction = 0.01,
        clippedPixelFraction = 0.001,
        ambientLightCoefficientOfVariation = 0.01,
        acceptedFrames = 20,
        temporalRoiMedianDeltaE = 0.5,
        openingClosingBaselineDeltaE = 0.7,
        neutralExpression = true,
        eyesOpen = true,
        occlusionFree = true,
        sharpEnough = true,
        flickerDetected = false,
        exposureControlled = true,
        whiteBalanceControlled = true,
        fabricRegionValid = true,
    )
}
