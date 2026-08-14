package com.drapeproof.mobile.data

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class DrapeSnap(
    val id: String = UUID.randomUUID().toString(),
    val imagePath: String,
    val colorHex: String,
    val colorName: String,
    val fabricId: String,
    val fabricName: String,
    val matchScorePercent: Int,
    val skinHex: String? = null,
    val timestampEpoch: Long = System.currentTimeMillis(),
)

object DrapeSnapRepository {
    private const val PREFS_NAME = "drapeit_snaps_store"
    private const val KEY_SNAPS_JSON = "saved_snaps_registry"

    private fun getSnapsDir(context: Context): File {
        val dir = File(context.filesDir, "drape_snaps")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun list(context: Context): List<DrapeSnap> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_SNAPS_JSON, null) ?: return emptyList()
        val list = mutableListOf<DrapeSnap>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.getString("imagePath")
                if (File(path).exists()) {
                    list.add(
                        DrapeSnap(
                            id = obj.getString("id"),
                            imagePath = path,
                            colorHex = obj.getString("colorHex"),
                            colorName = obj.getString("colorName"),
                            fabricId = obj.getString("fabricId"),
                            fabricName = obj.getString("fabricName"),
                            matchScorePercent = obj.optInt("matchScorePercent", 90),
                            skinHex = obj.optString("skinHex").takeIf { it.isNotBlank() },
                            timestampEpoch = obj.optLong("timestampEpoch", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }
        return list
    }

    fun saveSnap(
        context: Context,
        bitmap: Bitmap,
        colorHex: String,
        colorName: String,
        fabricId: String,
        fabricName: String,
        matchScorePercent: Int,
        skinHex: String?,
    ): DrapeSnap? {
        return runCatching {
            val id = UUID.randomUUID().toString()
            val dir = getSnapsDir(context)
            val file = File(dir, "snap_$id.jpg")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            val snap = DrapeSnap(
                id = id,
                imagePath = file.absolutePath,
                colorHex = colorHex,
                colorName = colorName,
                fabricId = fabricId,
                fabricName = fabricName,
                matchScorePercent = matchScorePercent,
                skinHex = skinHex,
            )

            val existing = list(context).toMutableList()
            existing.add(0, snap)
            saveList(context, existing)
            snap
        }.getOrNull()
    }

    fun delete(context: Context, id: String) {
        val existing = list(context).toMutableList()
        val toRemove = existing.find { it.id == id }
        if (toRemove != null) {
            runCatching { File(toRemove.imagePath).delete() }
            existing.remove(toRemove)
            saveList(context, existing)
        }
    }

    private fun saveList(context: Context, list: List<DrapeSnap>) {
        val arr = JSONArray()
        list.forEach { snap ->
            arr.put(
                JSONObject().apply {
                    put("id", snap.id)
                    put("imagePath", snap.imagePath)
                    put("colorHex", snap.colorHex)
                    put("colorName", snap.colorName)
                    put("fabricId", snap.fabricId)
                    put("fabricName", snap.fabricName)
                    put("matchScorePercent", snap.matchScorePercent)
                    snap.skinHex?.let { put("skinHex", it) }
                    put("timestampEpoch", snap.timestampEpoch)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SNAPS_JSON, arr.toString())
            .apply()
    }
}
