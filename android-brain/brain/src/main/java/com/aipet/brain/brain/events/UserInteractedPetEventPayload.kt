package com.aipet.brain.brain.events

import com.aipet.brain.brain.interaction.PetInteractionType

data class UserInteractedPetEventPayload(
    val interactedAtMs: Long,
    val source: String,
    val interactionType: String,
    val resultingMood: String,
    val socialDelta: Int,
    val bondDelta: Int,
    val feedbackText: String
) {
    fun toJson(): String {
        return buildString(capacity = 220) {
            append("{")
            append("\"interactedAtMs\":").append(interactedAtMs).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\",")
            append("\"interactionType\":\"").append(interactionType.toJsonEscaped()).append("\",")
            append("\"resultingMood\":\"").append(resultingMood.toJsonEscaped()).append("\",")
            append("\"socialDelta\":").append(socialDelta).append(",")
            append("\"bondDelta\":").append(bondDelta).append(",")
            append("\"feedbackText\":\"").append(feedbackText.toJsonEscaped()).append("\"")
            append("}")
        }
    }

    companion object {
        private val INTERACTED_AT_MS_PATTERN = Regex("\\"interactedAtMs\\"\\s*:\\s*(-?\\d+)")
        private val SOURCE_PATTERN = Regex("\\"source\\"\\s*:\\s*\\"([^\\"]+)\\"")
        private val INTERACTION_TYPE_PATTERN = Regex("\\"interactionType\\"\\s*:\\s*\\"([^\\"]+)\\"")
        private val RESULTING_MOOD_PATTERN = Regex("\\"resultingMood\\"\\s*:\\s*\\"([^\\"]+)\\"")
        private val SOCIAL_DELTA_PATTERN = Regex("\\"socialDelta\\"\\s*:\\s*(-?\\d+)")
        private val BOND_DELTA_PATTERN = Regex("\\"bondDelta\\"\\s*:\\s*(-?\\d+)")
        private val FEEDBACK_TEXT_PATTERN = Regex("\\"feedbackText\\"\\s*:\\s*\\"([^\\"]+)\\"")

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
            val resultingMood = RESULTING_MOOD_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val socialDelta = SOCIAL_DELTA_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val bondDelta = BOND_DELTA_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val feedbackText = FEEDBACK_TEXT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            return UserInteractedPetEventPayload(
                interactedAtMs = interactedAtMs,
                source = source,
                interactionType = interactionType,
                resultingMood = resultingMood,
                socialDelta = socialDelta,
                bondDelta = bondDelta,
                feedbackText = feedbackText
            )
        }
    }
}
