package com.drapeproof.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val curatedColorPresets = listOf(
    "#831843" to "Royal Burgundy",
    "#1E3A8A" to "Cobalt Navy",
    "#065F46" to "Deep Emerald",
    "#78350F" to "Cognac Brown",
    "#4C1D95" to "Midnight Plum",
    "#0F172A" to "Anthracite Slate",
    "#9A3412" to "Terracotta",
    "#0E7490" to "Deep Teal",
    "#D97706" to "Amber Gold",
    "#2563EB" to "Royal Blue",
    "#059669" to "Forest Pine",
    "#E11D48" to "Ruby Rose",
    "#475569" to "Classic Slate",
    "#B45309" to "Spiced Ochre",
    "#6B21A8" to "Imperial Purple",
    "#166534" to "Rich Olive",
    "#FFFFFF" to "Pure White",
    "#000000" to "Deep Black",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (hex: String, name: String) -> Unit,
) {
    var hexInput by remember { mutableStateOf(initialColorHex.removePrefix("#").uppercase()) }
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0.85f) }
    var value by remember { mutableFloatStateOf(0.70f) }

    // Synchronize initial HSV from initialColorHex
    remember(initialColorHex) {
        runCatching {
            val parsed = android.graphics.Color.parseColor(if (initialColorHex.startsWith("#")) initialColorHex else "#$initialColorHex")
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed, hsv)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
        }
    }

    val activeColor = remember(hue, saturation, value, hexInput) {
        val hsv = floatArrayOf(hue, saturation, value)
        val argb = android.graphics.Color.HSVToColor(hsv)
        Color(argb)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Spectrum & Color Wheel",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. CIRCULAR COLOR WHEEL / SPECTRUM SLIDER
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val spectrumSweep = remember {
                        Brush.sweepGradient(
                            listOf(
                                Color.Red, Color.Yellow, Color.Green,
                                Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
                            ),
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures { change, _ ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touch = change.position
                                    val angleRad = atan2(touch.y - center.y, touch.x - center.x)
                                    var degrees = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    if (degrees < 0) degrees += 360f
                                    hue = degrees
                                    val hsv = floatArrayOf(hue, saturation, value)
                                    val argb = android.graphics.Color.HSVToColor(hsv)
                                    hexInput = String.format("%06X", (0xFFFFFF and argb))
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures { touch ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val angleRad = atan2(touch.y - center.y, touch.x - center.x)
                                    var degrees = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    if (degrees < 0) degrees += 360f
                                    hue = degrees
                                    val hsv = floatArrayOf(hue, saturation, value)
                                    val argb = android.graphics.Color.HSVToColor(hsv)
                                    hexInput = String.format("%06X", (0xFFFFFF and argb))
                                }
                            },
                    ) {
                        val strokeWidth = 24.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Outer Rainbow Wheel
                        drawCircle(
                            brush = spectrumSweep,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )

                        // Circular Thumb Indicator positioned on the wheel
                        val angleRad = Math.toRadians(hue.toDouble())
                        val thumbX = center.x + radius * cos(angleRad).toFloat()
                        val thumbY = center.y + radius * sin(angleRad).toFloat()
                        val thumbRadius = 14.dp.toPx()

                        // Dynamic live-color filled thumb ring
                        drawCircle(
                            color = Color.White,
                            radius = thumbRadius + 2.dp.toPx(),
                            center = Offset(thumbX, thumbY),
                        )
                        drawCircle(
                            color = activeColor,
                            radius = thumbRadius,
                            center = Offset(thumbX, thumbY),
                        )
                        drawCircle(
                            color = EditorialSienna,
                            radius = thumbRadius,
                            center = Offset(thumbX, thumbY),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }

                    // Center live color swatch preview disc
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                            .border(3.dp, Color.White, CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "#$hexInput",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (value > 0.5f && saturation < 0.6f) Color.Black else Color.White,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 2. SATURATION & BRIGHTNESS ROUND SLIDERS
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("SATURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna, fontSize = 10.sp)
                        Text("${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Slider(
                        value = saturation,
                        onValueChange = {
                            saturation = it
                            val hsv = floatArrayOf(hue, saturation, value)
                            val argb = android.graphics.Color.HSVToColor(hsv)
                            hexInput = String.format("%06X", (0xFFFFFF and argb))
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = EditorialSienna,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("BRIGHTNESS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna, fontSize = 10.sp)
                        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Slider(
                        value = value,
                        onValueChange = {
                            value = it
                            val hsv = floatArrayOf(hue, saturation, value)
                            val argb = android.graphics.Color.HSVToColor(hsv)
                            hexInput = String.format("%06X", (0xFFFFFF and argb))
                        },
                        valueRange = 0.05f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = activeColor,
                            activeTrackColor = EditorialSienna,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 3. CURATED PALETTE SWATCHES
                Text(
                    "CURATED PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EditorialSienna,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    curatedColorPresets.forEach { (hex, _) ->
                        val presetColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hexInput.equals(hex.removePrefix("#"), ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) EditorialSienna else MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable {
                                    hexInput = hex.removePrefix("#").uppercase()
                                    val parsed = android.graphics.Color.parseColor(hex)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(parsed, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = "#$hexInput"
                    val matchingPreset = curatedColorPresets.firstOrNull { it.first.equals(finalHex, ignoreCase = true) }
                    val colorName = matchingPreset?.second ?: "Custom Color ($finalHex)"
                    onColorSelected(finalHex, colorName)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Apply Color", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            }
        },
        dismissButton = {},
    )
}
