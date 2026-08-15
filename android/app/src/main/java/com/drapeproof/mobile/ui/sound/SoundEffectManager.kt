package com.drapeproof.mobile.ui.sound

import android.content.Context
import android.media.AudioManager
import android.media.MediaActionSound
import android.media.ToneGenerator
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Lightweight native sound and haptic feedback manager.
 * Uses Android system MediaActionSound for zero-latency camera shutter and focus sounds.
 */
object SoundEffectManager {
    private var mediaActionSound: MediaActionSound? = null

    fun init() {
        runCatching {
            if (mediaActionSound == null) {
                mediaActionSound = MediaActionSound().apply {
                    load(MediaActionSound.SHUTTER_CLICK)
                    load(MediaActionSound.FOCUS_COMPLETE)
                }
            }
        }
    }

    fun playShutter(view: View? = null) {
        runCatching {
            if (mediaActionSound == null) {
                mediaActionSound = MediaActionSound().apply {
                    load(MediaActionSound.SHUTTER_CLICK)
                }
            }
            mediaActionSound?.play(MediaActionSound.SHUTTER_CLICK)
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    fun playFocus(view: View? = null) {
        runCatching {
            if (mediaActionSound == null) {
                mediaActionSound = MediaActionSound().apply {
                    load(MediaActionSound.FOCUS_COMPLETE)
                }
            }
            mediaActionSound?.play(MediaActionSound.FOCUS_COMPLETE)
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    fun playSuccess(view: View? = null) {
        runCatching {
            playFocus(view)
        }
        runCatching {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    fun release() {
        runCatching {
            mediaActionSound?.release()
            mediaActionSound = null
        }
    }
}
