package com.aipet.brain.perception.audio

import com.aipet.brain.perception.audio.model.AudioCaptureState
import com.aipet.brain.perception.audio.model.AudioFrame

/**
 * Entry point of the audio perception pipeline:
 * Microphone capture -> AudioFrameSource -> detector plugins -> audio events.
 *
 * Implementations are responsible for streaming PCM frames and reporting capture state,
 * without exposing Android UI dependencies in this contract.
 */
interface AudioFrameSource {
    fun start()

    fun stop()

    fun isRunning(): Boolean

    fun captureState(): AudioCaptureState

    fun setFrameListener(listener: ((AudioFrame) -> Unit)?)
}
