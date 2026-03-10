package com.aipet.brain.brain.events.audio

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.toJsonEscaped

/**
 * Keyword detection event contracts for the shared EventBus/EventStore pipeline.
 *
 * Runtime keyword adapters should map real detections through these contracts.
 */
enum class KeywordDetectionEventKind {
    WAKE_WORD,
    KEYWORD
}

data class KeywordDetectionPayload(
    val keywordId: String,
    val keywordText: String? = null,
    val confidence: Float,
    val timestamp: Long,
    val engine: String,
    val kind: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 196) {
            append("{")
            append("\"keywordId\":\"").append(keywordId.toJsonEscaped()).append("\",")
            append("\"confidence\":").append(confidence).append(",")
            append("\"timestamp\":").append(timestamp).append(",")
            append("\"engine\":\"").append(engine.toJsonEscaped()).append("\"")
            if (!keywordText.isNullOrBlank()) {
                append(",\"keywordText\":\"").append(keywordText.toJsonEscaped()).append("\"")
            }
            if (!kind.isNullOrBlank()) {
                append(",\"kind\":\"").append(kind.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }

    companion object {
        private val KEYWORD_ID_PATTERN = Regex("\"keywordId\"\\s*:\\s*\"([^\"]+)\"")
        private val KEYWORD_TEXT_PATTERN = Regex("\"keywordText\"\\s*:\\s*\"([^\"]+)\"")
        private val CONFIDENCE_PATTERN = Regex("\"confidence\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")
        private val ENGINE_PATTERN = Regex("\"engine\"\\s*:\\s*\"([^\"]+)\"")
        private val KIND_PATTERN = Regex("\"kind\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): KeywordDetectionPayload? {
            val keywordId = KEYWORD_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
                ?: return null
            val confidence = CONFIDENCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val engine = ENGINE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
                ?: return null
            val keywordText = KEYWORD_TEXT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val kind = KIND_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            return KeywordDetectionPayload(
                keywordId = keywordId,
                keywordText = keywordText,
                confidence = confidence,
                timestamp = timestamp,
                engine = engine,
                kind = kind
            )
        }
    }
}

data class KeywordDetectionEventInput(
    val kind: KeywordDetectionEventKind,
    val keywordId: String,
    val keywordText: String? = null,
    val confidence: Float,
    val timestampMs: Long,
    val engine: String
)

/**
 * Central mapping path from structured keyword detections to EventEnvelope contracts.
 */
class KeywordDetectionEventMapper {
    fun toEventEnvelope(input: KeywordDetectionEventInput): EventEnvelope? {
        if (input.keywordId.isBlank()) {
            return null
        }
        if (input.engine.isBlank()) {
            return null
        }
        if (!input.confidence.isFinite() || input.confidence !in 0f..1f) {
            return null
        }
        if (input.timestampMs <= 0L) {
            return null
        }

        val payload = KeywordDetectionPayload(
            keywordId = input.keywordId,
            keywordText = input.keywordText?.trim()?.ifBlank { null },
            confidence = input.confidence,
            timestamp = input.timestampMs,
            engine = input.engine,
            kind = input.kind.name
        )
        return EventEnvelope.create(
            type = resolveEventType(input.kind),
            timestampMs = input.timestampMs,
            payloadJson = payload.toJson()
        )
    }

    fun toEventType(kind: KeywordDetectionEventKind): EventType {
        return resolveEventType(kind)
    }

    private fun resolveEventType(kind: KeywordDetectionEventKind): EventType {
        return when (kind) {
            KeywordDetectionEventKind.WAKE_WORD -> EventType.WAKE_WORD_DETECTED
            KeywordDetectionEventKind.KEYWORD -> EventType.KEYWORD_DETECTED
        }
    }
}
