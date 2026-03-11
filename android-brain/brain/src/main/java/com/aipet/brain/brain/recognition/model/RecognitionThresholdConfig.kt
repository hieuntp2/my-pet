package com.aipet.brain.brain.recognition.model

const val DEFAULT_RECOGNITION_THRESHOLD: Float = 0.75f

data class RecognitionThresholdConfig(
    val acceptanceThreshold: Float = DEFAULT_RECOGNITION_THRESHOLD
) {
    val normalizedAcceptanceThreshold: Float
        get() = if (acceptanceThreshold.isFinite()) {
            acceptanceThreshold.coerceIn(-1f, 1f)
        } else {
            DEFAULT_RECOGNITION_THRESHOLD
        }
}
