package com.aipet.brain.brain.recognition.model

// MobileFaceNet cosine similarity scores are similarity measures, not calibrated probabilities.
// The final known-person decision is intentionally conservative so recognition only accepts
// strong matches and keeps debug/UI thresholds separate from the recognizer threshold.
const val DEFAULT_RECOGNITION_THRESHOLD: Float = 0.80f

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
