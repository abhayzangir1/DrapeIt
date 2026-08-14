package com.drapeproof.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialStone

private val curatedColorPresets = listOf(
    "#831843" to "Royal Burgundy",
    "#1E3A8A" to "Cobalt Navy",
    "#065F46" to "Deep Emerald",
    "#78350F" to "Cognac Brown",
    "#4C1D95" to "Midnight Plum",
    "#1F2937" to "Anthracite Slate",
    "#9A3412" to "Terracotta",
    "#0E7490" to "Deep Teal",
    "#374151" to "Charcoal",
    "#713F12" to "Warm Ochre",
    "#881337" to "Crimson Wine",
    "#D97706" to "Amber Gold",
    "#2563EB" to "Royal Blue",
    "#059669" to "Forest Pine",
    "#E11D48" to "Ruby Rose",
    "#475569" to "Classic Slate",
)

@Composable
fun UniversalColorPickerDialog(
    initialColorHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (hex: String, name: String) -> Unit,
) {
    var hexInput by remember { mutableStateOf(initialColorHex.removePrefix("#").uppercase()) }
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0.85f) }
    var value by remember { mutableFloatStateOf(0.65f) }

    val activeColor = remember(hue, saturation, value, hexInput) {
        runCatching {
            val parsed = android.graphics.Color.parseColor("#$hexInput")
            Color(parsed)
        }.getOrElse {
            val hsv = floatArrayOf(hue, saturation, value)
            val argb = android.graphics.Color.HSVToColor(hsv)
            Color(argb)
        }
    }

    val hueGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Universal Color Wheel", fontWeight = FontWeight.Bold, color = EditorialInk)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Choose any color across the full 16.7M spectrum or enter a custom hex code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EditorialMuted,
                )
                Spacer(Modifier.height(14.dp))

                // COLOR PREVIEW & HEX DISPLAY
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(activeColor)
                            .border(2.dp, EditorialStone, RoundedCornerShape(14.dp)),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("HEX CODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna)
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isLetterOrDigit() }.take(6).uppercase()
                                hexInput = clean
                            },
                            prefix = { Text("#", fontWeight = FontWeight.Bold, color = EditorialInk) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // HUE SLIDER
                Text("HUE SPECTRUM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(hueGradient),
                )
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        val hsv = floatArrayOf(hue, saturation, value)
                        val argb = android.graphics.Color.HSVToColor(hsv)
                        hexInput = String.format("%06X", (0xFFFFFF and argb))
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = EditorialSienna,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                )

                // SATURATION & BRIGHTNESS
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SATURATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted, fontSize = 10.sp)
                        Slider(
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                val hsv = floatArrayOf(hue, saturation, value)
                                val argb = android.graphics.Color.HSVToColor(hsv)
                                hexInput = String.format("%06X", (0xFFFFFF and argb))
                            },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = EditorialSienna, activeTrackColor = EditorialSienna),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BRIGHTNESS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialMuted, fontSize = 10.sp)
                        Slider(
                            value = value,
                            onValueChange = {
                                value = it
                                val hsv = floatArrayOf(hue, saturation, value)
                                val argb = android.graphics.Color.HSVToColor(hsv)
                                hexInput = String.format("%06X", (0xFFFFFF and argb))
                            },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = EditorialSienna, activeTrackColor = EditorialSienna),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // QUICK PRESETS
                Text("CURATED PALETTE PRESETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    curatedColorPresets.forEach { (hex, name) ->
                        val presetColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hexInput.equals(hex.removePrefix("#"), ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(presetColor)
                                .border(if (isSelected) 3.dp else 1.dp, if (isSelected) EditorialSienna else Color.LightGray, CircleShape)
                                .clickable {
                                    hexInput = hex.removePrefix("#").uppercase()
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
                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
            ) {
                Text("Apply Color", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = EditorialInk)
            }
        },
    )
}
