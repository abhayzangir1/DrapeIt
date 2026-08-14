package com.drapeproof.mobile.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class SavedTryOnOutfit(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val fabricName: String,
    val colorHex: String,
    val topwearCut: String,
    val bottomwearCut: String,
    val bottomwearColor: String,
    val resultImagePath: String? = null,
    val matchScorePercent: Int = 96,
    val createdAtEpoch: Long = System.currentTimeMillis(),
)

data class SavedWishlistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val retailer: String,
    val price: String,
    val fabricName: String,
    val colorHex: String,
    val shoppingUrl: String,
    val thumbnailPath: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
)

object WardrobeRepository {
    private const val PREFS_NAME = "drapeit_wardrobe_store"
    private const val KEY_OUTFITS_JSON = "saved_outfits_json"
    private const val KEY_WISHLIST_JSON = "saved_wishlist_json"

    // --- Outfits ---
    fun listOutfits(context: Context): List<SavedTryOnOutfit> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_OUTFITS_JSON, null) ?: return emptyList()
        val list = mutableListOf<SavedTryOnOutfit>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedTryOnOutfit(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        fabricName = obj.getString("fabricName"),
                        colorHex = obj.getString("colorHex"),
                        topwearCut = obj.getString("topwearCut"),
                        bottomwearCut = obj.getString("bottomwearCut"),
                        bottomwearColor = obj.getString("bottomwearColor"),
                        resultImagePath = obj.optString("resultImagePath").takeIf { it.isNotBlank() },
                        matchScorePercent = obj.optInt("matchScorePercent", 96),
                        createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis()),
                    )
                )
            }
        }
        return list
    }

    fun addOutfit(context: Context, outfit: SavedTryOnOutfit) {
        val list = listOutfits(context).toMutableList()
        list.add(0, outfit)
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("fabricName", item.fabricName)
                put("colorHex", item.colorHex)
                put("topwearCut", item.topwearCut)
                put("bottomwearCut", item.bottomwearCut)
                put("bottomwearColor", item.bottomwearColor)
                put("resultImagePath", item.resultImagePath ?: "")
                put("matchScorePercent", item.matchScorePercent)
                put("createdAtEpoch", item.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OUTFITS_JSON, arr.toString())
            .apply()
    }

    fun removeOutfit(context: Context, id: String) {
        val list = listOutfits(context).toMutableList()
        val toRemove = list.firstOrNull { it.id == id }
        toRemove?.resultImagePath?.let { path -> runCatching { File(path).delete() } }
        list.removeAll { it.id == id }
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("fabricName", item.fabricName)
                put("colorHex", item.colorHex)
                put("topwearCut", item.topwearCut)
                put("bottomwearCut", item.bottomwearCut)
                put("bottomwearColor", item.bottomwearColor)
                put("resultImagePath", item.resultImagePath ?: "")
                put("matchScorePercent", item.matchScorePercent)
                put("createdAtEpoch", item.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_OUTFITS_JSON, arr.toString())
            .apply()
    }

    // --- Wishlist ---
    fun listWishlist(context: Context): List<SavedWishlistItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_WISHLIST_JSON, null) ?: return emptyList()
        val list = mutableListOf<SavedWishlistItem>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedWishlistItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        retailer = obj.getString("retailer"),
                        price = obj.getString("price"),
                        fabricName = obj.getString("fabricName"),
                        colorHex = obj.getString("colorHex"),
                        shoppingUrl = obj.getString("shoppingUrl"),
                        thumbnailPath = obj.optString("thumbnailPath").takeIf { it.isNotBlank() },
                        createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis()),
                    )
                )
            }
        }
        return list
    }

    fun addWishlistItem(context: Context, item: SavedWishlistItem) {
        val list = listWishlist(context).toMutableList()
        list.removeAll { it.shoppingUrl == item.shoppingUrl }
        list.add(0, item)
        val arr = JSONArray()
        list.forEach { w ->
            val obj = JSONObject().apply {
                put("id", w.id)
                put("title", w.title)
                put("retailer", w.retailer)
                put("price", w.price)
                put("fabricName", w.fabricName)
                put("colorHex", w.colorHex)
                put("shoppingUrl", w.shoppingUrl)
                put("thumbnailPath", w.thumbnailPath ?: "")
                put("createdAtEpoch", w.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_WISHLIST_JSON, arr.toString())
            .apply()
    }

    fun removeWishlistItem(context: Context, id: String) {
        val list = listWishlist(context).toMutableList()
        list.removeAll { it.id == id }
        val arr = JSONArray()
        list.forEach { w ->
            val obj = JSONObject().apply {
                put("id", w.id)
                put("title", w.title)
                put("retailer", w.retailer)
                put("price", w.price)
                put("fabricName", w.fabricName)
                put("colorHex", w.colorHex)
                put("shoppingUrl", w.shoppingUrl)
                put("thumbnailPath", w.thumbnailPath ?: "")
                put("createdAtEpoch", w.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_WISHLIST_JSON, arr.toString())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
