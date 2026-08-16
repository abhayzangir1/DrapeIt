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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
        val px = (normOffset.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val py = (normOffset.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(px, py)
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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Point & Sample Colortone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Drag 3 points on forehead & cheeks to sample", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.40f), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
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

                // INTERACTIVE PHOTO VIEWPORT WITH DRAGGABLE COLOR SAMPLING PIN PUCKETS
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "User Headshot",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { tapOffset ->
                                    val normX = (tapOffset.x / size.width).coerceIn(0.05f, 0.95f)
                                    val normY = (tapOffset.y / size.height).coerceIn(0.05f, 0.95f)
                                    pointOffsets[selectedPointIndex] = Offset(normX, normY)
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val current = pointOffsets[selectedPointIndex]
                                    val newNormX = (current.x + dragAmount.x / size.width).coerceIn(0.05f, 0.95f)
                                    val newNormY = (current.y + dragAmount.y / size.height).coerceIn(0.05f, 0.95f)
                                    pointOffsets[selectedPointIndex] = Offset(newNormX, newNormY)
                                }
                            },
                    ) {
                        val w = size.width
                        val h = size.height

                        pointOffsets.forEachIndexed { index, normPos ->
                            val isSelected = index == selectedPointIndex
                            val center = Offset(normPos.x * w, normPos.y * h)
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
