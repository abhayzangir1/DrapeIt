package com.drapeproof.mobile.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.drapeproof.core.color.ColorConversions
import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.color.SrgbColor
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drapeproof.mobile.camera.ControlledCameraPreview
import com.drapeproof.mobile.camera.FrameReading
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.ui.sound.SoundEffectManager
import com.drapeproof.mobile.ui.theme.EditorialGold
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialWarning

@Composable
fun LiveSkinScanScreen(
    onDismiss: () -> Unit,
    onScanSuccess: (skinHex: String) -> Unit,
) {
    val context = LocalContext.current
    val currentView = LocalView.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var latestReading by remember { mutableStateOf<FrameReading?>(null) }
    var detectedSkinHex by remember { mutableStateOf<String?>(null) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }
    var isScanLocked by remember { mutableStateOf(false) }
    val skinBuffer = remember { mutableListOf<SrgbColor>() }

    val animatedProgress by animateFloatAsState(
        targetValue = calibrationProgress,
        animationSpec = tween(150),
        label = "progressAnim",
    )

    // Progressive Skin Tone Colorimetric Stability Accumulator (15-frame sliding window <= 1.2 DeltaE00)
    LaunchedEffect(latestReading) {
        if (isScanLocked) return@LaunchedEffect

        val reading = latestReading
        if (reading != null && reading.hasFace && reading.basicCaptureReady && reading.skinSrgb != null) {
            skinBuffer.add(reading.skinSrgb)
            if (skinBuffer.size > 15) {
                skinBuffer.removeAt(0)
            }

            // Calculate mean color across current buffer
            val avgR = skinBuffer.map { it.red }.average().toInt().coerceIn(0, 255)
            val avgG = skinBuffer.map { it.green }.average().toInt().coerceIn(0, 255)
            val avgB = skinBuffer.map { it.blue }.average().toInt().coerceIn(0, 255)
            val meanSrgb = SrgbColor(avgR, avgG, avgB)
            val meanLab = ColorConversions.srgbToLab(meanSrgb)

            // Compute maximum deltaE00 variance against mean in current sliding window
            val maxDeltaE = skinBuffer.map { ColorDifference.ciede2000(ColorConversions.srgbToLab(it), meanLab) }.maxOrNull() ?: 0.0

            val currentHex = meanSrgb.toHex()
            detectedSkinHex = currentHex

            // If variation is low (<= 1.4 DeltaE) and face is still, advance progress genuinely
            if (maxDeltaE <= 1.4) {
                val fraction = (skinBuffer.size.toFloat() / 15f).coerceIn(0f, 1f)
                calibrationProgress = fraction

                if (skinBuffer.size >= 15 && maxDeltaE <= 1.2) {
                    isScanLocked = true
                    SoundEffectManager.playSuccess(currentView)
                }
            } else {
                // If lighting is fluctuating, trim buffer
                if (skinBuffer.size > 3) skinBuffer.removeAt(0)
                calibrationProgress = (skinBuffer.size.toFloat() / 15f).coerceIn(0f, 1f)
            }
        } else {
            if (skinBuffer.isNotEmpty()) skinBuffer.removeAt(0)
            calibrationProgress = (skinBuffer.size.toFloat() / 15f).coerceIn(0f, 1f)
        }
    }

    val ovalGuideColor by animateColorAsState(
        targetValue = when {
            isScanLocked -> EditorialPositive
            calibrationProgress > 0.5f -> EditorialPositive.copy(alpha = 0.85f)
            latestReading?.hasFace == true -> EditorialGold
            else -> Color.White.copy(alpha = 0.60f)
        },
        animationSpec = tween(250),
        label = "ovalColorAnim",
    )

    if (!permissionGranted) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Camera Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "To calibrate your personal colortone, please grant camera access.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Enable Camera", color = Color.White)
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LIVE CAMERA FEED (PAUSES FEEDBACK PROCESSING WHEN LOCKED)
        ControlledCameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrame = { reading ->
                if (!isScanLocked) {
                    latestReading = reading
                }
            },
            onControlsReady = {},
            onCameraError = {},
        )

        // 2. BIOMETRIC SCANNER RETICLE & PROGRESS RING
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width * 0.50f, height * 0.38f)
            val ovalW = width * 0.60f
            val ovalH = height * 0.36f

            drawOval(
                color = ovalGuideColor,
                topLeft = Offset(center.x - ovalW / 2, center.y - ovalH / 2),
                size = Size(ovalW, ovalH),
                style = Stroke(width = if (isScanLocked) 4.5.dp.toPx() else 2.5.dp.toPx()),
            )

            // Dynamic progress arc around the oval
            if (!isScanLocked && animatedProgress > 0f) {
                drawArc(
                    color = EditorialPositive,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(center.x - (ovalW / 2 + 10.dp.toPx()), center.y - (ovalH / 2 + 10.dp.toPx())),
                    size = Size(ovalW + 20.dp.toPx(), ovalH + 20.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx()),
                )
            }
        }

        // 3. TOP BAR WITH CONCISE INSTRUCTION & CLOSE BUTTON
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(0.75.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text("Live Facial Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.70f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✕", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // CONCISE INSTRUCTION PILL
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(0.75.dp, EditorialGold.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Align face in soft natural light • Hold steady for 2 seconds",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        // 4. BOTTOM STATUS OR LOCKED CONFIRMATION CARD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.50f), RoundedCornerShape(22.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isScanLocked && detectedSkinHex != null) {
                        val finalHex = detectedSkinHex!!
                        val previewProfile = remember(finalHex) {
                            SkinProfileRepository.deriveProfileFromSkinHex(finalHex, source = "live_facial_scan")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(finalHex.asComposeColor())
                                    .border(2.5.dp, EditorialGold.copy(alpha = 0.85f), CircleShape),
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    previewProfile.season,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${finalHex.uppercase()} • ${previewProfile.undertone} Undertone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EditorialSienna,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    calibrationProgress = 0f
                                    isScanLocked = false
                                    detectedSkinHex = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Rescan", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val finalProfile = SkinProfileRepository.deriveProfileFromSkinHex(finalHex, source = "live_facial_scan")
                                    SkinProfileRepository.save(context, finalProfile)
                                    onScanSuccess(finalHex)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Apply Colortone", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        val reading = latestReading
                        val hasFace = reading?.hasFace == true
                        val ready = reading?.basicCaptureReady == true

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (hasFace) "✨" else "⏳", fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        when {
                                            !hasFace -> "Position face within oval"
                                            !ready -> "Stabilizing facial lighting..."
                                            else -> "Scanning colortone (${(animatedProgress * 100).toInt()}%)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        detectedSkinHex?.let { "Sample: ${it.uppercase()}" } ?: "Searching for skin tone...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            detectedSkinHex?.let { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(hex.asComposeColor())
                                        .border(1.5.dp, EditorialGold, CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String.asComposeColor(): Color {
    return runCatching {
        val value = removePrefix("#").toLong(16)
        Color(
            red = ((value shr 16) and 0xFF).toInt(),
            green = ((value shr 8) and 0xFF).toInt(),
            blue = (value and 0xFF).toInt(),
        )
    }.getOrDefault(Color.Gray)
}
