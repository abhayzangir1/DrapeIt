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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drapeproof.mobile.ui.theme.EditorialCream
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
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red,
            ),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Color Palette & Spectrum", fontWeight = FontWeight.Bold, color = EditorialInk, style = MaterialTheme.typography.titleMedium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(EditorialSand)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 14.sp, color = EditorialInk, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // COLOR PREVIEW & HEX DISPLAY CARD
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EditorialCream),
                    modifier = Modifier.fillMaxWidth().border(1.dp, EditorialStone.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(activeColor)
                                .border(2.dp, Color.White, RoundedCornerShape(14.dp)),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("HEX COLOR CODE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialSienna, fontSize = 10.sp)
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
                }

                Spacer(Modifier.height(14.dp))

                // HUE SPECTRUM SLIDER
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
                            valueRange = 0.05f..1.0f,
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
                            valueRange = 0.05f..1.0f,
                            colors = SliderDefaults.colors(thumbColor = EditorialSienna, activeTrackColor = EditorialSienna),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // QUICK PRESETS
                Text("CURATED PALETTE PRESETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = EditorialInk)
                Spacer(Modifier.height(6.dp))
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
                                .size(34.dp)
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
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Apply Color", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {},
    )
}
