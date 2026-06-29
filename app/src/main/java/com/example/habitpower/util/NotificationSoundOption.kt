package com.example.habitpower.util

import android.media.ToneGenerator
import android.media.AudioManager

/**
 * Preset notification sounds the user can choose from.
 * Each entry is played using [ToneGenerator] so no raw audio files are needed.
 */
enum class NotificationSoundOption(
    val id: String,
    val displayName: String,
    val toneType: Int,
    val durationMs: Int
) {
    SHORT_BEEP(
        id = "short_beep",
        displayName = "Short Beep",
        toneType = ToneGenerator.TONE_PROP_BEEP,
        durationMs = 300
    ),
    DOUBLE_BEEP(
        id = "double_beep",
        displayName = "Double Beep",
        toneType = ToneGenerator.TONE_PROP_BEEP2,
        durationMs = 600
    ),
    ALERT(
        id = "alert",
        displayName = "Alert",
        toneType = ToneGenerator.TONE_PROP_NACK,
        durationMs = 500
    ),
    POSITIVE(
        id = "positive",
        displayName = "Positive Ding",
        toneType = ToneGenerator.TONE_PROP_ACK,
        durationMs = 400
    ),
    LONG_BEEP(
        id = "long_beep",
        displayName = "Long Beep",
        toneType = ToneGenerator.TONE_CDMA_HIGH_L,
        durationMs = 1000
    );

    companion object {
        fun fromId(id: String): NotificationSoundOption =
            entries.firstOrNull { it.id == id } ?: SHORT_BEEP
    }
}

/**
 * Plays a [NotificationSoundOption] using [ToneGenerator].
 * Safe to call from any thread; ToneGenerator is created fresh each call.
 */
object SoundPlayer {
    fun play(option: NotificationSoundOption, volumePercent: Int = 80) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volumePercent)
            toneGen.startTone(option.toneType, option.durationMs)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (option.durationMs + 200).toLong())
        } catch (_: Exception) {}
    }

    fun playById(id: String, volumePercent: Int = 80) {
        if (id == "none") return
        play(NotificationSoundOption.fromId(id), volumePercent)
    }

    /** Plays habit completion tone if sound is enabled. */
    fun playHabitCompletion(enabled: Boolean, soundId: String = NotificationSoundOption.POSITIVE.id) {
        if (enabled) playById(soundId, 70)
    }

    /**
     * Unified session-end bell: a double tone that signals the end of any Focus
     * session (Pomodoro, Meditation, Chant). Distinct from single-tap completion tones.
     */
    fun playSessionEnd(volumePercent: Int = 85) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, volumePercent)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 500)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 700)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    toneGen.release()
                }, 1000)
            }, 700)
        } catch (_: Exception) {}
    }
}
