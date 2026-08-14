package com.drapeproof.mobile.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.core.color.LabColor
import com.drapeproof.core.color.SrgbColor
import com.drapeproof.core.domain.FaceColorObservation
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One camera observation with beard-resilient skin sampling and real-time capture quality. */
data class FrameReading(
    val face: FaceColorObservation?,
    val fabric: LabColor?,
    val skinSrgb: SrgbColor?,
    val fabricSrgb: SrgbColor?,
    val yawDegrees: Double,
    val pitchDegrees: Double,
    val rollDegrees: Double,
    val faceScale: Double,
    val clippedPixelFraction: Double,
    val fabricClippedPixelFraction: Double,
    val faceLuminance: Double,
    val sharpEnough: Boolean,
    val neutralExpression: Boolean,
    val eyesOpen: Boolean,
    val occlusionFree: Boolean,
    val fabricRegionValid: Boolean,
    val chinX: Float = 0.5f,
    val chinY: Float = 0.60f,
    val leftJawX: Float = 0.30f,
    val rightJawX: Float = 0.70f,
    val captureConfidencePercent: Int = 94,
    val lightingStatusLabel: String = "Good Lighting",
    val timestampNanos: Long,
) {
    val hasFace: Boolean get() = face != null
    val basicCaptureReady: Boolean
        get() = hasFace && sharpEnough && neutralExpression && eyesOpen && occlusionFree
}

/**
 * MediaPipe runs completely on-device. The analyzer samples raw camera pixels,
 * utilizing upper-facial hair-immune zones for beard & stubble resilience.
 */
class FaceFrameAnalyzer(
    context: Context,
    private val onReading: (FrameReading) -> Unit,
    private val onFailure: (String) -> Unit,
) : ImageAnalysis.Analyzer, AutoCloseable {
    private val busy = AtomicBoolean(false)
    private val faceLandmarker = FaceLandmarker.createFromOptions(
        context,
        FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath("face_landmarker.task")
                    .build(),
            )
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .setMinFaceDetectionConfidence(0.65f)
            .setMinFacePresenceConfidence(0.65f)
            .setMinTrackingConfidence(0.65f)
            .build(),
    )

    override fun analyze(image: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        try {
            val source = image.toBitmap()
            val oriented = source.oriented(image.imageInfo.rotationDegrees)
            val mpImage = BitmapImageBuilder(oriented).build()
            try {
                val result = faceLandmarker.detect(mpImage)
                onReading(analyzeFrame(oriented, result, image.imageInfo.timestamp))
            } finally {
                mpImage.close()
                if (oriented !== source) oriented.recycle()
                source.recycle()
            }
        } catch (error: Throwable) {
            onFailure(error.message ?: "On-device face analysis failed")
        } finally {
            busy.set(false)
            image.close()
        }
    }

    override fun close() = faceLandmarker.close()

    private fun analyzeFrame(bitmap: Bitmap, result: FaceLandmarkerResult, timestamp: Long): FrameReading {
        val landmarks = result.faceLandmarks().firstOrNull()
        if (landmarks == null || landmarks.size < 455) {
            return FrameReading(
                face = null,
                fabric = null,
                skinSrgb = null,
                fabricSrgb = null,
                yawDegrees = 0.0,
                pitchDegrees = 0.0,
                rollDegrees = 0.0,
                faceScale = 0.0,
                clippedPixelFraction = 1.0,
                fabricClippedPixelFraction = 1.0,
                faceLuminance = 0.0,
                sharpEnough = false,
                neutralExpression = false,
                eyesOpen = false,
                occlusionFree = false,
                fabricRegionValid = false,
                captureConfidencePercent = 20,
                lightingStatusLabel = "Align Face in Oval",
                timestampNanos = timestamp,
            )
        }

        fun point(index: Int): Pair<Double, Double> =
            landmarks[index].x().toDouble() to landmarks[index].y().toDouble()

        val xs = landmarks.map { it.x().toDouble() }
        val ys = landmarks.map { it.y().toDouble() }
        val minX = xs.minOrNull()!!.coerceIn(0.0, 1.0)
        val maxX = xs.maxOrNull()!!.coerceIn(0.0, 1.0)
        val minY = ys.minOrNull()!!.coerceIn(0.0, 1.0)
        val maxY = ys.maxOrNull()!!.coerceIn(0.0, 1.0)

        // 1. BEARD & FACIAL HAIR-RESILIENT MULTI-ZONE SKIN SAMPLING
        // Sample exclusively from upper hair-free zones:
        val forehead1 = bitmap.samplePatchStats(point(10), 0.016)
        val forehead2 = bitmap.samplePatchStats(point(151), 0.016)
        val forehead3 = bitmap.samplePatchStats(point(9), 0.016)
        val leftHighCheek = bitmap.samplePatchStats(point(118), 0.015) // Sub-orbital, above beard line
        val rightHighCheek = bitmap.samplePatchStats(point(347), 0.015) // Sub-orbital, above beard line
        val nasalBridge = bitmap.samplePatchStats(point(6), 0.012)

        val candidatePatches = listOf(forehead1, forehead2, forehead3, leftHighCheek, rightHighCheek, nasalBridge)

        // Texture variance check: discard patches with high micro-contrast (hair, stubble, fabric edges)
        val smoothPatches = candidatePatches.filter { it.color != null && it.channelDeviation < 34.0 }

        // Luminance outlier trimming: discard patches significantly darker than forehead reference
        val refLuminance = (forehead1.color ?: forehead2.color)?.relativeLuminance() ?: 0.50
        val validSkinPatches = smoothPatches.mapNotNull { it.color }.filter { color ->
            abs(color.relativeLuminance() - refLuminance) < 0.22 && isPlausibleHumanSkin(color)
        }

        val skin = if (validSkinPatches.isNotEmpty()) {
            medianColor(validSkinPatches)
        } else {
            smoothPatches.mapNotNull { it.color }.firstOrNull()?.takeIf { isPlausibleHumanSkin(it) }
        }

        val cheek = skin
        val underChin = bitmap.samplePatchStats(point(152).let { it.first to (it.second - 0.035) }, 0.014).color

        val eye = if (landmarks.size > 473) {
            medianColor(
                listOfNotNull(
                    bitmap.samplePatchStats(point(468), 0.0045).color,
                    bitmap.samplePatchStats(point(473), 0.0045).color,
                ),
            )
        } else {
            null
        }
        val eyebrow = medianColor(
            listOfNotNull(
                bitmap.samplePatchStats(point(105), 0.010).color,
                bitmap.samplePatchStats(point(334), 0.010).color,
            ),
        )
        val lip = medianColor(
            listOfNotNull(
                bitmap.samplePatchStats(point(13), 0.011).color,
                bitmap.samplePatchStats(point(14), 0.011).color,
            ),
        )

        val chinY = point(152).second
        val fabricStartY = max(0.70, chinY + 0.055).coerceAtMost(0.86)
        val fabricStats = bitmap.sampleRegion(
            left = max(0.10, minX - 0.08),
            top = fabricStartY,
            right = min(0.90, maxX + 0.08),
            bottom = 0.94,
        )

        val leftEyeOuter = point(33)
        val rightEyeOuter = point(263)
        val roll = Math.toDegrees(
            atan2(rightEyeOuter.second - leftEyeOuter.second, rightEyeOuter.first - leftEyeOuter.first),
        )
        val nose = point(1)
        val faceCenterX = (minX + maxX) / 2.0
        val yaw = ((nose.first - faceCenterX) / max(0.001, maxX - minX) * 70.0).coerceIn(-35.0, 35.0)
        val verticalRatio = (nose.second - minY) / max(0.001, maxY - minY)
        val pitch = ((verticalRatio - 0.53) * 75.0).coerceIn(-35.0, 35.0)

        val leftEyeOpen = eyeAspectRatio(landmarks, 33, 133, 159, 145)
        val rightEyeOpen = eyeAspectRatio(landmarks, 362, 263, 386, 374)
        val mouthWidth = normalizedDistance(point(61), point(291))
        val mouthOpen = normalizedDistance(point(13), point(14)) / max(0.001, mouthWidth)

        // 2. CAPTURE QUALITY & LIGHTING CONFIDENCE ENGINE
        val faceLum = skin?.relativeLuminance() ?: 0.0
        val exposureScore = when {
            faceLum in 0.28..0.78 -> 1.0
            faceLum in 0.18..0.88 -> 0.75
            else -> 0.35
        }

        val skinLab = skin?.let { ColorConversions.srgbToLab(it) }
        val colorCastScore = when {
            skinLab != null && skinLab.b > 32.0 -> 0.60 // Heavy warm cast
            skinLab != null && skinLab.a < -8.0 -> 0.60 // Fluorescent green cast
            else -> 1.0
        }

        val framingDist = hypot(faceCenterX - 0.50, ((minY + maxY) / 2.0) - 0.38)
        val framingScore = (1.0 - framingDist * 2.0).coerceIn(0.2, 1.0)
        val visibilityScore = if (skin != null && eye != null) 1.0 else 0.50

        val captureConfidence = (0.35 * exposureScore + 0.25 * colorCastScore + 0.25 * visibilityScore + 0.15 * framingScore) * 100.0
        val confidenceInt = captureConfidence.toInt().coerceIn(30, 98)

        val lightingLabel = when {
            faceLum < 0.20 -> "⚠ Low Light: Face a window"
            faceLum > 0.85 -> "⚠ Harsh Light: Avoid direct glare"
            colorCastScore < 0.70 -> "⚠ Warm Indoor Light: Move to natural light"
            confidenceInt >= 85 -> "Lighting: ●●●●○ Good ($confidenceInt%)"
            else -> "Align Face in Center"
        }

        val faceObservation = skin?.let {
            FaceColorObservation(
                skin = ColorConversions.srgbToLab(it),
                eye = eye?.let(ColorConversions::srgbToLab),
                eyebrow = eyebrow?.let(ColorConversions::srgbToLab),
                lip = lip?.let(ColorConversions::srgbToLab),
                cheek = cheek?.let(ColorConversions::srgbToLab),
                underChin = underChin?.let(ColorConversions::srgbToLab),
            )
        }

        return FrameReading(
            face = faceObservation,
            fabric = fabricStats.color?.let(ColorConversions::srgbToLab),
            skinSrgb = skin,
            fabricSrgb = fabricStats.color,
            yawDegrees = yaw,
            pitchDegrees = pitch,
            rollDegrees = roll,
            faceScale = sqrt(max(0.0, (maxX - minX) * (maxY - minY))),
            clippedPixelFraction = weightedClippedFraction(forehead1, leftHighCheek),
            fabricClippedPixelFraction = fabricStats.clippedFraction,
            faceLuminance = faceLum,
            sharpEnough = bitmap.gradientEnergy(minX, minY, maxX, maxY) >= 6.0,
            neutralExpression = mouthOpen < 0.20,
            eyesOpen = leftEyeOpen > 0.10 && rightEyeOpen > 0.10,
            occlusionFree = skin != null && eye != null && eyebrow != null && lip != null,
            fabricRegionValid = fabricStats.samples >= 80 && fabricStats.channelDeviation <= 42.0,
            chinX = point(152).first.toFloat(),
            chinY = point(152).second.toFloat(),
            leftJawX = point(234).first.toFloat(),
            rightJawX = point(454).first.toFloat(),
            captureConfidencePercent = confidenceInt,
            lightingStatusLabel = lightingLabel,
            timestampNanos = timestamp,
        )
    }
}

private data class RegionStats(
    val color: SrgbColor?,
    val samples: Int,
    val clippedSamples: Int,
    val channelDeviation: Double,
) {
    val clippedFraction: Double
        get() = if (samples == 0) 1.0 else clippedSamples.toDouble() / samples
}

private fun Bitmap.samplePatchStats(center: Pair<Double, Double>, radiusFraction: Double): RegionStats {
    val cx = (center.first * width).roundToInt()
    val cy = (center.second * height).roundToInt()
    val r = max(2, (radiusFraction * min(width, height)).roundToInt())
    val left = max(0, cx - r)
    val right = min(width - 1, cx + r)
    val top = max(0, cy - r)
    val bottom = min(height - 1, cy + r)

    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0
    var clipped = 0
    val pixels = IntArray((right - left + 1) * (bottom - top + 1))
    getPixels(pixels, 0, right - left + 1, left, top, right - left + 1, bottom - top + 1)

    for (pixel in pixels) {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        sumR += red
        sumG += green
        sumB += blue
        count++
        if (red <= 3 || red >= 252 || green <= 3 || green >= 252 || blue <= 3 || blue >= 252) {
            clipped++
        }
    }
    if (count == 0) return RegionStats(null, 0, 0, 0.0)

    val avgR = (sumR / count).toInt()
    val avgG = (sumG / count).toInt()
    val avgB = (sumB / count).toInt()

    var varSum = 0.0
    for (pixel in pixels) {
        val dr = Color.red(pixel) - avgR
        val dg = Color.green(pixel) - avgG
        val db = Color.blue(pixel) - avgB
        varSum += (dr * dr + dg * dg + db * db) / 3.0
    }
    val dev = sqrt(varSum / count)

    return RegionStats(SrgbColor(avgR, avgG, avgB), count, clipped, dev)
}

private fun Bitmap.sampleRegion(left: Double, top: Double, right: Double, bottom: Double): RegionStats {
    val x0 = (left * width).roundToInt().coerceIn(0, width - 1)
    val y0 = (top * height).roundToInt().coerceIn(0, height - 1)
    val x1 = (right * width).roundToInt().coerceIn(x0, width - 1)
    val y1 = (bottom * height).roundToInt().coerceIn(y0, height - 1)
    val w = x1 - x0 + 1
    val h = y1 - y0 + 1
    val pixels = IntArray(w * h)
    getPixels(pixels, 0, w, x0, y0, w, h)

    var sumR = 0L
    var sumG = 0L
    var sumB = 0L
    var count = 0
    var clipped = 0
    for (pixel in pixels) {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        sumR += red
        sumG += green
        sumB += blue
        count++
        if (red <= 3 || red >= 252 || green <= 3 || green >= 252 || blue <= 3 || blue >= 252) {
            clipped++
        }
    }
    if (count == 0) return RegionStats(null, 0, 0, 0.0)
    val avgR = (sumR / count).toInt()
    val avgG = (sumG / count).toInt()
    val avgB = (sumB / count).toInt()

    var varSum = 0.0
    for (pixel in pixels) {
        val dr = Color.red(pixel) - avgR
        val dg = Color.green(pixel) - avgG
        val db = Color.blue(pixel) - avgB
        varSum += (dr * dr + dg * dg + db * db) / 3.0
    }
    val dev = sqrt(varSum / count)

    return RegionStats(SrgbColor(avgR, avgG, avgB), count, clipped, dev)
}

private fun SrgbColor.relativeLuminance(): Double =
    (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0

private fun isPlausibleHumanSkin(srgb: SrgbColor?): Boolean {
    if (srgb == null) return false
    val total = (srgb.red + srgb.green + srgb.blue).toDouble()
    if (total <= 15.0 || total >= 750.0) return false

    val rNorm = srgb.red / total
    val gNorm = srgb.green / total
    val bNorm = srgb.blue / total

    return rNorm in 0.30..0.72 && gNorm in 0.20..0.48 && bNorm in 0.08..0.42 && srgb.red >= srgb.blue
}

private fun medianColor(colors: List<SrgbColor>): SrgbColor? {
    if (colors.isEmpty()) return null
    val rs = colors.map { it.red }.sorted()
    val gs = colors.map { it.green }.sorted()
    val bs = colors.map { it.blue }.sorted()
    val mid = colors.size / 2
    return SrgbColor(rs[mid], gs[mid], bs[mid])
}

private fun eyeAspectRatio(landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>, p1: Int, p2: Int, p3: Int, p4: Int): Double {
    fun p(i: Int) = landmarks[i].x().toDouble() to landmarks[i].y().toDouble()
    val width = normalizedDistance(p(p1), p(p2))
    val height = normalizedDistance(p(p3), p(p4))
    return if (width <= 0.001) 0.0 else height / width
}

private fun normalizedDistance(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val dx = a.first - b.first
    val dy = a.second - b.second
    return sqrt(dx * dx + dy * dy)
}

private fun weightedClippedFraction(vararg stats: RegionStats): Double {
    var totalSamples = 0
    var totalClipped = 0
    for (s in stats) {
        totalSamples += s.samples
        totalClipped += s.clippedSamples
    }
    return if (totalSamples == 0) 1.0 else totalClipped.toDouble() / totalSamples
}

private fun Bitmap.gradientEnergy(minX: Double, minY: Double, maxX: Double, maxY: Double): Double {
    val x0 = (minX * width).roundToInt().coerceIn(0, width - 2)
    val y0 = (minY * height).roundToInt().coerceIn(0, height - 2)
    val x1 = (maxX * width).roundToInt().coerceIn(x0 + 1, width - 1)
    val y1 = (maxY * height).roundToInt().coerceIn(y0 + 1, height - 1)

    var sum = 0.0
    var samples = 0
    val stepX = max(1, (x1 - x0) / 20)
    val stepY = max(1, (y1 - y0) / 20)

    for (y in y0 until y1 step stepY) {
        for (x in x0 until x1 step stepX) {
            val c = getPixel(x, y)
            val cRight = getPixel(x + 1, y)
            val cDown = getPixel(x, y + 1)
            val lum = Color.red(c) * 0.299 + Color.green(c) * 0.587 + Color.blue(c) * 0.114
            val lumR = Color.red(cRight) * 0.299 + Color.green(cRight) * 0.587 + Color.blue(cRight) * 0.114
            val lumD = Color.red(cDown) * 0.299 + Color.green(cDown) * 0.587 + Color.blue(cDown) * 0.114
            val dx = lumR - lum
            val dy = lumD - lum
            sum += sqrt(dx * dx + dy * dy)
            samples++
        }
    }
    return if (samples == 0) 0.0 else sum / samples
}

private fun Bitmap.oriented(rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return this
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
