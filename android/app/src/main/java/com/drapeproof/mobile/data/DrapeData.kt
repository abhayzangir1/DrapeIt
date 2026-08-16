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
        val isWarm = lab.b > 9.0
        val isCool = lab.b < 5.0
        val isDark = lab.l < 55.0

        val undertone = when {
            isWarm -> "Warm Golden"
            isCool -> "Cool Rose"
            else -> "Balanced Neutral"
        }

        val (season, seasonDesc, bestMetals) = when {
            isWarm && isDark -> Triple(
                "Deep Autumn",
                "Warm, deep, and earthy. You look radiant in rich burgundies, warm terracotta, deep olive, and burnished gold.",
                "Yellow Gold, Antique Brass & Copper",
            )
            isWarm && !isDark -> Triple(
                "Warm Spring",
                "Clear, warm, and sunlit. Glowing in coral, camel, peach, warm ivory, and vibrant moss green.",
                "Polished Yellow Gold & Rose Gold",
            )
            isCool && isDark -> Triple(
                "True Winter",
                "Crisp, vivid, and high-contrast. Striking in cobalt navy, obsidian noir, emerald jewel, and pure snow white.",
                "Silver, Platinum & White Gold",
            )
            isCool && !isDark -> Triple(
                "Soft Summer",
                "Delicate, cool, and soft. Elegant in dusty rose, slate blue, heather mauve, and French navy.",
                "Brushed Silver, White Gold & Rose Gold",
            )
            else -> Triple(
                "Neutral Classic",
                "Balanced tone with natural adaptability across warm and cool palettes.",
                "Rose Gold, Gold & Silver (All Metals)",
            )
        }

        val ita = (kotlin.math.atan2(lab.l - 50.0, lab.b) * 180.0 / Math.PI).toFloat()

        // Genuinely analyze and score candidate spectrum colors against user skin
        val scoredPalette = REFERENCE_COLOR_PALETTE.map { colorHex ->
            val result = com.drapeproof.core.color.TrueColorHarmonyEngine.evaluate(
                skinHex = skinHex,
                fabricHex = colorHex,
                fabricId = "cotton",
            )
            colorHex to result.scorePercent
        }.sortedByDescending { it.second }

        val bestColors = scoredPalette.take(7).map { it.first }
        val worstColors = scoredPalette.takeLast(4).reversed().map { it.first }

        return StoredSkinProfile(
            skinHex = skinHex,
            evidenceTier = EvidenceTier.CONTROLLED_PAIR,
            source = source,
            capturedAtEpochMillis = System.currentTimeMillis(),
            isCalibrated = true,
            undertone = undertone,
            season = season,
            seasonDescription = seasonDesc,
            itaScore = ita,
            bestMetals = bestMetals,
            bestColors = bestColors,
            worstColors = worstColors,
        )
    }
}

private val REFERENCE_COLOR_PALETTE = listOf(
    // Deep Warm Jewels & Earth
    "#831843", // Deep Berry
    "#78350F", // Warm Terracotta
    "#3F6212", // Deep Olive
    "#451A03", // Dark Mahogany
    "#D97706", // Burnished Ochre
    "#B45309", // Warm Rust
    "#9A3412", // Burnt Orange
    "#713F12", // Antique Gold
    "#7C2D12", // Spiced Cinnamon

    // Light Warm & Spring Tones
    "#EA580C", // Bright Coral Flame
    "#CA8A04", // Sunlit Gold
    "#65A30D", // Fresh Spring Grass
    "#0D9488", // Warm Persian Teal
    "#F59E0B", // Sun Amber
    "#FB923C", // Radiant Peach
    "#EAB308", // Goldenrod
    "#84CC16", // Lime Chartreuse

    // Deep Cool Jewels & Winter
    "#1D4ED8", // Royal Cobalt
    "#0F172A", // Obsidian Midnight
    "#047857", // Deep Emerald Jewel
    "#4C1D95", // Imperial Amethyst
    "#BE123C", // Vivid Crimson Ruby
    "#1E293B", // Cool Navy Charcoal
    "#374151", // Graphite Noir
    "#312E81", // Deep Indigo

    // Soft Cool Pastels & Summer
    "#475569", // Slate Blue
    "#64748B", // Dusty Denim
    "#9333EA", // Vibrant Orchid
    "#2563EB", // Sapphire Blue
    "#BE185D", // Heather Magenta
    "#0284C7", // Mediterranean Cerulean
    "#6D28D9", // Royal Violet
    "#93C5FD", // Icy Sky Blue
    "#F472B6", // Soft Pastel Rose
    "#E0E7FF", // Frost Periwinkle
    "#C7D2FE", // Soft Lavender
    "#A7F3D0", // Mint Ice
    "#FDE047", // Pastel Canary
    "#CBD5E1", // Muted Silver Gray
    "#F3E8FF", // Powder Lilac
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
