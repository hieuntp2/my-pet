package com.aipet.brain.brain.events

import com.aipet.brain.brain.traits.TraitsSnapshot

data class TraitsUpdatedEventPayload(
    val changedAtMs: Long,
    val changedFields: List<String>,
    val previous: TraitsSnapshot,
    val current: TraitsSnapshot
) {
    fun toJson(): String {
        val changedFieldsJson = changedFields.joinToString(
            prefix = "[",
            postfix = "]",
            separator = ","
        ) { field ->
            "\"${field.toJsonEscaped()}\""
        }
        return buildString(capacity = 512) {
            append("{")
            append("\"changedAtMs\":").append(changedAtMs).append(",")
            append("\"changedFields\":").append(changedFieldsJson).append(",")
            append("\"previous\":").append(previous.toJson()).append(",")
            append("\"current\":").append(current.toJson())
            append("}")
        }
    }
}

private fun TraitsSnapshot.toJson(): String {
    return buildString(capacity = 224) {
        append("{")
        append("\"snapshotId\":\"").append(snapshotId.toJsonEscaped()).append("\",")
        append("\"capturedAtMs\":").append(capturedAtMs).append(",")
        append("\"curiosity\":").append(curiosity).append(",")
        append("\"sociability\":").append(sociability).append(",")
        append("\"energy\":").append(energy).append(",")
        append("\"patience\":").append(patience).append(",")
        append("\"boldness\":").append(boldness)
        append("}")
    }
}
