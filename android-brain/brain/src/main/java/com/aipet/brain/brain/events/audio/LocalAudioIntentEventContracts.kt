package com.aipet.brain.brain.events.audio

import com.aipet.brain.brain.events.toJsonEscaped

enum class AudioIntent {
    WAKE_UP,
    LEARN_PERSON,
    LEARN_OBJECT,
    PLAY_RANDOM,
    UNKNOWN
}

data class LocalAudioIntentEvent(
    val intent: AudioIntent,
    val confidence: Float,
    val rawText: String
) {
    fun toJson(): String {
        return buildString(capacity = 160) {
            append("{")
            append("\"intent\":\"").append(intent.name).append("\",")
            append("\"confidence\":").append(confidence).append(",")
            append("\"rawText\":\"").append(rawText.toJsonEscaped()).append("\"")
            append("}")
        }
    }

    companion object {
        private val INTENT_PATTERN = Regex("\\\"intent\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        private val CONFIDENCE_PATTERN = Regex("\\\"confidence\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val RAW_TEXT_PATTERN = Regex("\\\"rawText\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")

        fun fromJson(payloadJson: String): LocalAudioIntentEvent? {
            val intent = INTENT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { rawIntent ->
                    AudioIntent.entries.firstOrNull { intent -> intent.name == rawIntent }
                }
                ?: return null
            val confidence = CONFIDENCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val rawText = RAW_TEXT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?: return null
            if (rawText.isBlank()) {
                return null
            }
            if (!confidence.isFinite()) {
                return null
            }
            return LocalAudioIntentEvent(
                intent = intent,
                confidence = confidence,
                rawText = rawText
            )
        }
    }
}
