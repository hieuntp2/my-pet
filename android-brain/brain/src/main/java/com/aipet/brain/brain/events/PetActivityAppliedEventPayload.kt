package com.aipet.brain.brain.events

data class PetActivityAppliedEventPayload(
    val activityType: String,
    val actedAtMs: Long,
    val reason: String,
    val resultingMood: String,
    val energyDelta: Int,
    val hungerDelta: Int,
    val sleepinessDelta: Int,
    val socialDelta: Int,
    val bondDelta: Int
) {
    fun toJson(): String {
        return buildString(capacity = 220) {
            append("{")
            append("\"activityType\":\"").append(activityType.toJsonEscaped()).append("\",")
            append("\"actedAtMs\":").append(actedAtMs).append(",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\",")
            append("\"resultingMood\":\"").append(resultingMood.toJsonEscaped()).append("\",")
            append("\"energyDelta\":").append(energyDelta).append(",")
            append("\"hungerDelta\":").append(hungerDelta).append(",")
            append("\"sleepinessDelta\":").append(sleepinessDelta).append(",")
            append("\"socialDelta\":").append(socialDelta).append(",")
            append("\"bondDelta\":").append(bondDelta)
            append("}")
        }
    }

    companion object {
        private val ACTIVITY_TYPE_PATTERN = Regex("""\"activityType\"\s*:\s*\"([^\"]+)\"""")
        private val ACTED_AT_MS_PATTERN = Regex("""\"actedAtMs\"\s*:\s*(-?\d+)""")
        private val REASON_PATTERN = Regex("""\"reason\"\s*:\s*\"([^\"]+)\"""")
        private val RESULTING_MOOD_PATTERN = Regex("""\"resultingMood\"\s*:\s*\"([^\"]+)\"""")
        private val ENERGY_DELTA_PATTERN = Regex("""\"energyDelta\"\s*:\s*(-?\d+)""")
        private val HUNGER_DELTA_PATTERN = Regex("""\"hungerDelta\"\s*:\s*(-?\d+)""")
        private val SLEEPINESS_DELTA_PATTERN = Regex("""\"sleepinessDelta\"\s*:\s*(-?\d+)""")
        private val SOCIAL_DELTA_PATTERN = Regex("""\"socialDelta\"\s*:\s*(-?\d+)""")
        private val BOND_DELTA_PATTERN = Regex("""\"bondDelta\"\s*:\s*(-?\d+)""")

        fun fromJson(payloadJson: String): PetActivityAppliedEventPayload? {
            val activityType = ACTIVITY_TYPE_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.ifBlank { null } ?: return null
            val actedAtMs = ACTED_AT_MS_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toLongOrNull() ?: return null
            val reason = REASON_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.ifBlank { null } ?: return null
            val resultingMood = RESULTING_MOOD_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.ifBlank { null } ?: return null
            val energyDelta = ENERGY_DELTA_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
            val hungerDelta = HUNGER_DELTA_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
            val sleepinessDelta = SLEEPINESS_DELTA_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
            val socialDelta = SOCIAL_DELTA_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
            val bondDelta = BOND_DELTA_PATTERN.find(payloadJson)
                ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
            return PetActivityAppliedEventPayload(
                activityType = activityType,
                actedAtMs = actedAtMs,
                reason = reason,
                resultingMood = resultingMood,
                energyDelta = energyDelta,
                hungerDelta = hungerDelta,
                sleepinessDelta = sleepinessDelta,
                socialDelta = socialDelta,
                bondDelta = bondDelta
            )
        }
    }
}
