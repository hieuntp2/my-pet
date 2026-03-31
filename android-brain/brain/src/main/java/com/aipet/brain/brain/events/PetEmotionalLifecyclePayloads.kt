package com.aipet.brain.brain.events

import com.aipet.brain.brain.pet.AbsenceBucket
import com.aipet.brain.brain.pet.PetGreetingStyle

data class PetStateDecayAppliedPayload(
    val appliedAtMs: Long,
    val absenceBucket: String,
    val elapsedMinutes: Long,
    val energyDelta: Int,
    val hungerDelta: Int,
    val sleepinessDelta: Int,
    val socialDelta: Int,
    val comfortDelta: Int,
    val stimulationDelta: Int,
    val trustDelta: Int
) {
    fun toJson(): String {
        return buildString(capacity = 300) {
            append("{")
            append("\"appliedAtMs\":").append(appliedAtMs).append(",")
            append("\"absenceBucket\":\"").append(absenceBucket.toJsonEscaped()).append("\",")
            append("\"elapsedMinutes\":").append(elapsedMinutes).append(",")
            append("\"energyDelta\":").append(energyDelta).append(",")
            append("\"hungerDelta\":").append(hungerDelta).append(",")
            append("\"sleepinessDelta\":").append(sleepinessDelta).append(",")
            append("\"socialDelta\":").append(socialDelta).append(",")
            append("\"comfortDelta\":").append(comfortDelta).append(",")
            append("\"stimulationDelta\":").append(stimulationDelta).append(",")
            append("\"trustDelta\":").append(trustDelta)
            append("}")
        }
    }
}

data class PetGreetedV2Payload(
    val greetedAtMs: Long,
    val absenceBucket: String,
    val greetingStyle: String,
    val emotion: String,
    val reason: String,
    val message: String,
    val bondScore: Int,
    val trustScore: Int,
    val neglectStreak: Int
) {
    fun toJson(): String {
        return buildString(capacity = 400) {
            append("{")
            append("\"greetedAtMs\":").append(greetedAtMs).append(",")
            append("\"absenceBucket\":\"").append(absenceBucket.toJsonEscaped()).append("\",")
            append("\"greetingStyle\":\"").append(greetingStyle.toJsonEscaped()).append("\",")
            append("\"emotion\":\"").append(emotion.toJsonEscaped()).append("\",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\",")
            append("\"message\":\"").append(message.toJsonEscaped()).append("\",")
            append("\"bondScore\":").append(bondScore).append(",")
            append("\"trustScore\":").append(trustScore).append(",")
            append("\"neglectStreak\":").append(neglectStreak)
            append("}")
        }
    }
}
