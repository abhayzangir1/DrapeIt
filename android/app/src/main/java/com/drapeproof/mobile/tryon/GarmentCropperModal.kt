package com.drapeproof.mobile.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun GarmentCropperModal(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onCropped: (File) -> Unit,
) {
    val context = LocalContext.current

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Crop & Align Garment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(34.dp),
                    ) {
                        Text("Cancel", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Text(
                    "Pinch to zoom and align the garment inside the frame. Background will be cleaned for YouCam AI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
                )

                Spacer(Modifier.height(10.dp))

                // Interactive Viewport Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // Render image transformed
                    val imageBmp = remember(sourceBitmap) { sourceBitmap.asImageBitmap() }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val viewportW = size.width
                        val viewportH = size.height

                        // Calculate crop box (3:4 portrait aspect)
                        val cropBoxW = viewportW * 0.85f
                        val cropBoxH = cropBoxW * (4.0f / 3.0f).coerceAtMost(viewportH * 0.88f / cropBoxW)
                        val cropBoxLeft = (viewportW - cropBoxW) / 2
                        val cropBoxTop = (viewportH - cropBoxH) / 2

                        // Draw image with transformations & rotation
                        val cropCenter = Offset(cropBoxLeft + cropBoxW / 2f, cropBoxTop + cropBoxH / 2f)
                        rotate(degrees = rotationDegrees, pivot = cropCenter) {
                            drawImage(
                                image = imageBmp,
                                dstOffset = androidx.compose.ui.unit.IntOffset(
                                    (cropBoxLeft + offsetX).toInt(),
                                    (cropBoxTop + offsetY).toInt(),
                                ),
                                dstSize = androidx.compose.ui.unit.IntSize(
                                    (cropBoxW * scale).toInt(),
                                    (cropBoxW * scale * (sourceBitmap.height.toFloat() / sourceBitmap.width.toFloat())).toInt(),
                                ),
                            )
                        }

                        // Shaded Dimming Outside Crop Window
                        // Top rect
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, 0f),
                            size = Size(viewportW, cropBoxTop),
                        )
                        // Bottom rect
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, cropBoxTop + cropBoxH),
                            size = Size(viewportW, viewportH - (cropBoxTop + cropBoxH)),
                        )
                        // Left rect
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(0f, cropBoxTop),
                            size = Size(cropBoxLeft, cropBoxH),
                        )
                        // Right rect
                        drawRect(
                            color = Color.Black.copy(alpha = 0.65f),
                            topLeft = Offset(cropBoxLeft + cropBoxW, cropBoxTop),
                            size = Size(viewportW - (cropBoxLeft + cropBoxW), cropBoxH),
                        )

                        // White Crop Frame Border & Corner Accents
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(cropBoxLeft, cropBoxTop),
                            size = Size(cropBoxW, cropBoxH),
                            style = Stroke(width = 2.dp.toPx()),
                        )

                        // 3x3 Rule of Thirds Grid Lines
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropBoxLeft + cropBoxW / 3, cropBoxTop),
                            end = Offset(cropBoxLeft + cropBoxW / 3, cropBoxTop + cropBoxH),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropBoxLeft + (cropBoxW * 2) / 3, cropBoxTop),
                            end = Offset(cropBoxLeft + (cropBoxW * 2) / 3, cropBoxTop + cropBoxH),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropBoxLeft, cropBoxTop + cropBoxH / 3),
                            end = Offset(cropBoxLeft + cropBoxW, cropBoxTop + cropBoxH / 3),
                            strokeWidth = 1.dp.toPx(),
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.35f),
                            start = Offset(cropBoxLeft, cropBoxTop + (cropBoxH * 2) / 3),
                            end = Offset(cropBoxLeft + cropBoxW, cropBoxTop + (cropBoxH * 2) / 3),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }

                // Action Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = {
                            scale = 1.0f
                            offsetX = 0f
                            offsetY = 0f
                            rotationDegrees = 0f
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("↺ Reset", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }

                    // Rotate 90 Button
                    OutlinedButton(
                        onClick = {
                            rotationDegrees = (rotationDegrees + 90f) % 360f
                        },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("⟳ Rotate", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }

                    // Confirm & Normalize Button
                    Button(
                        onClick = {
                            isProcessing = true
                            val croppedFile = processAndExportCroppedGarment(
                                context = context,
                                source = sourceBitmap,
                                scale = scale,
                                offsetX = offsetX,
                                offsetY = offsetY,
                                rotationDeg = rotationDegrees,
                            )
                            isProcessing = false
                            if (croppedFile != null) {
                                onCropped(croppedFile)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                        enabled = !isProcessing,
                    ) {
                        Text(
                            if (isProcessing) "Optimizing..." else "✓ Use Garment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Normalizes, crops, and flattens garment onto clean white background (sRGB JPEG max 1280px)
 * optimized specifically for YouCam Cloud Clothes V3 VTO engine.
 */
private fun processAndExportCroppedGarment(
    context: Context,
    source: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    rotationDeg: Float,
): File? {
    return runCatching {
        // Target high-res canvas (1024 x 1280)
        val targetW = 1024
        val targetH = 1280
        val cleanCanvasBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(cleanCanvasBitmap)

        // Solid White Studio Background (Optimal for YouCam VTO edge extraction)
        canvas.drawColor(AndroidColor.WHITE)

        val matrix = Matrix().apply {
            // Apply scale & translation (min preserves entire garment in canvas)
            val baseScale = min(targetW.toFloat() / source.width, targetH.toFloat() / source.height) * scale
            postScale(baseScale, baseScale)
            postTranslate(
                (targetW - source.width * baseScale) / 2 + offsetX * (targetW / 400f),
                (targetH - source.height * baseScale) / 2 + offsetY * (targetH / 500f),
            )
            if (rotationDeg != 0f) {
                postRotate(rotationDeg, targetW / 2f, targetH / 2f)
            }
        }

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        canvas.drawBitmap(source, matrix, paint)

        // Save to cache directory
        val dir = File(context.cacheDir, "garment_crops").apply { mkdirs() }
        val outputFile = File(dir, "garment_prep_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outputFile).use { out ->
            cleanCanvasBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        outputFile
    }.getOrNull()
}
