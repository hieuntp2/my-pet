package com.aipet.brain.brain.recognition.model

enum class RecognitionClassification {
    RECOGNIZED,
    UNKNOWN
}

data class RecognitionResult(
    val classification: RecognitionClassification,
    val bestPersonId: String?,
    val bestScore: Float,
    val threshold: Float,
    val accepted: Boolean,
    val evaluatedCandidates: Int,
    val timestamp: Long
)
