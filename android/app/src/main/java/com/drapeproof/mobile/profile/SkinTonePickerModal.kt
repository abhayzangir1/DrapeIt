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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

private data class SamplerPoint(
    val id: String,
    val label: String,
    val normalizedX: Float,
    val normalizedY: Float,
)

@Composable
fun SkinTonePickerModal(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onSaved: (skinHex: String) -> Unit,
) {
    val context = LocalContext.current

    var points by remember {
        mutableStateOf(
            listOf(
                SamplerPoint("forehead", "Forehead", 0.50f, 0.36f),
                SamplerPoint("leftCheek", "Left Cheek", 0.38f, 0.52f),
                SamplerPoint("rightCheek", "Right Cheek", 0.62f, 0.52f),
            ),
        )
    }

    var selectedPointIndex by remember { mutableStateOf(0) }

    fun sampleColorAt(normX: Float, normY: Float): Int {
        val px = (normX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val py = (normY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(px, py)
    }

    val sampledColors = points.map { sampleColorAt(it.normalizedX, it.normalizedY) }

    val averagedHex = remember(points) {
        var rSum = 0
        var gSum = 0
        var bSum = 0
        sampledColors.forEach { c ->
            rSum += android.graphics.Color.red(c)
            gSum += android.graphics.Color.green(c)
            bSum += android.graphics.Color.blue(c)
        }
        val avgR = (rSum / sampledColors.size).coerceIn(0, 255)
        val avgG = (gSum / sampledColors.size).coerceIn(0, 255)
        val avgB = (bSum / sampledColors.size).coerceIn(0, 255)
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
            modifier = Modifier
                .fillMaxSize()
                .background(EditorialCream),
            color = EditorialCream,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // TOP BAR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Point & Sample Colortone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                        Text("Drag 3 points on forehead & cheeks to sample", style = MaterialTheme.typography.bodySmall, color = EditorialMuted)
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EditorialSand)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✕", fontSize = 16.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // LIVE SAMPLED COLOR & DERIVED SEASON PREVIEW CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, EditorialStone.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(averagedHex)))
                                .border(2.5.dp, EditorialSienna, CircleShape),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SAMPLED COLORTONE: $averagedHex",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialSienna,
                                fontSize = 11.sp,
                            )
                            Text(
                                "${derivedProfile.season} • ${derivedProfile.undertone}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EditorialInk,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // INTERACTIVE IMAGE WITH MOVABLE TARGET PINS
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    val containerW = maxWidth
                    val containerH = maxHeight

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "User Portrait for Sampling",
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val nx = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                                    val ny = (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                                    points = points.toMutableList().also { list ->
                                        list[selectedPointIndex] = list[selectedPointIndex].copy(
                                            normalizedX = nx,
                                            normalizedY = ny,
                                        )
                                    }
                                }
                            },
                        contentScale = ContentScale.Fit,
                    )

                    // RENDER 3 SAMPLING PINS
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        points.forEachIndexed { index, pt ->
                            val isSel = index == selectedPointIndex
                            val cx = pt.normalizedX * w
                            val cy = pt.normalizedY * h

                            // Outer Pulse Circle
                            drawCircle(
                                color = if (isSel) EditorialSienna else Color.White,
                                radius = if (isSel) 18.dp.toPx() else 14.dp.toPx(),
                                center = Offset(cx, cy),
                            )
                            // Inner Sample Color Circle
                            val ptColor = sampledColors.getOrNull(index) ?: android.graphics.Color.GRAY
                            drawCircle(
                                color = Color(ptColor),
                                radius = if (isSel) 13.dp.toPx() else 10.dp.toPx(),
                                center = Offset(cx, cy),
                            )
                        }
                    }

                    // POINTER DRAG GESTURE OVERLAY
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val currentX = points[selectedPointIndex].normalizedX * size.width
                                    val currentY = points[selectedPointIndex].normalizedY * size.height
                                    val newX = (currentX + dragAmount.x).coerceIn(0f, size.width.toFloat())
                                    val newY = (currentY + dragAmount.y).coerceIn(0f, size.height.toFloat())
                                    points = points.toMutableList().also { list ->
                                        list[selectedPointIndex] = list[selectedPointIndex].copy(
                                            normalizedX = newX / size.width,
                                            normalizedY = newY / size.height,
                                        )
                                    }
                                }
                            },
                    )
                }

                Spacer(Modifier.height(10.dp))

                // POINT SELECTOR BUTTONS: [ Forehead ] [ Left Cheek ] [ Right Cheek ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    points.forEachIndexed { index, pt ->
                        val isSel = index == selectedPointIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) EditorialSienna else Color.White)
                                .border(1.dp, if (isSel) EditorialSienna else EditorialStone.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { selectedPointIndex = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else EditorialInk,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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
