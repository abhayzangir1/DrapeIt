package com.drapeproof.mobile.profile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.drapeproof.mobile.camera.ControlledCameraPreview
import com.drapeproof.mobile.camera.FrameReading
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.ui.theme.EditorialCream
import com.drapeproof.mobile.ui.theme.EditorialInk
import com.drapeproof.mobile.ui.theme.EditorialMuted
import com.drapeproof.mobile.ui.theme.EditorialPositive
import com.drapeproof.mobile.ui.theme.EditorialSand
import com.drapeproof.mobile.ui.theme.EditorialSienna
import com.drapeproof.mobile.ui.theme.EditorialWarning
import kotlinx.coroutines.delay

@Composable
fun LiveSkinScanScreen(
    onDismiss: () -> Unit,
    onScanSuccess: (skinHex: String) -> Unit,
) {
    val context = LocalContext.current

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
    var isCalibrated by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = calibrationProgress,
        animationSpec = tween(150),
        label = "progressAnim",
    )

    // Virtual KYC Calibration Engine
    LaunchedEffect(latestReading) {
        val reading = latestReading
        if (reading != null && reading.hasFace && reading.basicCaptureReady) {
            reading.skinSrgb?.let { srgb ->
                detectedSkinHex = srgb.toHex()
            }
            if (calibrationProgress < 1.0f) {
                calibrationProgress = (calibrationProgress + 0.08f).coerceAtMost(1.0f)
            } else if (!isCalibrated) {
                isCalibrated = true
                val finalHex = detectedSkinHex ?: "#D8B498"
                val profile = SkinProfileRepository.deriveProfileFromSkinHex(finalHex, source = "live_kyc_scan")
                SkinProfileRepository.save(context, profile)
                delay(1200)
                onScanSuccess(finalHex)
            }
        } else {
            if (calibrationProgress > 0.1f && !isCalibrated) {
                calibrationProgress = (calibrationProgress - 0.04f).coerceAtLeast(0f)
            }
        }
    }

    val ovalGuideColor by animateColorAsState(
        targetValue = when {
            isCalibrated -> EditorialPositive
            calibrationProgress > 0.5f -> EditorialPositive.copy(alpha = 0.85f)
            latestReading?.hasFace == true -> EditorialWarning
            else -> Color.White.copy(alpha = 0.60f)
        },
        animationSpec = tween(250),
        label = "ovalColorAnim",
    )

    if (!permissionGranted) {
        Surface(modifier = Modifier.fillMaxSize(), color = EditorialCream) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text("Camera Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EditorialInk)
                Spacer(Modifier.height(8.dp))
                Text(
                    "To calibrate your personal colortone, DrapeIt performs a live facial scan under daylight.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = EditorialMuted,
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = EditorialSienna),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Grant Camera Access", color = Color.White)
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. LIVE CAMERA FEED
        ControlledCameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFrame = { reading -> latestReading = reading },
            onControlsReady = {},
            onCameraError = {},
        )

        // 2. KYC BIOMETRIC SCANNER RETICLE & PROGRESS RING
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width * 0.50f, height * 0.40f)
            val ovalW = width * 0.62f
            val ovalH = height * 0.38f

            // Shaded vignette outside the oval
            drawOval(
                color = ovalGuideColor,
                topLeft = Offset(center.x - ovalW / 2, center.y - ovalH / 2),
                size = Size(ovalW, ovalH),
                style = Stroke(width = if (isCalibrated) 5.dp.toPx() else 3.dp.toPx()),
            )

            // Dynamic progress arc around the oval
            if (!isCalibrated && animatedProgress > 0f) {
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

        // 3. TOP BAR WITH CLOSE BUTTON
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("Live Colortone KYC Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // 4. BOTTOM STATUS INSTRUCTION CARD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCalibrated) EditorialPositive else Color.Black.copy(alpha = 0.82f),
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isCalibrated) {
                        Text("✓", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Colortone Calibrated Successfully!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Sampled skin tone: ${detectedSkinHex ?: "#D8B498"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                    } else {
                        val statusText = when {
                            latestReading?.hasFace != true -> "Align your face inside the oval"
                            latestReading?.sharpEnough != true -> "Hold steady for camera focus"
                            latestReading?.occlusionFree != true -> "Ensure forehead and cheeks are clearly visible"
                            else -> "Scanning cheek & forehead tone... ${(animatedProgress * 100).toInt()}%"
                        }

                        Text(
                            statusText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Avoid harsh direct shadows or backlights for best CIELAB accuracy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
