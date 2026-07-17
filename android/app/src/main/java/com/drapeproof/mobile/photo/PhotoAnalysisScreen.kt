package com.drapeproof.mobile.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.get
import androidx.exifinterface.media.ExifInterface
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.core.color.SrgbColor
import com.drapeproof.core.domain.ContrastCalculator
import com.drapeproof.core.domain.ContrastVector
import com.drapeproof.core.domain.EvidenceInputs
import com.drapeproof.core.domain.EvidencePolicy
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.core.domain.FaceColorObservation
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.data.LocalDrapeRecord
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.data.displayName
import com.drapeproof.mobile.ui.ScreenHeader
import com.drapeproof.mobile.ui.theme.Cobalt
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import java.util.Locale

private enum class PhotoMode { SAME_SCENE, SEPARATE_PHOTOS }
private enum class SampleTarget { SKIN, FABRIC }
private data class PixelPoint(val x: Int, val y: Int)
private data class LoadedPhoto(val uri: Uri, val bitmap: Bitmap)
private data class PhotoResult(
    val skin: SrgbColor,
    val fabric: SrgbColor,
    val vector: ContrastVector,
    val evidence: EvidenceTier,
)

@Composable
fun PhotoAnalysisScreen(
    initialFabricUri: Uri?,
    onInitialUriConsumed: () -> Unit,
    onBack: () -> Unit,
    onSeeCatalog: () -> Unit,
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(PhotoMode.SAME_SCENE) }
    var sceneUri by remember { mutableStateOf<Uri?>(null) }
    var faceUri by remember { mutableStateOf<Uri?>(null) }
    var fabricUri by remember { mutableStateOf<Uri?>(null) }
    var skinPoint by remember { mutableStateOf<PixelPoint?>(null) }
    var fabricPoint by remember { mutableStateOf<PixelPoint?>(null) }
    var target by remember { mutableStateOf(SampleTarget.SKIN) }
    var result by remember { mutableStateOf<PhotoResult?>(null) }
    var saved by remember { mutableStateOf(false) }

    val scenePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            sceneUri = uri
            skinPoint = null
            fabricPoint = null
            result = null
        }
    }
    val facePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            faceUri = uri
            skinPoint = null
            result = null
        }
    }
    val fabricPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            fabricUri = uri
            fabricPoint = null
            result = null
        }
    }

    LaunchedEffect(initialFabricUri) {
        if (initialFabricUri != null) {
            mode = PhotoMode.SEPARATE_PHOTOS
            fabricUri = initialFabricUri
            fabricPoint = null
            result = null
            onInitialUriConsumed()
        }
    }

    val scenePhoto by rememberLoadedPhoto(sceneUri)
    val facePhoto by rememberLoadedPhoto(faceUri)
    val fabricPhoto by rememberLoadedPhoto(fabricUri)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Photo contrast",
            evidence = result?.evidence?.displayName()?.uppercase(),
            onBack = onBack,
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
        ) {
            Text(
                "Measure what the pixels support.",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a clear cheek area and the exact fabric color. A small median patch is sampled locally; your images are not uploaded for this measurement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == PhotoMode.SAME_SCENE,
                    onClick = {
                        mode = PhotoMode.SAME_SCENE
                        skinPoint = null
                        fabricPoint = null
                        result = null
                    },
                    label = { Text("One same-scene photo") },
                )
                FilterChip(
                    selected = mode == PhotoMode.SEPARATE_PHOTOS,
                    onClick = {
                        mode = PhotoMode.SEPARATE_PHOTOS
                        skinPoint = null
                        fabricPoint = null
                        result = null
                    },
                    label = { Text("Two separate photos") },
                )
            }

            Spacer(Modifier.height(12.dp))
            EvidenceExplanation(mode)
            Spacer(Modifier.height(14.dp))

            if (mode == PhotoMode.SAME_SCENE) {
                PickPhotoButton(
                    label = if (scenePhoto == null) "Choose face + fabric photo" else "Replace same-scene photo",
                    onClick = { scenePicker.launch("image/*") },
                )
                scenePhoto?.let { photo ->
                    Spacer(Modifier.height(12.dp))
                    TargetSelector(target) { target = it }
                    Spacer(Modifier.height(8.dp))
                    TappablePhoto(
                        photo = photo,
                        skinPoint = skinPoint,
                        fabricPoint = fabricPoint,
                        activeTarget = target,
                        onSample = { selectedTarget, point ->
                            if (selectedTarget == SampleTarget.SKIN) {
                                skinPoint = point
                                target = SampleTarget.FABRIC
                            } else {
                                fabricPoint = point
                            }
                            result = null
                            saved = false
                        },
                    )
                }
            } else {
                PhotoSampleCard(
                    title = "1  FACE PHOTO",
                    guidance = "Use neutral light. Tap visible cheek skin; avoid glare, makeup and deep shadow.",
                    button = if (facePhoto == null) "Choose face photo" else "Replace face photo",
                    photo = facePhoto,
                    target = SampleTarget.SKIN,
                    point = skinPoint,
                    onPick = { facePicker.launch("image/*") },
                    onSample = { skinPoint = it; result = null; saved = false },
                )
                Spacer(Modifier.height(14.dp))
                PhotoSampleCard(
                    title = "2  FABRIC OR DRESS PHOTO",
                    guidance = "Tap a flat, matte area of the exact colorway; avoid folds, print, glare and background.",
                    button = if (fabricPhoto == null) "Choose fabric / dress photo" else "Replace product photo",
                    photo = fabricPhoto,
                    target = SampleTarget.FABRIC,
                    point = fabricPoint,
                    onPick = { fabricPicker.launch("image/*") },
                    onSample = { fabricPoint = it; result = null; saved = false },
                )
            }

            Spacer(Modifier.height(18.dp))
            val sourceBitmaps = if (mode == PhotoMode.SAME_SCENE) {
                scenePhoto?.let { it.bitmap to it.bitmap }
            } else {
                if (facePhoto != null && fabricPhoto != null) facePhoto!!.bitmap to fabricPhoto!!.bitmap else null
            }
            Button(
                onClick = {
                    val skin = samplePatch(sourceBitmaps!!.first, skinPoint!!)
                    val fabric = samplePatch(sourceBitmaps.second, fabricPoint!!)
                    val evidence = EvidencePolicy.highestSupported(
                        EvidenceInputs(
                            hasOpeningAndClosingBaseline = false,
                            faceAndFabricInSameScene = mode == PhotoMode.SAME_SCENE,
                            hasSeparateFaceAndProductPhotos = mode == PhotoMode.SEPARATE_PHOTOS,
                            inputEligibleForMeasurement = true,
                        ),
                        quality = null,
                    )
                    val vector = ContrastCalculator.calculate(
                        baseline = null,
                        drape = FaceColorObservation(skin = ColorConversions.srgbToLab(skin)),
                        fabric = ColorConversions.srgbToLab(fabric),
                        allowApparentFaceShift = false,
                    )
                    result = PhotoResult(skin, fabric, vector, evidence)
                    SkinProfileRepository.save(
                        context,
                        StoredSkinProfile(
                            skinHex = skin.toHex(),
                            evidenceTier = evidence,
                            source = if (mode == PhotoMode.SAME_SCENE) "same-scene photo" else "separate face photo",
                            capturedAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                    saved = false
                },
                enabled = sourceBitmaps != null && skinPoint != null && fabricPoint != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text("Analyze sampled contrast")
            }

            result?.let { analysis ->
                Spacer(Modifier.height(18.dp))
                AnalysisResultCard(analysis)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            DrapeRecordRepository.add(
                                context,
                                analysis.toLocalRecord(mode),
                            )
                            saved = true
                        },
                        enabled = !saved,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (saved) "Saved" else "Save evidence") }
                    Button(onClick = onSeeCatalog, modifier = Modifier.weight(1f)) {
                        Text("Rank exact colors")
                    }
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun EvidenceExplanation(mode: PhotoMode) {
    val (title, detail) = if (mode == PhotoMode.SAME_SCENE) {
        "SAME-SCENE EVIDENCE" to "Stronger photo evidence because skin and cloth share one exposure and white balance. It is still below a controlled live pair."
    } else {
        "SEPARATE-PHOTO ESTIMATE" to "Different cameras, lighting, edits or white balance can move both colors. The app reports an estimate and never upgrades it to a real-life capture."
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(5.dp))
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PickPhotoButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(label) }
}

@Composable
private fun TargetSelector(selected: SampleTarget, onSelect: (SampleTarget) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected == SampleTarget.SKIN, { onSelect(SampleTarget.SKIN) }, label = { Text("Tap cheek") })
        FilterChip(selected == SampleTarget.FABRIC, { onSelect(SampleTarget.FABRIC) }, label = { Text("Tap fabric") })
    }
}

@Composable
private fun PhotoSampleCard(
    title: String,
    guidance: String,
    button: String,
    photo: LoadedPhoto?,
    target: SampleTarget,
    point: PixelPoint?,
    onPick: () -> Unit,
    onSample: (PixelPoint) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(5.dp))
            Text(guidance, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text(button) }
            photo?.let {
                Spacer(Modifier.height(8.dp))
                TappablePhoto(
                    photo = it,
                    skinPoint = if (target == SampleTarget.SKIN) point else null,
                    fabricPoint = if (target == SampleTarget.FABRIC) point else null,
                    activeTarget = target,
                    onSample = { _, selected -> onSample(selected) },
                )
            }
        }
    }
}

@Composable
private fun TappablePhoto(
    photo: LoadedPhoto,
    skinPoint: PixelPoint?,
    fabricPoint: PixelPoint?,
    activeTarget: SampleTarget,
    onSample: (SampleTarget, PixelPoint) -> Unit,
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val image = remember(photo.bitmap) { photo.bitmap.asImageBitmap() }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF252524), RoundedCornerShape(14.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(photo.bitmap, activeTarget, canvasSize) {
                detectTapGestures { offset: Offset ->
                    mapToBitmap(offset, canvasSize, photo.bitmap)?.let { onSample(activeTarget, it) }
                }
            },
    ) {
        val destination = fittedDestination(canvasSize, photo.bitmap)
        drawImage(
            image = image,
            dstOffset = IntOffset(destination.left.roundToInt(), destination.top.roundToInt()),
            dstSize = IntSize(destination.width.roundToInt(), destination.height.roundToInt()),
        )
        fun marker(point: PixelPoint?, color: Color) {
            if (point == null) return
            val x = destination.left + point.x * destination.scale
            val y = destination.top + point.y * destination.scale
            drawCircle(Color.White, radius = 13.dp.toPx(), center = Offset(x, y), style = Stroke(4.dp.toPx()))
            drawCircle(color, radius = 10.dp.toPx(), center = Offset(x, y), style = Stroke(4.dp.toPx()))
        }
        marker(skinPoint, DrapeCoral)
        marker(fabricPoint, Cobalt)
    }
}

private data class FittedDestination(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
)

private fun fittedDestination(size: IntSize, bitmap: Bitmap): FittedDestination {
    if (size.width == 0 || size.height == 0) return FittedDestination(0f, 0f, 0f, 0f, 1f)
    val scale = min(size.width.toFloat() / bitmap.width, size.height.toFloat() / bitmap.height)
    val width = bitmap.width * scale
    val height = bitmap.height * scale
    return FittedDestination((size.width - width) / 2f, (size.height - height) / 2f, width, height, scale)
}

private fun mapToBitmap(offset: Offset, size: IntSize, bitmap: Bitmap): PixelPoint? {
    val destination = fittedDestination(size, bitmap)
    if (offset.x !in destination.left..(destination.left + destination.width) ||
        offset.y !in destination.top..(destination.top + destination.height)
    ) return null
    return PixelPoint(
        x = ((offset.x - destination.left) / destination.scale).roundToInt().coerceIn(0, bitmap.width - 1),
        y = ((offset.y - destination.top) / destination.scale).roundToInt().coerceIn(0, bitmap.height - 1),
    )
}

@Composable
private fun AnalysisResultCard(result: PhotoResult) {
    val separation = result.vector.clothSkinSeparation
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(result.evidence.displayName().uppercase(), style = MaterialTheme.typography.labelSmall, color = Moss)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ColorSample("CHEEK", result.skin, Modifier.weight(1f))
                ColorSample("FABRIC", result.fabric, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            Text(String.format(Locale.US, "%.1f ΔE00", separation.deltaE00), style = MaterialTheme.typography.headlineLarge)
            Text(
                String.format(Locale.US, "Cloth–skin separation • ΔL* %+.1f", separation.deltaLStar),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (separation.lightnessDirection.name) {
                    "FABRIC_LIGHTER" -> "The sampled fabric is lighter than the sampled skin in this image."
                    "FABRIC_DARKER" -> "The sampled fabric is darker than the sampled skin in this image."
                    else -> "The two samples have similar lightness in this image."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            Text("Not claimed from this input", style = MaterialTheme.typography.labelSmall)
            Text(
                "Feature-definition change and apparent face shift need a controlled baseline/drape sequence. This result does not infer them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun ColorSample(label: String, color: SrgbColor, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .background(Color(color.red, color.green, color.blue), CircleShape),
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(color.toHex(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun PhotoResult.toLocalRecord(mode: PhotoMode): LocalDrapeRecord {
    val separation = vector.clothSkinSeparation
    return LocalDrapeRecord.create(
        source = if (mode == PhotoMode.SAME_SCENE) "same-scene photo sample" else "separate-photo sample",
        evidenceTier = evidence,
        intent = null,
        sku = "PHOTO-REFERENCE",
        variantId = fabric.toHex().removePrefix("#"),
        variantName = "Sampled fabric",
        skinHex = skin.toHex(),
        fabricHex = fabric.toHex(),
        separationDeltaE00 = separation.deltaE00,
        deltaLStar = separation.deltaLStar,
        limitations = listOf(
            "User-guided cheek and fabric patches; no material reflectance calibration.",
            if (mode == PhotoMode.SAME_SCENE) {
                "Same-scene image without a locked opening and closing baseline."
            } else {
                "Separate images may use different cameras, lighting, white balance or edits."
            },
        ),
    )
}

private fun samplePatch(bitmap: Bitmap, point: PixelPoint, radius: Int = 6): SrgbColor {
    val left = (point.x - radius).coerceAtLeast(0)
    val right = (point.x + radius).coerceAtMost(bitmap.width - 1)
    val top = (point.y - radius).coerceAtLeast(0)
    val bottom = (point.y + radius).coerceAtMost(bitmap.height - 1)
    val reds = ArrayList<Int>((right - left + 1) * (bottom - top + 1))
    val greens = ArrayList<Int>(reds.size)
    val blues = ArrayList<Int>(reds.size)
    for (y in top..bottom) {
        for (x in left..right) {
            val pixel = bitmap[x, y]
            reds += android.graphics.Color.red(pixel)
            greens += android.graphics.Color.green(pixel)
            blues += android.graphics.Color.blue(pixel)
        }
    }
    return SrgbColor(reds.median(), greens.median(), blues.median())
}

private fun List<Int>.median(): Int = sorted()[size / 2]

@Composable
private fun rememberLoadedPhoto(uri: Uri?): androidx.compose.runtime.State<LoadedPhoto?> {
    val context = LocalContext.current
    val state = remember(uri) { mutableStateOf<LoadedPhoto?>(null) }
    LaunchedEffect(uri) {
        state.value = uri?.let { selected ->
            runCatching { loadBitmap(context, selected) }.getOrNull()?.let { LoadedPhoto(selected, it) }
        }
    }
    return state
}

private suspend fun loadBitmap(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val maxSide = maxOf(info.size.width, info.size.height)
            if (maxSide > 1_600) {
                val scale = 1_600f / maxSide
                decoder.setTargetSize((info.size.width * scale).roundToInt(), (info.size.height * scale).roundToInt())
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1_600) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Unable to decode image")
        val rotation = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).rotationDegrees) {
                90 -> 90f
                180 -> 180f
                270 -> 270f
                else -> 0f
            }
        } ?: 0f
        if (rotation == 0f) bitmap else Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            Matrix().apply { postRotate(rotation) },
            true,
        )
    }
}
