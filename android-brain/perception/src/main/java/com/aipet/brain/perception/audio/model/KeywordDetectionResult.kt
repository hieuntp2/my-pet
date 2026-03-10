package com.aipet.brain.perception.audio.model

/**
 * Runtime keyword spotting output model for the offline audio perception path.
 *
 * This is a pre-AI extension point only. Real keyword engines are integrated in later tasks.
 */
data class KeywordDetectionResult(
    val detectionType: KeywordDetectionType,
    val keywordId: String,
    val keywordText: String? = null,
    val confidence: Float,
    val timestampMs: Long,
    val engineName: String
) {
    init {
        require(keywordId.isNotBlank()) { "keywordId must not be blank" }
        require(confidence.isFinite()) { "confidence must be finite" }
        require(confidence in 0f..1f) { "confidence must be in [0, 1]" }
        require(timestampMs > 0L) { "timestampMs must be > 0" }
        require(engineName.isNotBlank()) { "engineName must not be blank" }
    }
}

enum class KeywordDetectionType {
    WAKE_WORD,
    KEYWORD
}
