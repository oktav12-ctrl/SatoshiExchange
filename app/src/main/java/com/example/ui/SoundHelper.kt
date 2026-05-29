package com.example.ui

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

object SoundHelper {
    private var toneGenerator: ToneGenerator? = null
    
    // Globally editable toggle to control sound output easily
    var isAudioEnabled = true

    init {
        try {
            // Using STREAM_MUSIC for predictable volume control with media keys
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Failed to initialize ToneGenerator", e)
        }
    }

    /**
     * Plays a crisp high-frequency bip when a task is successfully completed/clicked.
     */
    fun playTaskCompletion() {
        if (!isAudioEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Tone playback error", e)
        }
    }

    /**
     * Plays a beautiful double acknowledge chime when sync completes successfully.
     */
    fun playSyncComplete() {
        if (!isAudioEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 220)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Tone playback error", e)
        }
    }

    /**
     * Plays a negative failure/alert tone when actions fail or data is purged.
     */
    fun playWarning() {
        if (!isAudioEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            Log.e("SoundHelper", "Tone playback error", e)
        }
    }
}
