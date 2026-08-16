package com.drapeproof.mobile.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight soft-gain luxury sound and tactile haptic manager.
 * Plays ultra-soft, subtle feedback sounds (volume ~10-15%) that feel premium and minimal.
 */
object SoundEffectManager {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var vibrator: Vibrator? = null

    fun init(context: Context? = null) {
        if (context != null) {
            runCatching {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
            }
        }
    }

    /**
     * Plays an ultra-soft mechanical camera shutter click (attenuated to 16% volume) with tactile feedback.
     */
    fun playShutter(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 950f, durationMs = 28, volume = 0.16f)
            }
        }
        triggerHaptic(view, durationMs = 35, hapticConstant = HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Plays a delicate tactile tap / swatch switch tick.
     */
    fun playTap(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 1200f, durationMs = 15, volume = 0.10f)
            }
        }
        triggerHaptic(view, durationMs = 12, hapticConstant = HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Plays a soft, delicate tactile focus tick.
     */
    fun playFocus(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 1400f, durationMs = 18, volume = 0.12f)
            }
        }
        triggerHaptic(view, durationMs = 15, hapticConstant = HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * Plays a warm luxury confirmation chime with confirm haptic pulse.
     */
    fun playSuccess(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 880f, durationMs = 35, volume = 0.14f)
            }
        }
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
        triggerHaptic(view, durationMs = 30, hapticConstant = constant)
    }

    private fun triggerHaptic(view: View?, durationMs: Long, hapticConstant: Int) {
        var performed = false
        if (view != null) {
            runCatching {
                performed = view.performHapticFeedback(hapticConstant)
            }
        }
        if (!performed && vibrator?.hasVibrator() == true) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        }
    }

    private fun playPcmClick(frequencyHz: Float, durationMs: Int, volume: Float) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            // Decaying envelope for natural mechanical click
            val envelope = (1.0f - (i.toFloat() / numSamples))
            val sample = (sin(2.0 * Math.PI * frequencyHz * t) * envelope * volume * Short.MAX_VALUE).toInt()
            buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        Thread.sleep(durationMs.toLong() + 20)
        track.stop()
        track.release()
    }

    fun release() {
        // Nothing to release
    }
}
