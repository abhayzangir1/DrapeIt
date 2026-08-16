package com.drapeproof.mobile.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialSienna

data class SamplingPoint(
    val label: String,
    val initialNormX: Float,
    val initialNormY: Float,
)

@Composable
fun SkinTonePickerModal(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onSaved: (averagedHex: String) -> Unit,
) {
    val context = LocalContext.current

    val points = remember {
        listOf(
            SamplingPoint("Forehead", 0.50f, 0.32f),
            SamplingPoint("Left Cheek", 0.38f, 0.48f),
            SamplingPoint("Right Cheek", 0.62f, 0.48f),
        )
    }

    val pointOffsets = remember {
        mutableStateListOf(
            Offset(points[0].initialNormX, points[0].initialNormY),
            Offset(points[1].initialNormX, points[1].initialNormY),
            Offset(points[2].initialNormX, points[2].initialNormY),
        )
    }

    var selectedPointIndex by remember { mutableIntStateOf(0) }

    fun samplePixelColor(normOffset: Offset): Int {
        val patchSize = 2 // radius
        val pixels = mutableListOf<Int>()
        var totalR = 0.0
        var totalG = 0.0
        var totalB = 0.0
        for (dy in -patchSize..patchSize) {
            for (dx in -patchSize..patchSize) {
                val sx = (normOffset.x * bitmap.width + dx).toInt().coerceIn(0, bitmap.width - 1)
                val sy = (normOffset.y * bitmap.height + dy).toInt().coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(sx, sy)
                pixels.add(pixel)
                totalR += android.graphics.Color.red(pixel)
                totalG += android.graphics.Color.green(pixel)
                totalB += android.graphics.Color.blue(pixel)
            }
        }
        val avgR = totalR / pixels.size
        val avgG = totalG / pixels.size
        val avgB = totalB / pixels.size
        // Spatial centroid median: select genuine pixel closest to patch average
        return pixels.minByOrNull { p ->
            val dr = android.graphics.Color.red(p) - avgR
            val dg = android.graphics.Color.green(p) - avgG
            val db = android.graphics.Color.blue(p) - avgB
            dr * dr + dg * dg + db * db
        } ?: bitmap.getPixel((normOffset.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1), (normOffset.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1))
    }

    val sampledColors = pointOffsets.map { samplePixelColor(it) }

    val averagedHex = remember(pointOffsets[0], pointOffsets[1], pointOffsets[2]) {
        var totalR = 0
        var totalG = 0
        var totalB = 0
        sampledColors.forEach { c ->
            totalR += (c shr 16) and 0xFF
            totalG += (c shr 8) and 0xFF
            totalB += c and 0xFF
        }
        val avgR = totalR / sampledColors.size
        val avgG = totalG / sampledColors.size
        val avgB = totalB / sampledColors.size
        String.format("#%02X%02X%02X", avgR, avgG, avgB)
    }

    val derivedProfile = remember(averagedHex) {
        SkinProfileRepository.deriveProfileFromSkinHex(averagedHex, source = "photo_point_picker")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(20.dp),
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "Manual Skin Calibration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Drag pucks to your forehead & cheeks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // LIVE SAMPLED COLOR & DERIVED SEASON PREVIEW CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(averagedHex)))
                                .border(2.dp, EditorialGold, CircleShape),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Derived Archetype: ${derivedProfile.season}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${derivedProfile.undertone} • $averagedHex",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EditorialSienna,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // INTERACTIVE PHOTO VIEWPORT WITH ACCURATE TOUCH MAPPING
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "User Headshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    val wBox = size.width.toFloat()
                                    val hBox = size.height.toFloat()
                                    val imgW = bitmap.width.toFloat()
                                    val imgH = bitmap.height.toFloat()
                                    val imgAspect = imgW / imgH
                                    val boxAspect = wBox / hBox

                                    val renderW = if (imgAspect > boxAspect) wBox else hBox * imgAspect
                                    val renderH = if (imgAspect > boxAspect) wBox / imgAspect else hBox
                                    val leftOff = (wBox - renderW) / 2f
                                    val topOff = (hBox - renderH) / 2f

                                    val normX = ((tapOffset.x - leftOff) / renderW).coerceIn(0.02f, 0.98f)
                                    val normY = ((tapOffset.y - topOff) / renderH).coerceIn(0.02f, 0.98f)
                                    pointOffsets[selectedPointIndex] = Offset(normX, normY)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val wBox = size.width.toFloat()
                                    val hBox = size.height.toFloat()
                                    val imgW = bitmap.width.toFloat()
                                    val imgH = bitmap.height.toFloat()
                                    val imgAspect = imgW / imgH
                                    val boxAspect = wBox / hBox

                                    val renderW = if (imgAspect > boxAspect) wBox else hBox * imgAspect
                                    val renderH = if (imgAspect > boxAspect) wBox / imgAspect else hBox

                                    val current = pointOffsets[selectedPointIndex]
                                    val newNormX = (current.x + dragAmount.x / renderW).coerceIn(0.02f, 0.98f)
                                    val newNormY = (current.y + dragAmount.y / renderH).coerceIn(0.02f, 0.98f)
                                    pointOffsets[selectedPointIndex] = Offset(newNormX, newNormY)
                                }
                            },
                    ) {
                        val wBox = size.width
                        val hBox = size.height
                        val imgW = bitmap.width.toFloat()
                        val imgH = bitmap.height.toFloat()
                        val imgAspect = imgW / imgH
                        val boxAspect = wBox / hBox

                        val renderW = if (imgAspect > boxAspect) wBox else hBox * imgAspect
                        val renderH = if (imgAspect > boxAspect) wBox / imgAspect else hBox
                        val leftOff = (wBox - renderW) / 2f
                        val topOff = (hBox - renderH) / 2f

                        pointOffsets.forEachIndexed { index, normPos ->
                            val isSelected = index == selectedPointIndex
                            val center = Offset(leftOff + normPos.x * renderW, topOff + normPos.y * renderH)
                            val pinColor = Color(sampledColors[index])

                            // Outer Glow Ring
                            drawCircle(
                                color = if (isSelected) EditorialSienna else Color.White,
                                radius = if (isSelected) 22.dp.toPx() else 16.dp.toPx(),
                                center = center,
                                style = Stroke(width = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx()),
                            )

                            // Inner Swatch Disc
                            drawCircle(
                                color = pinColor,
                                radius = if (isSelected) 18.dp.toPx() else 12.dp.toPx(),
                                center = center,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // POINT SELECTOR BUTTONS: [ Forehead ] [ Left Cheek ] [ Right Cheek ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    points.forEachIndexed { index, pt ->
                        val isSel = index == selectedPointIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) EditorialSienna else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSel) EditorialGold.copy(alpha = 0.85f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .clickable { selectedPointIndex = index }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // SAVE COLORTONE CTA
                Button(
                    onClick = {
                        val finalProfile = SkinProfileRepository.deriveProfileFromSkinHex(averagedHex, source = "photo_point_picker")
                        SkinProfileRepository.save(context, finalProfile)
                        onSaved(averagedHex)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text("Save Colortone ✓", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}
