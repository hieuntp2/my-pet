package com.aipet.brain.brain.events

data class ObjectDetectedEventPayload(
    val objectId: String? = null,
    val label: String,
    val confidence: Float,
    val detectedAtMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 128) {
            append("{")
            if (!objectId.isNullOrBlank()) {
                append("\"objectId\":\"").append(objectId.toJsonEscaped()).append("\",")
            }
            append("\"label\":\"").append(label.toJsonEscaped()).append("\",")
            append("\"confidence\":").append(confidence).append(",")
            append("\"detectedAtMs\":").append(detectedAtMs)
            append("}")
        }
    }

    companion object {
        private val OBJECT_ID_PATTERN = Regex("\"objectId\"\\s*:\\s*\"([^\"]+)\"")
        private val LABEL_PATTERN = Regex("\"label\"\\s*:\\s*\"([^\"]+)\"")
        private val CONFIDENCE_PATTERN = Regex("\"confidence\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val DETECTED_AT_PATTERN = Regex("\"detectedAtMs\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): ObjectDetectedEventPayload? {
            val objectId = OBJECT_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
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

            return ObjectDetectedEventPayload(
                objectId = objectId,
                label = label,
                confidence = confidence,
                detectedAtMs = detectedAtMs
            )
        }
    }
}
