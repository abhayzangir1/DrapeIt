package com.drapeproof.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedSuitedColor(
    val id: String = UUID.randomUUID().toString(),
    val colorHex: String,
    val colorName: String,
    val fabricId: String? = null,
    val fabricName: String? = null,
    val matchScorePercent: Int = 0,
    val contrastLabel: String = "Natural Balance",
    val createdAtEpoch: Long = System.currentTimeMillis(),
)

object SuitedColorsRepository {
    private const val PREFS_NAME = "drapeit_suited_colors"
    private const val KEY_COLORS_JSON = "saved_suited_colors_json"

    fun list(context: Context): List<SavedSuitedColor> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_COLORS_JSON, null) ?: return emptyList()
        val list = mutableListOf<SavedSuitedColor>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedSuitedColor(
                        id = obj.getString("id"),
                        colorHex = obj.getString("colorHex"),
                        colorName = obj.getString("colorName"),
                        fabricId = obj.optString("fabricId").takeIf { it.isNotBlank() },
                        fabricName = obj.optString("fabricName").takeIf { it.isNotBlank() },
                        matchScorePercent = obj.optInt("matchScorePercent", 0),
                        contrastLabel = obj.optString("contrastLabel", "Natural Balance"),
                        createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis()),
                    )
                )
            }
        }
        return list
    }

    fun add(context: Context, item: SavedSuitedColor) {
        val list = list(context).toMutableList()
        list.removeAll { it.colorHex.equals(item.colorHex, ignoreCase = true) && it.fabricId == item.fabricId }
        list.add(0, item)
        saveList(context, list)
    }

    fun remove(context: Context, id: String) {
        val list = list(context).toMutableList()
        list.removeAll { it.id == id }
        saveList(context, list)
    }

    fun isFavorite(context: Context, colorHex: String, fabricId: String?): Boolean {
        return list(context).any { it.colorHex.equals(colorHex, ignoreCase = true) && it.fabricId == fabricId }
    }

    private fun saveList(context: Context, list: List<SavedSuitedColor>) {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("colorHex", item.colorHex)
                put("colorName", item.colorName)
                put("fabricId", item.fabricId ?: "")
                put("fabricName", item.fabricName ?: "")
                put("matchScorePercent", item.matchScorePercent)
                put("contrastLabel", item.contrastLabel)
                put("createdAtEpoch", item.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_COLORS_JSON, arr.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
