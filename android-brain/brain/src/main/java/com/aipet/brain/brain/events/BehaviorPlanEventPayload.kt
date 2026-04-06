package com.aipet.brain.brain.events

data class BehaviorPlanEventPayload(
    val planId: String,
    val intention: String,
    val debugLabel: String,
    val animationFamily: String,
    val interruptPriority: Int,
    val source: String,
    val timestampMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 220) {
            append("{")
            append("\"planId\":\"").append(planId.toJsonEscaped()).append("\",")
            append("\"intention\":\"").append(intention.toJsonEscaped()).append("\",")
            append("\"debugLabel\":\"").append(debugLabel.toJsonEscaped()).append("\",")
            append("\"animationFamily\":\"").append(animationFamily.toJsonEscaped()).append("\",")
            append("\"interruptPriority\":").append(interruptPriority).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\",")
            append("\"timestampMs\":").append(timestampMs)
            append("}")
        }
    }
}

data class PetIntentionChangedEventPayload(
    val previousIntention: String? = null,
    val newIntention: String,
    val planId: String,
    val reason: String,
    val timestampMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 180) {
            append("{")
            if (!previousIntention.isNullOrBlank()) {
                append("\"previousIntention\":\"").append(previousIntention.toJsonEscaped()).append("\",")
            }
            append("\"newIntention\":\"").append(newIntention.toJsonEscaped()).append("\",")
            append("\"planId\":\"").append(planId.toJsonEscaped()).append("\",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\",")
            append("\"timestampMs\":").append(timestampMs)
            append("}")
        }
    }
}
