package com.aipet.brain.perception.audio

import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.VadResult

/**
 * Detector plugin contract for VAD-light in the audio perception pipeline.
 *
 * Implementations can be swapped (energy threshold, WebRTC VAD, etc.) while keeping a
 * stable interface for downstream behavior/event layers.
 */
interface VoiceActivityDetector {
    val detectorId: String

    fun processFrame(frame: AudioFrame, energyMetrics: AudioEnergyMetrics): VadResult

    fun reset()
}
