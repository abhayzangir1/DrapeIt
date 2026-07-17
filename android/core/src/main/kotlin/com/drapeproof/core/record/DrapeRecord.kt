package com.drapeproof.core.record

import com.drapeproof.core.capture.CaptureQualityMetrics
import com.drapeproof.core.capture.QualityGateCode
import com.drapeproof.core.domain.ContrastVector
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.domain.ProductProvenance
import com.drapeproof.core.ranking.ContrastIntent

enum class CameraControlMode {
    LOCKED_3A,
    MANUAL,
    UNCONTROLLED,
}

data class DeviceMetadata(
    val manufacturer: String,
    val model: String,
    val cameraId: String,
    val operatingSystemVersion: String,
    val appVersion: String,
    val exposureControlMode: CameraControlMode,
    val whiteBalanceControlMode: CameraControlMode,
)

data class CaptureQualitySnapshot(
    val passed: Boolean,
    val metrics: CaptureQualityMetrics,
    val failureCodes: List<QualityGateCode>,
    val warningCodes: List<QualityGateCode>,
)

data class ProductSelection(
    val sku: String,
    val variantId: String,
    val displayColorHex: String?,
    val provenance: ProductProvenance,
)

data class RankingSnapshot(
    val intent: ContrastIntent,
    val targetPercentile: Double,
    val rankedVariantIds: List<String>,
)

enum class YouCamFeature {
    FACIAL_COLOR_TONES,
    APPAREL_TRY_ON,
}

enum class RemoteTaskState {
    NOT_REQUESTED,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
}

/** Contains status and an opaque task reference, never an API key or expiring result URL. */
data class YouCamTaskSummary(
    val feature: YouCamFeature,
    val state: RemoteTaskState,
    val opaqueTaskId: String? = null,
)

data class DrapeRecord(
    val recordId: String,
    val profileId: String,
    val createdAtEpochMillis: Long,
    val selectedIntent: ContrastIntent,
    val evidenceTier: EvidenceTier,
    val product: ProductSelection,
    val contrastVector: ContrastVector,
    val captureQuality: CaptureQualitySnapshot?,
    val device: DeviceMetadata,
    val ranking: RankingSnapshot?,
    val scoringVersion: String,
    val youCamTasks: List<YouCamTaskSummary> = emptyList(),
    val limitations: List<String> = emptyList(),
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

enum class RecordValidationIssue {
    INVALID_SCHEMA_VERSION,
    MISSING_IDENTITY,
    INVALID_TIMESTAMP,
    MISSING_SCORING_VERSION,
    CONTROLLED_PAIR_WITHOUT_PASSING_QUALITY,
    CONTROLLED_PAIR_WITHOUT_FACE_SHIFT,
    INVALID_DISPLAY_HEX,
    RANKING_INTENT_MISMATCH,
}

object DrapeRecordValidator {
    fun validate(record: DrapeRecord): Set<RecordValidationIssue> = buildSet {
        if (record.schemaVersion <= 0) add(RecordValidationIssue.INVALID_SCHEMA_VERSION)
        if (record.recordId.isBlank() || record.profileId.isBlank() ||
            record.product.sku.isBlank() || record.product.variantId.isBlank()
        ) {
            add(RecordValidationIssue.MISSING_IDENTITY)
        }
        if (record.createdAtEpochMillis < 0L) add(RecordValidationIssue.INVALID_TIMESTAMP)
        if (record.scoringVersion.isBlank()) add(RecordValidationIssue.MISSING_SCORING_VERSION)
        if (record.evidenceTier == EvidenceTier.CONTROLLED_PAIR) {
            if (record.captureQuality?.passed != true) {
                add(RecordValidationIssue.CONTROLLED_PAIR_WITHOUT_PASSING_QUALITY)
            }
            if (!record.contrastVector.apparentFaceShift.measured) {
                add(RecordValidationIssue.CONTROLLED_PAIR_WITHOUT_FACE_SHIFT)
            }
        }
        record.product.displayColorHex?.let {
            if (!HEX_PATTERN.matches(it)) add(RecordValidationIssue.INVALID_DISPLAY_HEX)
        }
        if (record.ranking != null && record.ranking.intent != record.selectedIntent) {
            add(RecordValidationIssue.RANKING_INTENT_MISMATCH)
        }
    }

    private val HEX_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
}
