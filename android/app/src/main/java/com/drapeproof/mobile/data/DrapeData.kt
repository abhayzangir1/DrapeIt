package com.drapeproof.mobile.data

import android.content.Context
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.ranking.ContrastIntent
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** The calibrated user skin & seasonal color profile. Raw face images are never stored. */
data class StoredSkinProfile(
    val skinHex: String,
    val evidenceTier: EvidenceTier,
    val source: String,
    val capturedAtEpochMillis: Long,
    val isCalibrated: Boolean = true,
    val undertone: String = "",
    val season: String = "",
    val seasonDescription: String = "",
    val itaScore: Float = 0.0f,
    val bestMetals: String = "",
    val bestColors: List<String> = emptyList(),
    val worstColors: List<String> = emptyList(),
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
            .putBoolean("is_calibrated", profile.isCalibrated)
            .putString("undertone", profile.undertone)
            .putString("season", profile.season)
            .putString("season_desc", profile.seasonDescription)
            .putFloat("ita_score", profile.itaScore)
            .putString("best_metals", profile.bestMetals)
            .putString("best_colors", profile.bestColors.joinToString(","))
            .putString("worst_colors", profile.worstColors.joinToString(","))
            .apply()
    }

    fun load(context: Context): StoredSkinProfile? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val skinHex = preferences.getString("skin_hex", null) ?: return null
        val tier = runCatching {
            EvidenceTier.valueOf(preferences.getString("evidence_tier", null).orEmpty())
        }.getOrNull() ?: EvidenceTier.CONTROLLED_PAIR

        val isCalibrated = preferences.getBoolean("is_calibrated", false)
        val undertone = preferences.getString("undertone", null)
        val season = preferences.getString("season", null)

        val bestColors = preferences.getString("best_colors", null)?.split(",")?.filter { it.isNotBlank() }
            ?: emptyList()
        val worstColors = preferences.getString("worst_colors", null)?.split(",")?.filter { it.isNotBlank() }
            ?: emptyList()

        return StoredSkinProfile(
            skinHex = skinHex,
            evidenceTier = tier,
            source = preferences.getString("source", "face_scan") ?: "face_scan",
            capturedAtEpochMillis = preferences.getLong("captured_at", 0L),
            isCalibrated = isCalibrated,
            undertone = undertone ?: "",
            season = season ?: "",
            seasonDescription = preferences.getString("season_desc", "") ?: "",
            itaScore = preferences.getFloat("ita_score", 0.0f),
            bestMetals = preferences.getString("best_metals", "") ?: "",
            bestColors = bestColors,
            worstColors = worstColors,
        )
    }

    fun isCalibrated(context: Context): Boolean {
        val profile = load(context)
        return profile != null && profile.isCalibrated
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun deriveProfileFromSkinHex(skinHex: String, source: String = "face_scan"): StoredSkinProfile {
        val lab = com.drapeproof.core.color.ColorConversions.hexToLab(skinHex)
        val isWarm = lab.b > 11.0
        val isCool = lab.b < 7.0
        val isNeutral = !isWarm && !isCool
        val isDark = lab.l < 55.0

        val undertone = when {
            isWarm -> "Warm Golden"
            isCool -> "Cool Rose"
            else -> "Balanced Neutral"
        }

        val (season, seasonDesc, bestMetals, bestColors, worstColors) = when {
            isWarm && isDark -> Quintuple(
                "Deep Autumn",
                "Warm, deep, and earthy. You look radiant in rich burgundies, warm terracotta, deep olive, and burnished gold.",
                "Yellow Gold, Antique Brass & Copper",
                listOf("#831843", "#78350F", "#3F6212", "#0F172A", "#D97706", "#B45309", "#451A03"),
                listOf("#93C5FD", "#F472B6", "#E0E7FF", "#C7D2FE"),
            )
            isWarm && !isDark -> Quintuple(
                "Warm Spring",
                "Clear, warm, and sunlit. Glowing in coral, camel, peach, warm ivory, and vibrant moss green.",
                "Polished Yellow Gold & Rose Gold",
                listOf("#EA580C", "#CA8A04", "#65A30D", "#0D9488", "#E11D48", "#D97706", "#F59E0B"),
                listOf("#475569", "#64748B", "#334155", "#94A3B8"),
            )
            isCool && isDark -> Quintuple(
                "True Winter",
                "Crisp, vivid, and high-contrast. Striking in cobalt navy, obsidian noir, emerald jewel, and pure snow white.",
                "Silver, Platinum & White Gold",
                listOf("#1D4ED8", "#0F172A", "#047857", "#4C1D95", "#BE123C", "#1E293B", "#374151"),
                listOf("#FDE047", "#F59E0B", "#B45309", "#78350F"),
            )
            isCool && !isDark -> Quintuple(
                "Soft Summer",
                "Delicate, cool, and soft. Elegant in dusty rose, slate blue, heather mauve, and French navy.",
                "Brushed Silver, White Gold & Rose Gold",
                listOf("#475569", "#64748B", "#9333EA", "#2563EB", "#BE185D", "#0284C7", "#6D28D9"),
                listOf("#EA580C", "#F97316", "#D97706", "#CA8A04"),
            )
            else -> Quintuple(
                "Neutral Classic",
                "Balanced tone with natural adaptability across warm and cool palettes.",
                "Rose Gold, Gold & Silver (All Metals)",
                listOf("#831843", "#1D4ED8", "#3F6212", "#78350F", "#0F172A", "#D97706", "#4B5563"),
                listOf("#84CC16", "#E11D48"),
            )
        }

        val ita = (kotlin.math.atan2(lab.l - 50.0, lab.b) * 180.0 / Math.PI).toFloat()

        return StoredSkinProfile(
            skinHex = skinHex,
            evidenceTier = EvidenceTier.CONTROLLED_PAIR,
            source = source,
            capturedAtEpochMillis = System.currentTimeMillis(),
            isCalibrated = true,
            undertone = undertone,
            season = season,
            seasonDescription = seasonDesc,
            itaScore = ita.toFloat(),
            bestMetals = bestMetals,
            bestColors = bestColors,
            worstColors = worstColors,
        )
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
)

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
