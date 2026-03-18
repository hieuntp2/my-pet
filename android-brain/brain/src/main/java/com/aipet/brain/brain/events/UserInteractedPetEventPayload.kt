package com.aipet.brain.brain.events

import com.aipet.brain.brain.interaction.PetInteractionType

data class UserInteractedPetEventPayload(
    val interactedAtMs: Long,
    val source: String,
    val interactionType: String
) {
    fun toJson(): String {
        return buildString(capacity = 120) {
            append("{")
            append("\"interactedAtMs\":").append(interactedAtMs).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\",")
            append("\"interactionType\":\"").append(interactionType.toJsonEscaped()).append("\"")
            append("}")
        }
    }

    companion object {
        private val INTERACTED_AT_MS_PATTERN = Regex("\"interactedAtMs\"\\s*:\\s*(-?\\d+)")
        private val SOURCE_PATTERN = Regex("\"source\"\\s*:\\s*\"([^\"]+)\"")
        private val INTERACTION_TYPE_PATTERN = Regex("\"interactionType\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): UserInteractedPetEventPayload? {
            val interactedAtMs = INTERACTED_AT_MS_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val source = SOURCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val interactionType = INTERACTION_TYPE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: PetInteractionType.TAP.name
            return UserInteractedPetEventPayload(
                interactedAtMs = interactedAtMs,
                source = source,
                interactionType = interactionType
            )
        }
    }
}
