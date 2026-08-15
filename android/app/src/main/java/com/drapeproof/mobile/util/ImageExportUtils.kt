package com.drapeproof.mobile.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ImageExportUtils {

    /**
     * Saves a Bitmap to the public Pictures/DrapeIt gallery folder.
     */
    fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String = "DrapeIt_Look_${System.currentTimeMillis()}"): Boolean {
        return runCatching {
            val filename = "${title}.jpg"
            var outputStream: OutputStream? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "DrapeIt")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + "DrapeIt"
                val file = File(imagesDir)
                if (!file.exists()) {
                    file.mkdirs()
                }
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
            }

            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                it.flush()
            }

            Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
            true
        }.getOrElse {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Rotates a Bitmap clockwise by [degrees] (default 90 degrees).
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float = 90f): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
