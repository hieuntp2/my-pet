package com.aipet.brain.brain.observations

import java.util.UUID

data class PerceptionObservation(
    val observationId: String,
    val observedAtMs: Long,
    val source: ObservationSource,
    val observationType: ObservationType,
    val note: String? = null
) {
    fun toPayloadJson(): String {
        val escapedObservationId = escapeJsonString(observationId)
        val escapedSource = escapeJsonString(source.name)
        val escapedObservationType = escapeJsonString(observationType.name)
        val noteJsonValue = note?.let { value ->
            "\"${escapeJsonString(value)}\""
        } ?: "null"

        return buildString {
            append("{")
            append("\"observationId\":\"")
            append(escapedObservationId)
            append("\",")
            append("\"observedAtMs\":")
            append(observedAtMs)
            append(",")
            append("\"source\":\"")
            append(escapedSource)
            append("\",")
            append("\"observationType\":\"")
            append(escapedObservationType)
            append("\",")
            append("\"note\":")
            append(noteJsonValue)
            append("}")
        }
    }

    companion object {
        fun create(
            source: ObservationSource,
            observationType: ObservationType,
            note: String? = null,
            observedAtMs: Long = System.currentTimeMillis(),
            observationId: String = UUID.randomUUID().toString()
        ): PerceptionObservation {
            return PerceptionObservation(
                observationId = observationId,
                observedAtMs = observedAtMs,
                source = source,
                observationType = observationType,
                note = note?.trim()?.ifBlank { null }
            )
        }
    }
}

enum class ObservationSource {
    CAMERA,
    DEBUG
}

enum class ObservationType {
    PERSON_LIKE,
    HUMAN_RELATED
}

private fun escapeJsonString(value: String): String {
    val output = StringBuilder(value.length + 8)
    value.forEach { character ->
        when (character) {
            '"' -> output.append("\\\"")
            '\\' -> output.append("\\\\")
            '\b' -> output.append("\\b")
            '\u000C' -> output.append("\\f")
            '\n' -> output.append("\\n")
            '\r' -> output.append("\\r")
            '\t' -> output.append("\\t")
            else -> {
                if (character.code < 0x20) {
                    output.append("\\u")
                    output.append(character.code.toString(16).padStart(4, '0'))
                } else {
                    output.append(character)
                }
            }
        }
    }
    return output.toString()
}
