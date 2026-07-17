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

/** One camera observation with no DrapeProof color or beauty transform applied. */
data class FrameReading(
    val face: FaceColorObservation?,
    val fabric: LabColor?,
    val skinSrgb: SrgbColor?,
    val fabricSrgb: SrgbColor?,
    val yawDegrees: Double,
    val pitchDegrees: Double,
    val rollDegrees: Double,
    val faceScale: Double,
    /** Clipping inside the two cheek patches used as the skin-color anchor. */
    val clippedPixelFraction: Double,
    /** Clipping inside the below-chin fabric patch. Evaluated only for drape readings. */
    val fabricClippedPixelFraction: Double,
    val faceLuminance: Double,
    val sharpEnough: Boolean,
    val neutralExpression: Boolean,
    val eyesOpen: Boolean,
    val occlusionFree: Boolean,
    val fabricRegionValid: Boolean,
    val timestampNanos: Long,
) {
    val hasFace: Boolean get() = face != null
    val basicCaptureReady: Boolean
        get() = hasFace && sharpEnough && neutralExpression && eyesOpen && occlusionFree
}

/**
 * MediaPipe runs completely on-device. The analyzer deliberately samples the original camera
 * bitmap rather than a preview screenshot, preserving the camera's recorded pixel values.
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

        val leftCheekStats = bitmap.samplePatchStats(point(117), 0.018)
        val rightCheekStats = bitmap.samplePatchStats(point(346), 0.018)
        val leftCheek = leftCheekStats.color
        val rightCheek = rightCheekStats.color
        val skin = medianColor(listOfNotNull(leftCheek, rightCheek))
        val cheek = skin
        val underChin = bitmap.samplePatchStats(point(152).let { it.first to (it.second - 0.035) }, 0.014).color
        // MediaPipe Face Landmarker iris centers. Upper-eyelid landmarks are not a valid proxy
        // for eye color, so a model without iris landmarks yields no eye component.
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
            clippedPixelFraction = weightedClippedFraction(leftCheekStats, rightCheekStats),
            fabricClippedPixelFraction = fabricStats.clippedFraction,
            faceLuminance = skin?.relativeLuminance() ?: 0.0,
            sharpEnough = bitmap.gradientEnergy(minX, minY, maxX, maxY) >= 6.0,
            neutralExpression = mouthOpen < 0.20,
            eyesOpen = leftEyeOpen > 0.10 && rightEyeOpen > 0.10,
            occlusionFree = skin != null && eye != null && eyebrow != null && lip != null,
            fabricRegionValid = fabricStats.samples >= 80 && fabricStats.channelDeviation <= 42.0,
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

private fun Bitmap.oriented(rotationDegrees: Int): Bitmap {
    if (rotationDegrees % 360 == 0) return this
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

private fun Bitmap.samplePatchStats(center: Pair<Double, Double>, radiusFraction: Double): RegionStats {
    val cx = (center.first * width).roundToInt()
    val cy = (center.second * height).roundToInt()
    val radius = max(2, (min(width, height) * radiusFraction).roundToInt())
    return sampleRegionPixels(cx - radius, cy - radius, cx + radius, cy + radius)
}

private fun Bitmap.sampleRegion(left: Double, top: Double, right: Double, bottom: Double): RegionStats =
    sampleRegionPixels(
        (left * width).roundToInt(),
        (top * height).roundToInt(),
        (right * width).roundToInt(),
        (bottom * height).roundToInt(),
    )

private fun Bitmap.sampleRegionPixels(left: Int, top: Int, right: Int, bottom: Int): RegionStats {
    val safeLeft = left.coerceIn(0, width - 1)
    val safeTop = top.coerceIn(0, height - 1)
    val safeRight = right.coerceIn(safeLeft + 1, width)
    val safeBottom = bottom.coerceIn(safeTop + 1, height)
    val span = max(1, max(safeRight - safeLeft, safeBottom - safeTop))
    val step = max(1, span / 28)
    val reds = mutableListOf<Int>()
    val greens = mutableListOf<Int>()
    val blues = mutableListOf<Int>()
    var clippedSamples = 0
    for (y in safeTop until safeBottom step step) {
        for (x in safeLeft until safeRight step step) {
            val pixel = getPixel(x, y)
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)
            reds += red
            greens += green
            blues += blue
            if (min(red, min(green, blue)) <= 2 || max(red, max(green, blue)) >= 253) {
                clippedSamples++
            }
        }
    }
    if (reds.isEmpty()) return RegionStats(null, 0, 0, Double.POSITIVE_INFINITY)
    val color = SrgbColor(reds.median(), greens.median(), blues.median())
    val deviation = (reds.meanAbsoluteDeviation() + greens.meanAbsoluteDeviation() + blues.meanAbsoluteDeviation()) / 3.0
    return RegionStats(color, reds.size, clippedSamples, deviation)
}

private fun weightedClippedFraction(vararg regions: RegionStats): Double {
    val samples = regions.sumOf(RegionStats::samples)
    return if (samples == 0) 1.0 else regions.sumOf(RegionStats::clippedSamples).toDouble() / samples
}

private fun Bitmap.gradientEnergy(left: Double, top: Double, right: Double, bottom: Double): Double {
    val x0 = (left * width).roundToInt().coerceIn(2, width - 3)
    val x1 = (right * width).roundToInt().coerceIn(x0 + 1, width - 2)
    val y0 = (top * height).roundToInt().coerceIn(2, height - 3)
    val y1 = (bottom * height).roundToInt().coerceIn(y0 + 1, height - 2)
    val step = max(2, min(width, height) / 80)
    var total = 0.0
    var count = 0
    for (y in y0 until y1 step step) {
        for (x in x0 until x1 step step) {
            val gx = abs(luma(getPixel(x + 1, y)) - luma(getPixel(x - 1, y)))
            val gy = abs(luma(getPixel(x, y + 1)) - luma(getPixel(x, y - 1)))
            total += gx + gy
            count++
        }
    }
    return if (count == 0) 0.0 else total / count
}

private fun eyeAspectRatio(
    points: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    outer: Int,
    inner: Int,
    upper: Int,
    lower: Int,
): Double {
    fun p(index: Int) = points[index].x().toDouble() to points[index].y().toDouble()
    return normalizedDistance(p(upper), p(lower)) / max(0.001, normalizedDistance(p(outer), p(inner)))
}

private fun normalizedDistance(first: Pair<Double, Double>, second: Pair<Double, Double>): Double =
    hypot(first.first - second.first, first.second - second.second)

private fun medianColor(colors: List<SrgbColor>): SrgbColor? = if (colors.isEmpty()) null else SrgbColor(
    colors.map(SrgbColor::red).median(),
    colors.map(SrgbColor::green).median(),
    colors.map(SrgbColor::blue).median(),
)

private fun List<Int>.median(): Int = sorted()[size / 2]

private fun List<Int>.meanAbsoluteDeviation(): Double {
    val center = median()
    return sumOf { abs(it - center).toDouble() } / size
}

private fun SrgbColor.relativeLuminance(): Double =
    (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255.0

private fun luma(pixel: Int): Double =
    0.2126 * Color.red(pixel) + 0.7152 * Color.green(pixel) + 0.0722 * Color.blue(pixel)

fun medianLab(values: List<LabColor>): LabColor {
    require(values.isNotEmpty())
    fun median(component: (LabColor) -> Double): Double {
        val sorted = values.map(component).sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }
    return LabColor(median(LabColor::l), median(LabColor::a), median(LabColor::b))
}

fun aggregateFace(readings: List<FrameReading>): FaceColorObservation? {
    val observations = readings.mapNotNull(FrameReading::face)
    if (observations.isEmpty()) return null
    fun aggregate(selector: (FaceColorObservation) -> LabColor?): LabColor? =
        observations.mapNotNull(selector).takeIf(List<*>::isNotEmpty)?.let(::medianLab)
    return FaceColorObservation(
        skin = medianLab(observations.map(FaceColorObservation::skin)),
        eye = aggregate(FaceColorObservation::eye),
        eyebrow = aggregate(FaceColorObservation::eyebrow),
        lip = aggregate(FaceColorObservation::lip),
        cheek = aggregate(FaceColorObservation::cheek),
        underChin = aggregate(FaceColorObservation::underChin),
    )
}
