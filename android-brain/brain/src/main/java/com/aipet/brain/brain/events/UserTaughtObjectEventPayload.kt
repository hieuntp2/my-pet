package com.aipet.brain.brain.events

data class UserTaughtObjectEventPayload(
    val objectId: String,
    val objectName: String,
    val taughtAtMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 192) {
            append("{")
            append("\"objectId\":\"").append(objectId.toJsonEscaped()).append("\",")
            append("\"objectName\":\"").append(objectName.toJsonEscaped()).append("\",")
            append("\"taughtAtMs\":").append(taughtAtMs)
            append("}")
        }
    }
}
