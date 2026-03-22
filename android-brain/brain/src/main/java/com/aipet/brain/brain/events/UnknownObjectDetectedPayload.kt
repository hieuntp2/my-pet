package com.aipet.brain.brain.events

data class UnknownObjectDetectedPayload(
    val canonicalLabel: String,
    val confidence: Float,
    val detectedAtMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 128) {
            append("{")
            append("\"canonicalLabel\":\"").append(canonicalLabel.toJsonEscaped()).append("\",")
            append("\"confidence\":").append(confidence).append(",")
            append("\"detectedAtMs\":").append(detectedAtMs)
            append("}")
        }
    }

    companion object {
        private val LABEL_PATTERN = Regex("\"canonicalLabel\"\\s*:\\s*\"([^\"]+)\"")
        private val CONFIDENCE_PATTERN = Regex("\"confidence\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val DETECTED_AT_PATTERN = Regex("\"detectedAtMs\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): UnknownObjectDetectedPayload? {
            val label = LABEL_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val confidence = CONFIDENCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val detectedAtMs = DETECTED_AT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            return UnknownObjectDetectedPayload(
                canonicalLabel = label,
                confidence = confidence,
                detectedAtMs = detectedAtMs
            )
        }
    }
}
