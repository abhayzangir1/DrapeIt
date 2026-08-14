package com.drapeproof.mobile.data

import android.content.Context
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.ranking.ContrastIntent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** The latest measured skin sample. Raw face images are never stored here. */
data class StoredSkinProfile(
    val skinHex: String,
    val evidenceTier: EvidenceTier,
    val source: String,
    val capturedAtEpochMillis: Long,
)

object SkinProfileRepository {
    private const val PREFERENCES = "drapeproof_profile"

    fun save(context: Context, profile: StoredSkinProfile) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString("skin_hex", profile.skinHex)
            .putString("evidence_tier", profile.evidenceTier.name)
            .putString("source", profile.source)
            .putLong("captured_at", profile.capturedAtEpochMillis)
            .apply()
    }

    fun load(context: Context): StoredSkinProfile? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val skinHex = preferences.getString("skin_hex", null) ?: return null
        val tier = runCatching {
            EvidenceTier.valueOf(preferences.getString("evidence_tier", null).orEmpty())
        }.getOrNull() ?: return null
        return StoredSkinProfile(
            skinHex = skinHex,
            evidenceTier = tier,
            source = preferences.getString("source", "unknown input") ?: "unknown input",
            capturedAtEpochMillis = preferences.getLong("captured_at", 0L),
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

/** A compact, locally persisted evidence trail. It contains measurements, never image bytes. */
data class LocalDrapeRecord(
    val recordId: String,
    val createdAtEpochMillis: Long,
    val source: String,
    val evidenceTier: EvidenceTier,
    val intent: ContrastIntent?,
    val sku: String,
    val variantId: String,
    val variantName: String,
    val skinHex: String,
    val fabricHex: String,
    val separationDeltaE00: Double,
    val deltaLStar: Double,
    val scoringVersion: String = "contrast-v1",
    val limitations: List<String> = emptyList(),
) {
    companion object {
        fun create(
            source: String,
            evidenceTier: EvidenceTier,
            intent: ContrastIntent?,
            sku: String,
            variantId: String,
            variantName: String,
            skinHex: String,
            fabricHex: String,
            separationDeltaE00: Double,
            deltaLStar: Double,
            limitations: List<String>,
        ) = LocalDrapeRecord(
            recordId = UUID.randomUUID().toString(),
            createdAtEpochMillis = System.currentTimeMillis(),
            source = source,
            evidenceTier = evidenceTier,
            intent = intent,
            sku = sku,
            variantId = variantId,
            variantName = variantName,
            skinHex = skinHex,
            fabricHex = fabricHex,
            separationDeltaE00 = separationDeltaE00,
            deltaLStar = deltaLStar,
            limitations = limitations,
        )
    }
}

object DrapeRecordRepository {
    private const val FILE_NAME = "drape_records.json"
    private const val MAX_RECORDS = 100

    @Synchronized
    fun all(context: Context): List<LocalDrapeRecord> = read(context)
        .sortedByDescending(LocalDrapeRecord::createdAtEpochMillis)

    @Synchronized
    fun add(context: Context, record: LocalDrapeRecord) {
        val records = (listOf(record) + read(context).filterNot { it.recordId == record.recordId })
            .take(MAX_RECORDS)
        write(context, records)
    }

    @Synchronized
    fun deleteAll(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
        val staging = File(context.filesDir, "$FILE_NAME.pending")
        if (staging.exists()) staging.delete()
    }

    fun exportJson(context: Context): String = JSONArray(all(context).map(::toJson)).toString(2)

    private fun read(context: Context): List<LocalDrapeRecord> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.isFile) return emptyList()
        return runCatching {
            val array = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (index in 0 until array.length()) {
                    fromJson(array.getJSONObject(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, records: List<LocalDrapeRecord>) {
        val destination = File(context.filesDir, FILE_NAME)
        val staging = File(context.filesDir, "$FILE_NAME.pending")
        staging.writeText(JSONArray(records.map(::toJson)).toString(), Charsets.UTF_8)
        if (!staging.renameTo(destination)) {
            destination.writeText(staging.readText(Charsets.UTF_8), Charsets.UTF_8)
            staging.delete()
        }
    }

    private fun toJson(record: LocalDrapeRecord) = JSONObject()
        .put("recordId", record.recordId)
        .put("createdAtEpochMillis", record.createdAtEpochMillis)
        .put("source", record.source)
        .put("evidenceTier", record.evidenceTier.name)
        .put("intent", record.intent?.name ?: JSONObject.NULL)
        .put("sku", record.sku)
        .put("variantId", record.variantId)
        .put("variantName", record.variantName)
        .put("skinHex", record.skinHex)
        .put("fabricHex", record.fabricHex)
        .put("separationDeltaE00", record.separationDeltaE00)
        .put("deltaLStar", record.deltaLStar)
        .put("scoringVersion", record.scoringVersion)
        .put("limitations", JSONArray(record.limitations))

    private fun fromJson(json: JSONObject): LocalDrapeRecord? = runCatching {
        val limitations = json.optJSONArray("limitations")?.let { array ->
            buildList {
                for (index in 0 until array.length()) add(array.getString(index))
            }
        }.orEmpty()
        LocalDrapeRecord(
            recordId = json.getString("recordId"),
            createdAtEpochMillis = json.getLong("createdAtEpochMillis"),
            source = json.getString("source"),
            evidenceTier = EvidenceTier.valueOf(json.getString("evidenceTier")),
            intent = if (json.isNull("intent")) null else ContrastIntent.valueOf(json.getString("intent")),
            sku = json.getString("sku"),
            variantId = json.getString("variantId"),
            variantName = json.getString("variantName"),
            skinHex = json.getString("skinHex"),
            fabricHex = json.getString("fabricHex"),
            separationDeltaE00 = json.getDouble("separationDeltaE00"),
            deltaLStar = json.getDouble("deltaLStar"),
            scoringVersion = json.optString("scoringVersion", "contrast-v1"),
            limitations = limitations,
        )
    }.getOrNull()
}

fun EvidenceTier.displayName(): String = when (this) {
    EvidenceTier.CONTROLLED_PAIR -> "Controlled pair"
    EvidenceTier.SAME_SCENE -> "Same scene"
    EvidenceTier.SEPARATE_PHOTO_ESTIMATE -> "Separate-photo estimate"
    EvidenceTier.PREVIEW_ONLY -> "Preview only"
}
