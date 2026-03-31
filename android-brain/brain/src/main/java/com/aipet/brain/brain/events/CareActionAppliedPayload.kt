package com.aipet.brain.brain.events

data class CareActionAppliedPayload(
    val actionType: String,
    val appliedAtMs: Long,
    val resultingEmotion: String,
    val didDiminishingReturnsApply: Boolean,
    val isRepairAction: Boolean,
    val burstCount: Int,
    val comfortDelta: Int,
    val trustDelta: Int,
    val socialDelta: Int,
    val moodValenceDelta: Int,
    val bondDelta: Int,
    val neglectStreakDelta: Int
) {
    fun toJson(): String {
        return buildString(capacity = 360) {
            append("{")
            append("\"actionType\":\"").append(actionType.toJsonEscaped()).append("\",")
            append("\"appliedAtMs\":").append(appliedAtMs).append(",")
            append("\"resultingEmotion\":\"").append(resultingEmotion.toJsonEscaped()).append("\",")
            append("\"diminishingReturns\":").append(didDiminishingReturnsApply).append(",")
            append("\"repairAction\":").append(isRepairAction).append(",")
            append("\"burstCount\":").append(burstCount).append(",")
            append("\"comfortDelta\":").append(comfortDelta).append(",")
            append("\"trustDelta\":").append(trustDelta).append(",")
            append("\"socialDelta\":").append(socialDelta).append(",")
            append("\"moodValenceDelta\":").append(moodValenceDelta).append(",")
            append("\"bondDelta\":").append(bondDelta).append(",")
            append("\"neglectStreakDelta\":").append(neglectStreakDelta)
            append("}")
        }
    }
}
