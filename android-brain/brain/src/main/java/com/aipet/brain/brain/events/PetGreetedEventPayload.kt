package com.aipet.brain.brain.events

data class PetGreetedEventPayload(
    val greetedAtMs: Long,
    val emotion: String,
    val reason: String,
    val message: String
) {
    fun toJson(): String {
        return buildString(capacity = 160) {
            append("{")
            append("\"greetedAtMs\":").append(greetedAtMs).append(",")
            append("\"emotion\":\"").append(emotion.toJsonEscaped()).append("\",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\",")
            append("\"message\":\"").append(message.toJsonEscaped()).append("\"")
            append("}")
        }
    }

    companion object {
        private val GREETED_AT_MS_PATTERN = Regex("""\"greetedAtMs\"\s*:\s*(-?\d+)""")
        private val EMOTION_PATTERN = Regex("""\"emotion\"\s*:\s*\"([^\"]+)\"""")
        private val REASON_PATTERN = Regex("""\"reason\"\s*:\s*\"([^\"]+)\"""")
        private val MESSAGE_PATTERN = Regex("""\"message\"\s*:\s*\"([^\"]+)\"""")

        fun fromJson(payloadJson: String): PetGreetedEventPayload? {
            val greetedAtMs = GREETED_AT_MS_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val emotion = EMOTION_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val reason = REASON_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val message = MESSAGE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            return PetGreetedEventPayload(
                greetedAtMs = greetedAtMs,
                emotion = emotion,
                reason = reason,
                message = message
            )
        }
    }
}
