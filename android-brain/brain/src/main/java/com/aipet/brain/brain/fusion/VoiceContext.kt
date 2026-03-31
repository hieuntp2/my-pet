package com.aipet.brain.brain.fusion

/**
 * Semantic type of a parsed voice command, mapped from raw ASR results.
 */
enum class VoiceCommandType {
    DIRECT_INSTRUCTION,
    PRAISE_AFFECTION,
    PLAY_INTENT,
    STOP_INTENT,
    COMFORT_INTENT,
    UNCLEAR
}

/**
 * Voice interaction context fused from VAD, ASR, and command confidence.
 */
data class VoiceContext(
    /** True if voice activity is currently detected. */
    val voiceActivity: Boolean = false,
    /** The type of the most recently parsed command, or null if none. */
    val commandType: VoiceCommandType? = null,
    /** 0–1: confidence level of the parsed command. */
    val commandConfidence: Float = 0f,
    /** True if the command appears addressed to the pet (not ambient speech). */
    val commandAddressedToPet: Boolean = false,
    /** Raw text of last recognized utterance. */
    val lastRawText: String? = null,
    /** When speech was last detected (0 if recently silent). */
    val recentSpeechMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = VoiceContext()
    }
}
