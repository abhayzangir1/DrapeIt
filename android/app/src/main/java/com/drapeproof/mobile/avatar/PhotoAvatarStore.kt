package com.drapeproof.mobile.avatar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class AvatarLighting(val displayName: String, val icon: String) {
    DAYLIGHT(displayName = "Natural Daylight", icon = "☀️"),
    INDOOR_WARM(displayName = "Warm Indoor", icon = "💡"),
    STUDIO(displayName = "Studio Neutral", icon = "📸"),
    OTHER(displayName = "Custom", icon = "🖼️"),
}

data class SavedAvatar(
    val id: String,
    val name: String,
    val lighting: AvatarLighting,
    val imagePath: String,
    val skinHex: String? = null,
    val createdAtEpoch: Long = System.currentTimeMillis(),
)

object PhotoAvatarStore {
    private const val PREFS_NAME = "drapeit_photo_avatars"
    private const val KEY_REGISTRY = "avatar_registry_json"
    private const val KEY_ACTIVE_ID = "active_avatar_id"

    private fun getAvatarsDir(context: Context): File {
        val dir = File(context.filesDir, "avatars")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listAvatars(context: Context): List<SavedAvatar> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_REGISTRY, null) ?: return emptyList()
        val list = mutableListOf<SavedAvatar>()
        runCatching {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val file = File(obj.getString("imagePath"))
                if (file.exists()) {
                    list.add(
                        SavedAvatar(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            lighting = runCatching { AvatarLighting.valueOf(obj.getString("lighting")) }.getOrDefault(AvatarLighting.DAYLIGHT),
                            imagePath = obj.getString("imagePath"),
                            skinHex = obj.optString("skinHex").takeIf { it.isNotBlank() },
                            createdAtEpoch = obj.optLong("createdAtEpoch", System.currentTimeMillis()),
                        )
                    )
                }
            }
        }
        return list
    }

    fun saveAvatarFromUri(
        context: Context,
        sourceUri: Uri,
        name: String,
        lighting: AvatarLighting,
        skinHex: String? = null,
    ): SavedAvatar? {
        return runCatching {
            val id = UUID.randomUUID().toString()
            val avatarsDir = getAvatarsDir(context)
            val targetFile = File(avatarsDir, "avatar_$id.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                FileOutputStream(targetFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }
            } ?: return null

            val avatar = SavedAvatar(
                id = id,
                name = name,
                lighting = lighting,
                imagePath = targetFile.absolutePath,
                skinHex = skinHex,
            )

            val existing = listAvatars(context).toMutableList()
            existing.add(0, avatar)
            saveRegistry(context, existing)
            setActiveAvatarId(context, id)
            avatar
        }.getOrNull()
    }

    fun saveAvatarFromBitmap(
        context: Context,
        bitmap: Bitmap,
        name: String,
        lighting: AvatarLighting = AvatarLighting.DAYLIGHT,
        skinHex: String? = null,
    ): SavedAvatar? {
        return runCatching {
            val id = UUID.randomUUID().toString()
            val avatarsDir = getAvatarsDir(context)
            val targetFile = File(avatarsDir, "avatar_$id.jpg")
            FileOutputStream(targetFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            val avatar = SavedAvatar(
                id = id,
                name = name,
                lighting = lighting,
                imagePath = targetFile.absolutePath,
                skinHex = skinHex,
            )
            val existing = listAvatars(context).toMutableList()
            existing.add(0, avatar)
            saveRegistry(context, existing)
            setActiveAvatarId(context, id)
            avatar
        }.getOrNull()
    }

    fun deleteAvatar(context: Context, id: String) {
        val existing = listAvatars(context).toMutableList()
        val toRemove = existing.firstOrNull { it.id == id }
        if (toRemove != null) {
            runCatching { File(toRemove.imagePath).delete() }
            existing.removeAll { it.id == id }
            saveRegistry(context, existing)
        }
    }

    fun getActiveAvatar(context: Context): SavedAvatar? {
        val avatars = listAvatars(context)
        if (avatars.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activeId = prefs.getString(KEY_ACTIVE_ID, null) ?: return null
        if (activeId.isBlank()) return null
        return avatars.firstOrNull { it.id == activeId }
    }

    fun setActiveAvatarId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACTIVE_ID, id)
            .apply()
    }

    fun clearActiveAvatar(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ACTIVE_ID)
            .apply()
    }

    private fun saveRegistry(context: Context, list: List<SavedAvatar>) {
        val arr = JSONArray()
        list.forEach { av ->
            val obj = JSONObject().apply {
                put("id", av.id)
                put("name", av.name)
                put("lighting", av.lighting.name)
                put("imagePath", av.imagePath)
                put("skinHex", av.skinHex ?: "")
                put("createdAtEpoch", av.createdAtEpoch)
            }
            arr.put(obj)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_REGISTRY, arr.toString())
            .apply()
    }

    fun createStudioModelAvatar(context: Context): SavedAvatar {
        val avatarsDir = getAvatarsDir(context)
        val targetFile = File(avatarsDir, "avatar_studio_model.jpg")
        if (!targetFile.exists()) {
            val width = 720
            val height = 960
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val bgPaint = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#EAE6E1") }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val skinPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#D8A47F") }
            canvas.drawRect(width * 0.40f, height * 0.35f, width * 0.60f, height * 0.55f, skinPaint)
            canvas.drawOval(android.graphics.RectF(width * 0.33f, height * 0.15f, width * 0.67f, height * 0.45f), skinPaint)

            val shoulderPath = android.graphics.Path().apply {
                moveTo(0f, height * 0.60f)
                quadTo(width * 0.35f, height * 0.48f, width * 0.40f, height * 0.48f)
                lineTo(width * 0.60f, height * 0.48f)
                quadTo(width * 0.65f, height * 0.48f, width.toFloat(), height * 0.60f)
                lineTo(width.toFloat(), height.toFloat())
                lineTo(0f, height.toFloat())
                close()
            }
            val shirtPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#FFFFFF") }
            canvas.drawPath(shoulderPath, shirtPaint)

            FileOutputStream(targetFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
        }
        val avatar = SavedAvatar(
            id = "studio_model_preset",
            name = "AI Fit Model",
            lighting = AvatarLighting.STUDIO,
            imagePath = targetFile.absolutePath,
            skinHex = "#D8A47F",
        )
        val existing = listAvatars(context).toMutableList()
        if (existing.none { it.id == avatar.id }) {
            existing.add(avatar)
            saveRegistry(context, existing)
        }
        setActiveAvatarId(context, avatar.id)
        return avatar
    }

    fun deleteAll(context: Context) {
        runCatching {
            getAvatarsDir(context).deleteRecursively()
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
