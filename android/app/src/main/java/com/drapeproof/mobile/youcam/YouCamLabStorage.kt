package com.drapeproof.mobile.youcam

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.drapeproof.mobile.network.UploadInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

internal object YouCamLabStore {
    private const val PREFS = "youcam_lab"
    private const val FACIAL_TASK = "facial_task_id"
    private const val TRY_ON_TASK = "try_on_task_id"
    private const val FACIAL_OPERATION = "facial_operation_id"
    private const val TRY_ON_OPERATION = "try_on_operation_id"
    private const val TRY_ON_RESULT = "try_on_result_path"

    fun facialTaskId(context: Context): String? = prefs(context).getString(FACIAL_TASK, null)

    fun saveFacialTaskId(context: Context, taskId: String) {
        prefs(context).edit().putString(FACIAL_TASK, taskId).apply()
    }

    fun facialOperationId(context: Context): String? = prefs(context).getString(FACIAL_OPERATION, null)

    fun saveFacialOperationId(context: Context, operationId: String) {
        prefs(context).edit().putString(FACIAL_OPERATION, operationId).commit()
    }

    fun clearFacialOperationId(context: Context) {
        prefs(context).edit().remove(FACIAL_OPERATION).commit()
    }

    fun tryOnTaskId(context: Context): String? = prefs(context).getString(TRY_ON_TASK, null)

    fun saveTryOnTaskId(context: Context, taskId: String) {
        prefs(context).edit().putString(TRY_ON_TASK, taskId).apply()
    }

    fun tryOnOperationId(context: Context): String? = prefs(context).getString(TRY_ON_OPERATION, null)

    fun saveTryOnOperationId(context: Context, operationId: String) {
        prefs(context).edit().putString(TRY_ON_OPERATION, operationId).commit()
    }

    fun clearTryOnOperationId(context: Context) {
        prefs(context).edit().remove(TRY_ON_OPERATION).commit()
    }

    fun tryOnResultPath(context: Context): String? = prefs(context)
        .getString(TRY_ON_RESULT, null)
        ?.takeIf { File(it).isFile }

    fun saveTryOnResultPath(context: Context, path: String) {
        prefs(context).edit().putString(TRY_ON_RESULT, path).apply()
    }

    fun clearFacialOutcome(context: Context) {
        prefs(context).edit().remove(FACIAL_TASK).apply()
    }

    fun clearTryOnOutcome(context: Context) {
        val result = prefs(context).getString(TRY_ON_RESULT, null)
        result?.let { runCatching { File(it).delete() } }
        prefs(context).edit().remove(TRY_ON_TASK).remove(TRY_ON_RESULT).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

internal suspend fun prepareUpload(
    context: Context,
    uri: Uri,
    role: String,
    jpegOnly: Boolean,
): UploadInput = withContext(Dispatchers.IO) {
    val sourceBytes = context.contentResolver.openInputStream(uri)?.use(::readSourceBounded)
        ?: throw IllegalArgumentException("The selected image could not be opened.")
    val sourceType = sniffImageType(sourceBytes)
        ?: throw IllegalArgumentException("Choose an unedited JPEG${if (jpegOnly) "" else " or PNG"} image.")
    if (jpegOnly && sourceType != "image/jpeg") {
        throw IllegalArgumentException("Facial Color Tones requires a JPEG. Choose the original camera JPEG.")
    }
    val normalized = normalizeForApi(sourceBytes, sourceType, jpegOnly)
    val bytes = normalized.first
    val contentType = normalized.second
    val extension = if (contentType == "image/png") "png" else "jpg"
    UploadInput(
        contentType = contentType,
        fileName = "${role}-${System.currentTimeMillis()}.$extension",
        bytes = bytes,
    )
}

internal suspend fun decodePreview(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, PREVIEW_EDGE)
        }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}

internal suspend fun decodePreview(path: String): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
        BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, RESULT_PREVIEW_EDGE)
            },
        )
    }.getOrNull()
}

internal suspend fun demoScarfUri(context: Context): Uri = withContext(Dispatchers.IO) {
    val destination = File(context.cacheDir, "drapeproof-demo-scarf-cobalt.png")
    if (!destination.isFile || destination.length() == 0L) {
        context.assets.open("demo_scarf_cobalt.png").use { input ->
            destination.outputStream().buffered().use { output -> input.copyTo(output) }
        }
    }
    Uri.fromFile(destination)
}

/**
 * Copies a short-lived VTO result into app-private storage immediately. The URL is deliberately
 * never written to preferences, logs, or a Drape Record.
 */
internal suspend fun saveTryOnResult(
    context: Context,
    temporaryUrl: String,
    taskId: String,
): String = withContext(Dispatchers.IO) {
    val uri = URI(temporaryUrl)
    require(uri.scheme == "https" && uri.host != null && uri.userInfo == null) {
        "The result image location was not trusted."
    }
    val connection = (URL(temporaryUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 12_000
        readTimeout = 25_000
        instanceFollowRedirects = false
        setRequestProperty("Accept", "image/jpeg,image/png")
    }
    var temporaryFile: File? = null
    try {
        val status = connection.responseCode
        if (status !in 200..299) throw IllegalStateException("The temporary result image could not be copied.")
        val declaredType = connection.contentType?.substringBefore(';')?.trim()?.lowercase()
        if (declaredType != null && declaredType !in setOf(
                "image/jpeg",
                "image/jpg",
                "image/png",
                "application/octet-stream",
            )
        ) {
            throw IllegalStateException("The service returned an unsupported result image.")
        }
        val declaredLength = connection.contentLengthLong
        if (declaredLength > MAX_RESULT_BYTES) {
            throw IllegalStateException("The result image was larger than the safe limit.")
        }

        val directory = File(context.filesDir, "youcam-results").apply { mkdirs() }
        require(directory.isDirectory) { "Private result storage is unavailable." }
        val name = MessageDigest.getInstance("SHA-256")
            .digest(taskId.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
        temporaryFile = File.createTempFile("incoming-", ".tmp", directory)
        connection.inputStream.use { input ->
            temporaryFile.outputStream().buffered().use { output ->
                val buffer = ByteArray(8_192)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESULT_BYTES) {
                        throw IllegalStateException("The result image was larger than the safe limit.")
                    }
                    output.write(buffer, 0, count)
                }
            }
        }
        val signature = ByteArray(8)
        val signatureLength = temporaryFile.inputStream().use { it.read(signature) }
        val detectedType = sniffImageType(signature.copyOf(signatureLength.coerceAtLeast(0)))
            ?: throw IllegalStateException("The service result was not a valid JPEG or PNG image.")
        val normalizedDeclaredType = if (declaredType == "image/jpg") "image/jpeg" else declaredType
        if (normalizedDeclaredType?.startsWith("image/") == true && normalizedDeclaredType != detectedType) {
            throw IllegalStateException("The service result image type did not match its content.")
        }
        val extension = if (detectedType == "image/png") "png" else "jpg"
        val destination = File(directory, "vto-$name.$extension")
        if (!temporaryFile.renameTo(destination)) {
            temporaryFile.copyTo(destination, overwrite = true)
            temporaryFile.delete()
        }
        YouCamLabStore.saveTryOnResultPath(context, destination.absolutePath)
        destination.absolutePath
    } finally {
        connection.disconnect()
        temporaryFile?.takeIf(File::exists)?.delete()
    }
}

private fun readSourceBounded(input: java.io.InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8_192)
    var total = 0
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        total += count
        if (total > MAX_SOURCE_BYTES) {
            throw IllegalArgumentException("The source exceeds 40 MB. Export a full-quality JPEG before trying again.")
        }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

/**
 * Keeps an already-valid image byte-for-byte. Rotation, oversize dimensions, or an oversize
 * payload trigger a geometry-only normalization: EXIF orientation, long-edge resize, and a
 * high-quality encode. No color, skin, contrast, sharpening, or beauty transform is applied.
 */
private fun normalizeForApi(
    source: ByteArray,
    sourceType: String,
    jpegOnly: Boolean,
): Pair<ByteArray, String> {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IllegalArgumentException("The image dimensions could not be read.")
    }
    if (!hasMinimumApiGeometry(bounds.outWidth, bounds.outHeight)) {
        throw IllegalArgumentException("The image needs a short edge of at least 384 px and a long edge of at least 512 px.")
    }

    val orientation = if (sourceType == "image/jpeg") {
        runCatching {
            ExifInterface(ByteArrayInputStream(source)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    } else {
        ExifInterface.ORIENTATION_NORMAL
    }
    val orientationTransformNeeded = orientation != ExifInterface.ORIENTATION_NORMAL &&
        orientation != ExifInterface.ORIENTATION_UNDEFINED
    val resizeNeeded = maxOf(bounds.outWidth, bounds.outHeight) > MAX_IMAGE_EDGE
    val encodeNeeded = source.size > MAX_UPLOAD_BYTES

    if (!orientationTransformNeeded && !resizeNeeded && !encodeNeeded) {
        return source to sourceType
    }

    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_IMAGE_EDGE) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(
        source,
        0,
        source.size,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    ) ?: throw IllegalArgumentException("The image could not be decoded safely.")
    var working = orientBitmap(decoded, orientation)
    if (working !== decoded) decoded.recycle()
    if (maxOf(working.width, working.height) > MAX_IMAGE_EDGE) {
        val scaled = scaleLongEdge(working, MAX_IMAGE_EDGE)
        if (scaled !== working) working.recycle()
        working = scaled
    }
    if (!hasMinimumApiGeometry(working.width, working.height)) {
        working.recycle()
        throw IllegalArgumentException("The oriented image needs a short edge of at least 384 px and a long edge of at least 512 px.")
    }

    return try {
        if (!jpegOnly && sourceType == "image/png") {
            val png = compress(working, Bitmap.CompressFormat.PNG, 100)
            if (png.size <= MAX_UPLOAD_BYTES) return png to "image/png"
        }
        val opaque = if (working.hasAlpha()) flattenOnWhite(working) else working
        if (opaque !== working) working.recycle()
        try {
            compressJpegUnderLimit(opaque) to "image/jpeg"
        } finally {
            opaque.recycle()
        }
    } finally {
        if (!working.isRecycled) working.recycle()
    }
}

private fun orientBitmap(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return source
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun scaleLongEdge(source: Bitmap, maxEdge: Int): Bitmap {
    val currentEdge = maxOf(source.width, source.height)
    if (currentEdge <= maxEdge) return source
    val factor = maxEdge.toFloat() / currentEdge.toFloat()
    return Bitmap.createScaledBitmap(
        source,
        (source.width * factor).toInt().coerceAtLeast(1),
        (source.height * factor).toInt().coerceAtLeast(1),
        true,
    )
}

private fun flattenOnWhite(source: Bitmap): Bitmap =
    Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).also { output ->
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(source, 0f, 0f, null)
    }

private fun compressJpegUnderLimit(source: Bitmap): ByteArray {
    var working = source
    val longEdges = listOf(MAX_IMAGE_EDGE, 3_600, 3_200, 2_800)
    try {
        for (edge in longEdges) {
            val resized = scaleLongEdge(working, edge)
            if (resized !== working) {
                if (working !== source) working.recycle()
                working = resized
            }
            for (quality in listOf(95, 92, 88, 84, 80)) {
                val bytes = compress(working, Bitmap.CompressFormat.JPEG, quality)
                if (bytes.size <= MAX_UPLOAD_BYTES) return bytes
            }
        }
        throw IllegalArgumentException("The image could not fit the secure 10 MB upload limit without excessive compression.")
    } finally {
        if (working !== source) working.recycle()
    }
}

private fun compress(source: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray =
    ByteArrayOutputStream().use { output ->
        if (!source.compress(format, quality, output)) {
            throw IllegalArgumentException("The image could not be encoded.")
        }
        output.toByteArray()
    }

private fun sniffImageType(bytes: ByteArray): String? = when {
    bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
    bytes.size >= 8 && bytes.take(8).map(Byte::toInt) ==
        listOf(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).map { it.toByte().toInt() } -> "image/png"
    else -> null
}

private fun sampleSize(width: Int, height: Int, targetEdge: Int): Int {
    var value = 1
    while (width / value > targetEdge * 2 || height / value > targetEdge * 2) value *= 2
    return value
}

private fun hasMinimumApiGeometry(width: Int, height: Int): Boolean =
    minOf(width, height) >= MIN_SHORT_EDGE && maxOf(width, height) >= MIN_LONG_EDGE

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024 - 1
private const val MAX_SOURCE_BYTES = 40 * 1024 * 1024
private const val MAX_IMAGE_EDGE = 4_096
private const val MIN_SHORT_EDGE = 384
private const val MIN_LONG_EDGE = 512
private const val MAX_RESULT_BYTES = 20L * 1024L * 1024L
private const val PREVIEW_EDGE = 720
private const val RESULT_PREVIEW_EDGE = 1_200
