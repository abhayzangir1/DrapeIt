package com.drapeproof.mobile.ui.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.view.HapticFeedbackConstants
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight soft-gain luxury sound and tactile haptic manager.
 * Plays ultra-soft, subtle feedback sounds (volume ~18%) that feel premium rather than abrasive.
 */
object SoundEffectManager {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun init() {
        // Pre-warm audio subsystem
    }

    /**
     * Plays an ultra-soft mechanical camera shutter click (attenuated to 18% volume).
     */
    fun playShutter(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 950f, durationMs = 28, volume = 0.16f)
            }
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
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
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * Plays a warm luxury confirmation chime.
     */
    fun playSuccess(view: View? = null) {
        scope.launch {
            runCatching {
                playPcmClick(frequencyHz = 880f, durationMs = 35, volume = 0.14f)
            }
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
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
