package com.example.habitpower.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TtsPlayer {
    private var tts: TextToSpeech? = null
    private var ready = false

    fun init(context: Context) {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_${System.currentTimeMillis()}")
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
