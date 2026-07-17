package com.drapeproof.core.capture

import kotlin.math.abs

data class QualityThresholds(
    val maxAbsoluteYawDegrees: Double = 5.0,
    val maxAbsolutePitchDegrees: Double = 5.0,
    val maxAbsoluteRollDegrees: Double = 5.0,
    val maxFaceScaleChangeFraction: Double = 0.03,
    val maxClippedPixelFractionExclusive: Double = 0.01,
    val maxAmbientLightCoefficientOfVariation: Double = 0.03,
    val minimumAcceptedFrames: Int = 15,
    val maxTemporalRoiMedianDeltaE: Double = 1.5,
    val maxOpeningClosingBaselineDeltaE: Double = 2.0,
)

/** Measurements already aggregated by the capture layer for one paired drape acquisition. */
data class CaptureQualityMetrics(
    val faceYawDegrees: Double,
    val facePitchDegrees: Double,
    val faceRollDegrees: Double,
    val faceScaleChangeFraction: Double,
    val clippedPixelFraction: Double,
    val ambientLightCoefficientOfVariation: Double?,
    val acceptedFrames: Int,
    val temporalRoiMedianDeltaE: Double,
    val openingClosingBaselineDeltaE: Double,
    val neutralExpression: Boolean,
    val eyesOpen: Boolean,
    val occlusionFree: Boolean,
    val sharpEnough: Boolean,
    val flickerDetected: Boolean,
    val exposureControlled: Boolean,
    val whiteBalanceControlled: Boolean,
    val fabricRegionValid: Boolean,
)

enum class QualityGateCode {
    YAW_OUT_OF_RANGE,
    PITCH_OUT_OF_RANGE,
    ROLL_OUT_OF_RANGE,
    FACE_SCALE_DRIFT,
    PIXEL_CLIPPING,
    AMBIENT_LIGHT_DRIFT,
    AMBIENT_SENSOR_UNAVAILABLE,
    INSUFFICIENT_STABLE_FRAMES,
    TEMPORAL_COLOR_INSTABILITY,
    BASELINE_DRIFT,
    EXPRESSION_CHANGED,
    EYES_NOT_OPEN,
    OCCLUSION_DETECTED,
    BLUR_DETECTED,
    FLICKER_DETECTED,
    EXPOSURE_NOT_CONTROLLED,
    WHITE_BALANCE_NOT_CONTROLLED,
    FABRIC_REGION_INVALID,
}

data class GateViolation(
    val code: QualityGateCode,
    val observed: Double? = null,
    val limit: Double? = null,
)

data class QualityGateResult(
    val passed: Boolean,
    val failures: List<GateViolation>,
    val warnings: List<GateViolation>,
)

object QualityGateEvaluator {
    fun evaluate(
        metrics: CaptureQualityMetrics,
        thresholds: QualityThresholds = QualityThresholds(),
    ): QualityGateResult {
        val failures = buildList {
            maximumAbsolute(metrics.faceYawDegrees, thresholds.maxAbsoluteYawDegrees, QualityGateCode.YAW_OUT_OF_RANGE)
            maximumAbsolute(metrics.facePitchDegrees, thresholds.maxAbsolutePitchDegrees, QualityGateCode.PITCH_OUT_OF_RANGE)
            maximumAbsolute(metrics.faceRollDegrees, thresholds.maxAbsoluteRollDegrees, QualityGateCode.ROLL_OUT_OF_RANGE)
            maximumAbsolute(
                metrics.faceScaleChangeFraction,
                thresholds.maxFaceScaleChangeFraction,
                QualityGateCode.FACE_SCALE_DRIFT,
            )
            if (metrics.clippedPixelFraction >= thresholds.maxClippedPixelFractionExclusive) {
                add(
                    GateViolation(
                        QualityGateCode.PIXEL_CLIPPING,
                        metrics.clippedPixelFraction,
                        thresholds.maxClippedPixelFractionExclusive,
                    ),
                )
            }
            metrics.ambientLightCoefficientOfVariation?.let {
                maximum(it, thresholds.maxAmbientLightCoefficientOfVariation, QualityGateCode.AMBIENT_LIGHT_DRIFT)
            }
            if (metrics.acceptedFrames < thresholds.minimumAcceptedFrames) {
                add(
                    GateViolation(
                        QualityGateCode.INSUFFICIENT_STABLE_FRAMES,
                        metrics.acceptedFrames.toDouble(),
                        thresholds.minimumAcceptedFrames.toDouble(),
                    ),
                )
            }
            maximum(
                metrics.temporalRoiMedianDeltaE,
                thresholds.maxTemporalRoiMedianDeltaE,
                QualityGateCode.TEMPORAL_COLOR_INSTABILITY,
            )
            maximum(
                metrics.openingClosingBaselineDeltaE,
                thresholds.maxOpeningClosingBaselineDeltaE,
                QualityGateCode.BASELINE_DRIFT,
            )
            if (!metrics.neutralExpression) add(GateViolation(QualityGateCode.EXPRESSION_CHANGED))
            if (!metrics.eyesOpen) add(GateViolation(QualityGateCode.EYES_NOT_OPEN))
            if (!metrics.occlusionFree) add(GateViolation(QualityGateCode.OCCLUSION_DETECTED))
            if (!metrics.sharpEnough) add(GateViolation(QualityGateCode.BLUR_DETECTED))
            if (metrics.flickerDetected) add(GateViolation(QualityGateCode.FLICKER_DETECTED))
            if (!metrics.exposureControlled) add(GateViolation(QualityGateCode.EXPOSURE_NOT_CONTROLLED))
            if (!metrics.whiteBalanceControlled) add(GateViolation(QualityGateCode.WHITE_BALANCE_NOT_CONTROLLED))
            if (!metrics.fabricRegionValid) add(GateViolation(QualityGateCode.FABRIC_REGION_INVALID))
        }
        val warnings = buildList {
            if (metrics.ambientLightCoefficientOfVariation == null) {
                add(GateViolation(QualityGateCode.AMBIENT_SENSOR_UNAVAILABLE))
            }
        }
        return QualityGateResult(failures.isEmpty(), failures, warnings)
    }

    private fun MutableList<GateViolation>.maximumAbsolute(
        observed: Double,
        limit: Double,
        code: QualityGateCode,
    ) {
        require(observed.isFinite() && limit.isFinite()) { "Quality metrics and thresholds must be finite" }
        if (abs(observed) > limit) add(GateViolation(code, observed, limit))
    }

    private fun MutableList<GateViolation>.maximum(
        observed: Double,
        limit: Double,
        code: QualityGateCode,
    ) {
        require(observed.isFinite() && limit.isFinite()) { "Quality metrics and thresholds must be finite" }
        if (observed > limit) add(GateViolation(code, observed, limit))
    }
}
