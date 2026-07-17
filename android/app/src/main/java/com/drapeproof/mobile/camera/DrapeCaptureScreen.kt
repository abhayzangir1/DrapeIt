package com.drapeproof.mobile.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.drapeproof.core.capture.CaptureQualityMetrics
import com.drapeproof.core.capture.QualityGateEvaluator
import com.drapeproof.core.capture.QualityGateResult
import com.drapeproof.core.color.ColorDifference
import com.drapeproof.core.color.LabColor
import com.drapeproof.core.domain.ContrastCalculator
import com.drapeproof.core.domain.ContrastVector
import com.drapeproof.core.domain.EvidenceInputs
import com.drapeproof.core.domain.EvidencePolicy
import com.drapeproof.core.domain.EvidenceTier
import com.drapeproof.mobile.data.DrapeRecordRepository
import com.drapeproof.mobile.data.LocalDrapeRecord
import com.drapeproof.mobile.data.SkinProfileRepository
import com.drapeproof.mobile.data.StoredSkinProfile
import com.drapeproof.mobile.ui.theme.Cobalt
import com.drapeproof.mobile.ui.theme.DrapeCoral
import com.drapeproof.mobile.ui.theme.Moss
import kotlin.math.abs
import java.util.Locale

private const val TARGET_READINGS = 18
private const val MIN_SAMPLE_GAP_NANOS = 140_000_000L

private enum class CapturePhase {
    INTRO,
    OPENING_BASELINE,
    ADD_FABRIC,
    FABRIC,
    REMOVE_FABRIC,
    CLOSING_BASELINE,
}

private data class SessionOutcome(
    val vector: ContrastVector,
    val quality: QualityGateResult,
    val evidenceTier: EvidenceTier,
    val skinColorHex: String,
    val fabricColorHex: String,
    val captureControl: CameraControlStatus,
)

@Composable
fun DrapeCaptureScreen(onBack: () -> Unit) {
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
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!permissionGranted) {
        CameraPermissionScreen(
            onBack = onBack,
            onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        return
    }

    ControlledCaptureSession(onBack = onBack)
}

@Composable
private fun ControlledCaptureSession(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    DisposableEffect(activity) {
        val window = activity?.window
        val previous = window?.attributes?.screenBrightness
        if (window != null) {
            val attributes = window.attributes
            attributes.screenBrightness = 0.16f
            window.attributes = attributes
        }
        onDispose {
            if (window != null && previous != null) {
                val attributes = window.attributes
                attributes.screenBrightness = previous
                window.attributes = attributes
            }
        }
    }

    var phase by remember { mutableStateOf(CapturePhase.INTRO) }
    var live by remember { mutableStateOf<FrameReading?>(null) }
    var controls by remember { mutableStateOf<DrapeCameraControls?>(null) }
    var controlStatus by remember {
        mutableStateOf(CameraControlStatus(false, false, "Camera is settling"))
    }
    var opening by remember { mutableStateOf<List<FrameReading>>(emptyList()) }
    var fabric by remember { mutableStateOf<List<FrameReading>>(emptyList()) }
    var closing by remember { mutableStateOf<List<FrameReading>>(emptyList()) }
    var activeReadings by remember { mutableStateOf<List<FrameReading>>(emptyList()) }
    var lastAcceptedNanos by remember { mutableStateOf(0L) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var outcome by remember { mutableStateOf<SessionOutcome?>(null) }
    var controlLockSettled by remember { mutableStateOf(false) }

    fun reset() {
        controls?.unlock()
        phase = CapturePhase.INTRO
        opening = emptyList()
        fabric = emptyList()
        closing = emptyList()
        activeReadings = emptyList()
        lastAcceptedNanos = 0L
        outcome = null
        cameraError = null
        controlStatus = CameraControlStatus(false, false, "Camera is settling")
        controlLockSettled = false
    }

    outcome?.let {
        DrapeResultScreen(result = it, onBack = onBack, onRetake = ::reset)
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ControlledCameraPreview(
            onFrame = { reading ->
                live = reading
                val collecting = phase == CapturePhase.OPENING_BASELINE ||
                    phase == CapturePhase.FABRIC || phase == CapturePhase.CLOSING_BASELINE
                val fabricRequired = phase == CapturePhase.FABRIC
                val farEnoughApart = reading.timestampNanos - lastAcceptedNanos >= MIN_SAMPLE_GAP_NANOS
                if (
                    collecting && reading.basicCaptureReady && farEnoughApart &&
                    (!fabricRequired || reading.fabricRegionValid)
                ) {
                    lastAcceptedNanos = reading.timestampNanos
                    activeReadings = (activeReadings + reading).take(TARGET_READINGS)
                }
            },
            onControlsReady = { controls = it },
            onCameraError = { cameraError = it },
        )

        CaptureGuide(
            ready = live?.basicCaptureReady == true,
            showFabricZone = phase == CapturePhase.ADD_FABRIC || phase == CapturePhase.FABRIC,
        )

        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 13.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.48f)),
            ) { Text("Back", color = Color.White) }
            Text("NO APP RETOUCHING", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }

        CaptureControlCard(
            modifier = Modifier.align(Alignment.BottomCenter),
            phase = phase,
            live = live,
            progress = activeReadings.size / TARGET_READINGS.toFloat(),
            controlStatus = controlStatus,
            error = cameraError,
            actionEnabled = phase != CapturePhase.ADD_FABRIC || controlLockSettled,
            onAction = {
                when (phase) {
                    CapturePhase.INTRO -> {
                        controls?.unlock()
                        opening = emptyList()
                        activeReadings = emptyList()
                        lastAcceptedNanos = 0L
                        controlLockSettled = false
                        phase = CapturePhase.OPENING_BASELINE
                    }
                    CapturePhase.ADD_FABRIC -> {
                        activeReadings = emptyList()
                        lastAcceptedNanos = 0L
                        phase = CapturePhase.FABRIC
                    }
                    CapturePhase.REMOVE_FABRIC -> {
                        activeReadings = emptyList()
                        lastAcceptedNanos = 0L
                        phase = CapturePhase.CLOSING_BASELINE
                    }
                    else -> Unit
                }
            },
        )
    }

    LaunchedEffect(activeReadings.size, phase) {
        if (activeReadings.size < TARGET_READINGS) return@LaunchedEffect
        when (phase) {
            CapturePhase.OPENING_BASELINE -> {
                opening = activeReadings
                activeReadings = emptyList()
                phase = CapturePhase.ADD_FABRIC
                controlLockSettled = false
                val currentControls = controls
                if (currentControls == null) {
                    controlStatus = CameraControlStatus(false, false, "Camera controls are unavailable")
                    controlLockSettled = true
                } else {
                    currentControls.lock {
                        controlStatus = it
                        controlLockSettled = true
                    }
                }
            }
            CapturePhase.FABRIC -> {
                fabric = activeReadings
                activeReadings = emptyList()
                phase = CapturePhase.REMOVE_FABRIC
            }
            CapturePhase.CLOSING_BASELINE -> {
                closing = activeReadings
                activeReadings = emptyList()
                outcome = calculateOutcome(opening, fabric, closing, controlStatus)
                controls?.unlock()
            }
            else -> Unit
        }
    }
}

@Composable
private fun CaptureGuide(ready: Boolean, showFabricZone: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val guideColor = if (ready) Color(0xFF75C895) else Color(0xFFFFC36C)
        val ovalWidth = size.width * 0.64f
        val ovalHeight = ovalWidth * 1.28f
        drawOval(
            color = guideColor,
            topLeft = Offset((size.width - ovalWidth) / 2f, size.height * 0.14f),
            size = Size(ovalWidth, ovalHeight),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
        if (showFabricZone) {
            val rect = Rect(
                left = size.width * 0.12f,
                top = size.height * 0.69f,
                right = size.width * 0.88f,
                bottom = size.height * 0.87f,
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.88f),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))),
            )
        }
    }
}

@Composable
private fun CaptureControlCard(
    modifier: Modifier,
    phase: CapturePhase,
    live: FrameReading?,
    progress: Float,
    controlStatus: CameraControlStatus,
    error: String?,
    actionEnabled: Boolean,
    onAction: () -> Unit,
) {
    val title = when (phase) {
        CapturePhase.INTRO -> "Make the camera trustworthy"
        CapturePhase.OPENING_BASELINE -> "Opening baseline"
        CapturePhase.ADD_FABRIC -> "Add one solid matte fabric"
        CapturePhase.FABRIC -> "Measuring the fabric beside you"
        CapturePhase.REMOVE_FABRIC -> "Remove the fabric"
        CapturePhase.CLOSING_BASELINE -> "Closing baseline"
    }
    val detail = when (phase) {
        CapturePhase.INTRO -> "Face a window, keep the phone at eye level, and turn off device beauty modes. The screen is dimmed to reduce color spill."
        CapturePhase.OPENING_BASELINE -> liveFeedback(live, "Keep a neutral expression while exposure settles.")
        CapturePhase.ADD_FABRIC -> if (actionEnabled) {
            "Controls: ${controlStatus.detail}. Cover the dashed area without shading your face."
        } else {
            "Applying camera controls. Wait for the fabric button to unlock."
        }
        CapturePhase.FABRIC -> liveFeedback(
            live,
            "Hold the same pose. Keep the cloth flat and close to your jaw.",
            includeFabricRoi = true,
        )
        CapturePhase.REMOVE_FABRIC -> "Move the fabric fully out of frame. Keep your face and phone in the same position."
        CapturePhase.CLOSING_BASELINE -> liveFeedback(live, "This detects lighting drift before a face-shift claim is allowed.")
    }
    val action = when (phase) {
        CapturePhase.INTRO -> "Begin opening baseline"
        CapturePhase.ADD_FABRIC -> "Fabric is in place"
        CapturePhase.REMOVE_FABRIC -> "Fabric is removed"
        else -> null
    }
    val collecting = phase == CapturePhase.OPENING_BASELINE ||
        phase == CapturePhase.FABRIC || phase == CapturePhase.CLOSING_BASELINE

    Card(
        modifier = modifier.fillMaxWidth().padding(14.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF51B1C1E)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (collecting) {
                    CircularProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.size(38.dp),
                        color = if (live?.basicCaptureReady == true) Moss else Color(0xFFFFC36C),
                        trackColor = Color.White.copy(alpha = 0.16f),
                    )
                    Spacer(Modifier.size(13.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    if (collecting) {
                        Text(
                            "${(progress * TARGET_READINGS).toInt()} / $TARGET_READINGS stable readings",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.62f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f))
            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Color(0xFFFF9A8E), style = MaterialTheme.typography.bodySmall)
            }
            if (action != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    enabled = actionEnabled,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
                ) { Text(action, color = Color.White) }
            }
        }
    }
}

private fun liveFeedback(
    reading: FrameReading?,
    fallback: String,
    includeFabricRoi: Boolean = false,
): String = when {
    reading == null || !reading.hasFace -> "Place one face inside the oval. $fallback"
    !reading.sharpEnough -> "Hold the phone and your face still; the image is soft."
    reading.clippedPixelFraction >= 0.01 -> "The sampled cheek area is clipped. Soften the light, then retake."
    includeFabricRoi && reading.fabricClippedPixelFraction >= 0.01 ->
        "The sampled fabric area is clipped. Soften the light or flatten the cloth."
    abs(reading.yawDegrees) > 7.0 -> "Turn a little toward the camera."
    abs(reading.rollDegrees) > 7.0 -> "Keep your head level."
    !reading.eyesOpen -> "Keep both eyes naturally open."
    !reading.neutralExpression -> "Relax your mouth for a repeatable reading."
    else -> "Good. Hold still while multiple frames are checked."
}

@Composable
private fun CameraPermissionScreen(onBack: () -> Unit, onRequest: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(72.dp).background(DrapeCoral.copy(alpha = 0.16f), CircleShape))
            Spacer(Modifier.height(22.dp))
            Text("Camera permission is required", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "Frames are analyzed on this phone. DrapeProof sends an image only when you explicitly start a YouCam feature.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("Allow camera") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        }
    }
}

@Composable
private fun DrapeResultScreen(result: SessionOutcome, onBack: () -> Unit, onRetake: () -> Unit) {
    val context = LocalContext.current
    var saved by remember(result) { mutableStateOf(false) }
    val separation = result.vector.clothSkinSeparation
    val feature = result.vector.featureDefinition
    val shift = result.vector.apparentFaceShift
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 36.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Text("Your contrast evidence", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 14.dp))
        }
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(58.dp).background(
                        color = Color(android.graphics.Color.parseColor(result.fabricColorHex)),
                        shape = RoundedCornerShape(18.dp),
                    ),
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(result.fabricColorHex, style = MaterialTheme.typography.headlineSmall)
                    Text(result.evidenceTier.displayName(), style = MaterialTheme.typography.labelLarge, color = Moss)
                }
            }
            Spacer(Modifier.height(24.dp))
            EvidenceMetric(
                color = Moss,
                label = "CLOTH–SKIN SEPARATION",
                value = "ΔE00 ${separation.deltaE00.format(1)}",
                detail = "Fabric is ${abs(separation.deltaLStar).format(1)} L* ${if (separation.deltaLStar >= 0) "lighter" else "darker"} than the captured skin region.",
            )
            EvidenceMetric(
                color = Cobalt,
                label = "FEATURE DEFINITION",
                value = feature.currentMedianDeltaE00?.let { "ΔE00 ${it.format(1)}" } ?: "Not available",
                detail = feature.changeFromBaselineDeltaE00?.let {
                    "Changed ${if (it >= 0) "+" else ""}${it.format(1)} from the opening baseline. This describes captured definition, not attractiveness."
                } ?: "A paired feature baseline was not available.",
            )
            EvidenceMetric(
                color = DrapeCoral,
                label = "APPARENT FACE SHIFT",
                value = if (shift.measured) "ΔE00 ${shift.aggregateDeltaE00!!.format(1)}" else "Withheld",
                detail = if (shift.measured) {
                    "Camera-recorded shift under locked controls. This is not a claim that your intrinsic skin color changed."
                } else {
                    shift.unavailableReason ?: "The controlled-pair gates did not pass."
                },
            )

            Spacer(Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.quality.passed) Moss.copy(alpha = 0.12f) else DrapeCoral.copy(alpha = 0.11f),
                ),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        if (result.quality.passed) "All controlled-pair gates passed" else "Evidence was honestly downgraded",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (result.quality.passed) {
                            result.captureControl.detail
                        } else {
                            result.quality.failures.take(4).joinToString(" • ") { it.code.name.lowercase().replace('_', ' ') }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    SkinProfileRepository.save(
                        context,
                        StoredSkinProfile(
                            skinHex = result.skinColorHex,
                            evidenceTier = result.evidenceTier,
                            source = "controlled real-cloth camera session",
                            capturedAtEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                    DrapeRecordRepository.add(
                        context,
                        LocalDrapeRecord.create(
                            source = "Real cloth beside face; opening and closing baselines",
                            evidenceTier = result.evidenceTier,
                            intent = null,
                            sku = "PHYSICAL-FABRIC",
                            variantId = result.fabricColorHex.removePrefix("#"),
                            variantName = "Captured fabric",
                            skinHex = result.skinColorHex,
                            fabricHex = result.fabricColorHex,
                            separationDeltaE00 = separation.deltaE00,
                            deltaLStar = separation.deltaLStar,
                            limitations = buildList {
                                add("Captured color is device- and illumination-dependent; compare within this session.")
                                if (!result.quality.passed) {
                                    add("Controlled-pair gates failed; apparent face shift was withheld.")
                                    result.quality.failures.take(5).forEach {
                                        add(it.code.name.lowercase().replace('_', ' '))
                                    }
                                }
                            },
                        ),
                    )
                    saved = true
                },
                enabled = !saved,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DrapeCoral),
            ) { Text(if (saved) "Saved to Drape Records" else "Save evidence + skin sample") }
            Spacer(Modifier.height(9.dp))
            Button(onClick = onRetake, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Compare another fabric") }
            Text(
                "No seasonal label, beauty score, or health inference is produced.",
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
            )
        }
    }
}

@Composable
private fun EvidenceMetric(color: Color, label: String, value: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(12.dp).background(color, CircleShape))
            Column(Modifier.padding(start = 13.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = color)
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Medium)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            }
        }
    }
}

private fun calculateOutcome(
    opening: List<FrameReading>,
    fabric: List<FrameReading>,
    closing: List<FrameReading>,
    control: CameraControlStatus,
): SessionOutcome? {
    val openingFace = aggregateFace(opening) ?: return null
    val fabricFace = aggregateFace(fabric) ?: return null
    val closingFace = aggregateFace(closing) ?: return null
    val fabricLab = fabric.mapNotNull(FrameReading::fabric).takeIf(List<*>::isNotEmpty)?.let(::medianLab) ?: return null
    val all = opening + fabric + closing
    val luminanceCv = baselineLuminanceCoefficientOfVariation(
        opening = opening.map(FrameReading::faceLuminance),
        closing = closing.map(FrameReading::faceLuminance),
    )
    val temporalDelta = fabric.mapNotNull { it.face?.skin }
        .map { ColorDifference.ciede2000(it, fabricFace.skin) }
        .median()
    val closingDrift = ColorDifference.ciede2000(openingFace.skin, closingFace.skin)
    val metrics = CaptureQualityMetrics(
        faceYawDegrees = worstAbsolutePhaseMedian(
            opening.map(FrameReading::yawDegrees),
            fabric.map(FrameReading::yawDegrees),
            closing.map(FrameReading::yawDegrees),
        ),
        facePitchDegrees = worstAbsolutePhaseMedian(
            opening.map(FrameReading::pitchDegrees),
            fabric.map(FrameReading::pitchDegrees),
            closing.map(FrameReading::pitchDegrees),
        ),
        faceRollDegrees = worstAbsolutePhaseMedian(
            opening.map(FrameReading::rollDegrees),
            fabric.map(FrameReading::rollDegrees),
            closing.map(FrameReading::rollDegrees),
        ),
        faceScaleChangeFraction = maximumPhaseScaleChangeFraction(
            opening.map(FrameReading::faceScale),
            fabric.map(FrameReading::faceScale),
            closing.map(FrameReading::faceScale),
        ),
        clippedPixelFraction = measurementRoiClippedFraction(
            openingCheekFractions = opening.map(FrameReading::clippedPixelFraction),
            fabricCheekFractions = fabric.map(FrameReading::clippedPixelFraction),
            closingCheekFractions = closing.map(FrameReading::clippedPixelFraction),
            fabricFractions = fabric.map(FrameReading::fabricClippedPixelFraction),
        ),
        ambientLightCoefficientOfVariation = luminanceCv,
        acceptedFrames = fabric.size,
        temporalRoiMedianDeltaE = temporalDelta,
        openingClosingBaselineDeltaE = closingDrift,
        neutralExpression = all.all(FrameReading::neutralExpression),
        eyesOpen = all.all(FrameReading::eyesOpen),
        occlusionFree = all.all(FrameReading::occlusionFree),
        sharpEnough = all.all(FrameReading::sharpEnough),
        flickerDetected = luminanceCv?.let { it > 0.06 } ?: false,
        exposureControlled = control.exposureLocked,
        whiteBalanceControlled = control.whiteBalanceLocked,
        fabricRegionValid = fabric.all(FrameReading::fabricRegionValid),
    )
    val quality = QualityGateEvaluator.evaluate(metrics)
    val vector = ContrastCalculator.calculate(
        baseline = openingFace,
        drape = fabricFace,
        fabric = fabricLab,
        allowApparentFaceShift = quality.passed,
    )
    val evidence = EvidencePolicy.highestSupported(
        inputs = EvidenceInputs(
            hasOpeningAndClosingBaseline = true,
            faceAndFabricInSameScene = true,
            hasSeparateFaceAndProductPhotos = false,
            inputEligibleForMeasurement = true,
        ),
        quality = quality,
    )
    fun medianHex(colors: List<com.drapeproof.core.color.SrgbColor>): String {
        val red = colors.map { it.red }.medianInt()
        val green = colors.map { it.green }.medianInt()
        val blue = colors.map { it.blue }.medianInt()
        return "#%02X%02X%02X".format(red, green, blue)
    }
    val fabricHex = medianHex(fabric.mapNotNull(FrameReading::fabricSrgb))
    val skinHex = medianHex(fabric.mapNotNull(FrameReading::skinSrgb))
    return SessionOutcome(vector, quality, evidence, skinHex, fabricHex, control)
}

private fun List<Double>.median(): Double {
    if (isEmpty()) return 0.0
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
}

private fun List<Int>.medianInt(): Int {
    if (isEmpty()) return 0
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

private fun Double.format(decimals: Int): String = String.format(Locale.US, "%1$.${decimals}f", this)

private fun EvidenceTier.displayName(): String = when (this) {
    EvidenceTier.CONTROLLED_PAIR -> "Controlled pair"
    EvidenceTier.SAME_SCENE -> "Same-scene evidence"
    EvidenceTier.SEPARATE_PHOTO_ESTIMATE -> "Separate-photo estimate"
    EvidenceTier.PREVIEW_ONLY -> "Preview only"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
