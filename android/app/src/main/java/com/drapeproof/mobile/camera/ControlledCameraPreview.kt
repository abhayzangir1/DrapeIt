@file:OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)

package com.drapeproof.mobile.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class CameraControlStatus(
    val exposureLocked: Boolean,
    val whiteBalanceLocked: Boolean,
    val detail: String,
) {
    val fullyControlled: Boolean get() = exposureLocked && whiteBalanceLocked
}

@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
class DrapeCameraControls internal constructor(
    private val camera2Control: Camera2CameraControl,
    private val aeLockAvailable: Boolean,
    private val awbLockAvailable: Boolean,
) {
    fun lock(onComplete: (CameraControlStatus) -> Unit) {
        val builder = CaptureRequestOptions.Builder()
        if (aeLockAvailable) builder.setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, true)
        if (awbLockAvailable) builder.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, true)
        val future = camera2Control.setCaptureRequestOptions(builder.build())
        future.addListener(
            {
                val status = runCatching {
                    future.get()
                    CameraControlStatus(
                        exposureLocked = aeLockAvailable,
                        whiteBalanceLocked = awbLockAvailable,
                        detail = when {
                            aeLockAvailable && awbLockAvailable -> "Exposure and white balance locked"
                            aeLockAvailable -> "Exposure locked; white balance lock unavailable"
                            awbLockAvailable -> "White balance locked; exposure lock unavailable"
                            else -> "This camera cannot lock exposure or white balance"
                        },
                    )
                }.getOrElse {
                    CameraControlStatus(false, false, "Camera controls could not be locked")
                }
                onComplete(status)
            },
            Runnable::run,
        )
    }

    fun unlock() {
        camera2Control.clearCaptureRequestOptions()
    }
}

@Composable
@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
fun ControlledCameraPreview(
    modifier: Modifier = Modifier,
    onFrame: (FrameReading) -> Unit,
    onControlsReady: (DrapeCameraControls) -> Unit,
    onCameraError: (String) -> Unit,
    onPreviewReady: ((PreviewView) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnControlsReady by rememberUpdatedState(onControlsReady)
    val currentOnError by rememberUpdatedState(onCameraError)
    val currentOnPreviewReady by rememberUpdatedState(onPreviewReady)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }.also { view ->
            currentOnPreviewReady?.invoke(view)
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analyzer by remember { mutableStateOf<FaceFrameAnalyzer?>(null) }

    AndroidView(factory = { previewView }, modifier = modifier.fillMaxSize())

    LaunchedEffect(lifecycleOwner) {
        try {
            val provider = ProcessCameraProvider.getInstance(context).await(context.compatMainExecutor)
            cameraProvider = provider
            val frameAnalyzer = FaceFrameAnalyzer(
                context = context.applicationContext,
                onReading = { reading -> context.compatMainExecutor.execute { currentOnFrame(reading) } },
                onFailure = { message -> context.compatMainExecutor.execute { currentOnError(message) } },
            )
            analyzer = frameAnalyzer
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }
            val analysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, frameAnalyzer) }

            provider.unbindAll()
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                analysis,
            )
            val info = Camera2CameraInfo.from(camera.cameraInfo)
            val aeAvailable = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) == true
            val awbAvailable = info.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) == true
            currentOnControlsReady(
                DrapeCameraControls(
                    camera2Control = Camera2CameraControl.from(camera.cameraControl),
                    aeLockAvailable = aeAvailable,
                    awbLockAvailable = awbAvailable,
                ),
            )
        } catch (error: Throwable) {
            currentOnError(error.message ?: "The front camera could not start")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            analyzer?.close()
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
}

private val android.content.Context.compatMainExecutor get() = ContextCompat.getMainExecutor(this)

private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(
    executor: java.util.concurrent.Executor,
): T = suspendCancellableCoroutine { continuation ->
    addListener(
        {
            try {
                continuation.resume(get())
            } catch (error: Throwable) {
                continuation.resumeWithException(error)
            }
        },
        executor,
    )
    continuation.invokeOnCancellation { cancel(true) }
}
