package com.aipet.brain.perception.audio.keyword

import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordSpotterState

/**
 * Offline keyword spotting extension point for the audio perception pipeline.
 *
 * This defines contracts only. Real engine integration is intentionally deferred.
 * Current audio MVP behavior (energy/VAD/playback) remains unchanged.
 */
interface KeywordSpotter {
    val spotterId: String

    fun start()

    fun stop()

    fun reset()

    fun state(): KeywordSpotterState

    fun processFrame(frame: AudioFrame): KeywordDetectionResult?

    fun setDetectionListener(listener: KeywordDetectionListener?)

    fun release() {
        stop()
    }
}

fun interface KeywordDetectionListener {
    fun onKeywordDetected(result: KeywordDetectionResult)
}
