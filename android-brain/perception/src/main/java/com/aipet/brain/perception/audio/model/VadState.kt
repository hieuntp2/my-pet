package com.aipet.brain.perception.audio.model

/**
 * Normalized VAD-light states emitted by detectors in the audio pipeline.
 */
enum class VadState {
    SILENT,
    SOUND_PRESENT,
    VOICE_LIKELY
}
