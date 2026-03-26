package com.aipet.brain.app.audio

import com.aipet.brain.brain.events.audio.AudioIntent
import java.util.Locale

internal class LocalAudioIntentMapper {
    fun normalize(rawText: String): String {
        val collapsed = rawText
            .lowercase(Locale.US)
            .trim()
            .replace(Regex("\\s+"), " ")
        if (collapsed.isBlank()) {
            return ""
        }

        return when (collapsed) {
            "wake up" -> "wakeup"
            "learn a person" -> "learn person"
            else -> collapsed
                .replace(" wake up ", " wakeup ")
                .replace("learn a person", "learn person")
                // The three variants below are unreachable via the Vosk grammar
                // (restricted to the canonical phrase list). Reserved for a future
                // non-Vosk input source that may produce these alias forms.
                .replace("learn an person", "learn person")
                .replace("learn a object", "learn object")
                .replace("learn an object", "learn object")
                .trim()
        }
    }

    fun mapToIntent(normalizedText: String): AudioIntent {
        return when (normalizedText) {
            "wakeup" -> AudioIntent.WAKE_UP
            "learn person" -> AudioIntent.LEARN_PERSON
            "learn object" -> AudioIntent.LEARN_OBJECT
            "play random" -> AudioIntent.PLAY_RANDOM
            else -> AudioIntent.UNKNOWN
        }
    }
}
