package com.aipet.brain.brain.recognition.model

// MobileFaceNet cosine similarity between the same person's embeddings typically
// falls in the 0.5–0.9 range. 0.55 gives a good balance for few-shot enrollment
// (1–3 captured samples) without excessive false positives.
const val DEFAULT_RECOGNITION_THRESHOLD: Float = 0.55f

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
