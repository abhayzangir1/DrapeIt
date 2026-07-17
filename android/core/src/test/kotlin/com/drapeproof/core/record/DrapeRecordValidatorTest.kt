package com.drapeproof.core.record

import com.drapeproof.core.capture.CaptureQualityMetrics
import com.drapeproof.core.capture.QualityGateEvaluator
import com.drapeproof.core.color.LabColor
import com.drapeproof.core.domain.ContrastCalculator
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.domain.FaceColorObservation
import com.drapeproof.core.domain.ProductProvenance
import com.drapeproof.core.ranking.ContrastIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrapeRecordValidatorTest {
    @Test
    fun `valid controlled record has no integrity issues`() {
        assertTrue(DrapeRecordValidator.validate(validRecord()).isEmpty())
    }

    @Test
    fun `controlled label cannot survive failed capture or unavailable face shift`() {
        val record = validRecord()
        val invalid = record.copy(
            contrastVector = record.contrastVector.copy(
                apparentFaceShift = record.contrastVector.apparentFaceShift.copy(measured = false),
            ),
            captureQuality = record.captureQuality?.copy(passed = false),
        )
        assertEquals(
            setOf(
                RecordValidationIssue.CONTROLLED_PAIR_WITHOUT_PASSING_QUALITY,
                RecordValidationIssue.CONTROLLED_PAIR_WITHOUT_FACE_SHIFT,
            ),
            DrapeRecordValidator.validate(invalid),
        )
    }

    @Test
    fun `schema catches malformed export-facing metadata`() {
        val invalid = validRecord().copy(
            recordId = "",
            createdAtEpochMillis = -1L,
            scoringVersion = "",
            product = validRecord().product.copy(displayColorHex = "red"),
            ranking = validRecord().ranking?.copy(intent = ContrastIntent.BOLD),
        )
        assertEquals(
            setOf(
                RecordValidationIssue.MISSING_IDENTITY,
                RecordValidationIssue.INVALID_TIMESTAMP,
                RecordValidationIssue.MISSING_SCORING_VERSION,
                RecordValidationIssue.INVALID_DISPLAY_HEX,
                RecordValidationIssue.RANKING_INTENT_MISMATCH,
            ),
            DrapeRecordValidator.validate(invalid),
        )
    }

    private fun validRecord(): DrapeRecord {
        val metrics = validMetrics()
        val quality = QualityGateEvaluator.evaluate(metrics)
        val baseline = FaceColorObservation(
            skin = LabColor(60.0, 10.0, 15.0),
            cheek = LabColor(60.0, 10.0, 15.0),
            underChin = LabColor(50.0, 10.0, 12.0),
        )
        val drape = baseline.copy(
            skin = LabColor(61.0, 11.0, 15.0),
            cheek = LabColor(61.0, 11.0, 15.0),
            underChin = LabColor(51.0, 10.0, 12.0),
        )
        return DrapeRecord(
            recordId = "record-1",
            profileId = "local-profile-1",
            createdAtEpochMillis = 1_750_000_000_000L,
            selectedIntent = ContrastIntent.BALANCED,
            evidenceTier = EvidenceTier.CONTROLLED_PAIR,
            product = ProductSelection("scarf-01", "blue", "#3355AA", ProductProvenance.EXACT_CATALOG_VARIANT),
            contrastVector = ContrastCalculator.calculate(
                baseline,
                drape,
                LabColor(30.0, 20.0, -30.0),
                allowApparentFaceShift = true,
            ),
            captureQuality = CaptureQualitySnapshot(
                passed = quality.passed,
                metrics = metrics,
                failureCodes = quality.failures.map { it.code },
                warningCodes = quality.warnings.map { it.code },
            ),
            device = DeviceMetadata(
                manufacturer = "Samsung",
                model = "Galaxy F15",
                cameraId = "1",
                operatingSystemVersion = "16",
                appVersion = "1.0",
                exposureControlMode = CameraControlMode.LOCKED_3A,
                whiteBalanceControlMode = CameraControlMode.LOCKED_3A,
            ),
            ranking = RankingSnapshot(ContrastIntent.BALANCED, 0.5, listOf("blue", "green", "red")),
            scoringVersion = "1.0.0",
            youCamTasks = listOf(YouCamTaskSummary(YouCamFeature.FACIAL_COLOR_TONES, RemoteTaskState.SUCCEEDED, "task-1")),
        )
    }

    private fun validMetrics() = CaptureQualityMetrics(
        faceYawDegrees = 0.0,
        facePitchDegrees = 0.0,
        faceRollDegrees = 0.0,
        faceScaleChangeFraction = 0.0,
        clippedPixelFraction = 0.0,
        ambientLightCoefficientOfVariation = 0.01,
        acceptedFrames = 15,
        temporalRoiMedianDeltaE = 0.5,
        openingClosingBaselineDeltaE = 0.5,
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
