package com.aipet.brain.brain.events

data class PersonSeenEventPayload(
    val personId: String,
    val seenAtMs: Long,
    val seenCount: Int,
    val isOwner: Boolean,
    val source: String,
    val profileId: String? = null,
    val observationId: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 192) {
            append("{")
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"seenAtMs\":").append(seenAtMs).append(",")
            append("\"seenCount\":").append(seenCount).append(",")
            append("\"isOwner\":").append(isOwner).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\"")
            if (!profileId.isNullOrBlank()) {
                append(",\"profileId\":\"").append(profileId.toJsonEscaped()).append("\"")
            }
            if (!observationId.isNullOrBlank()) {
                append(",\"observationId\":\"").append(observationId.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }

    companion object {
        private val PERSON_ID_PATTERN = Regex("\"personId\"\\s*:\\s*\"([^\"]+)\"")
        private val SEEN_AT_PATTERN = Regex("\"seenAtMs\"\\s*:\\s*(-?\\d+)")
        private val SEEN_COUNT_PATTERN = Regex("\"seenCount\"\\s*:\\s*(-?\\d+)")
        private val IS_OWNER_PATTERN = Regex("\"isOwner\"\\s*:\\s*(true|false)")
        private val SOURCE_PATTERN = Regex("\"source\"\\s*:\\s*\"([^\"]+)\"")
        private val PROFILE_ID_PATTERN = Regex("\"profileId\"\\s*:\\s*\"([^\"]+)\"")
        private val OBSERVATION_ID_PATTERN = Regex("\"observationId\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): PersonSeenEventPayload? {
            val personId = PERSON_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val seenAtMs = SEEN_AT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val seenCount = SEEN_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val isOwner = IS_OWNER_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toBooleanStrictOrNull()
                ?: return null
            val source = SOURCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val profileId = PROFILE_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val observationId = OBSERVATION_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }

            return PersonSeenEventPayload(
                personId = personId,
                seenAtMs = seenAtMs,
                seenCount = seenCount,
                isOwner = isOwner,
                source = source,
                profileId = profileId,
                observationId = observationId
            )
        }
    }
}
