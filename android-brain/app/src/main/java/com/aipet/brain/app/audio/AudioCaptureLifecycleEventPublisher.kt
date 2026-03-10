package com.aipet.brain.app.audio

import android.os.SystemClock
import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioCaptureEventPayload
import com.aipet.brain.brain.events.audio.KeywordDetectionEventInput
import com.aipet.brain.brain.events.audio.KeywordDetectionEventKind
import com.aipet.brain.brain.events.audio.KeywordDetectionEventMapper
import com.aipet.brain.brain.events.audio.SoundEnergyPayload
import com.aipet.brain.brain.events.audio.VoiceActivityPayload
import com.aipet.brain.brain.events.audio.VoiceActivityState
import com.aipet.brain.perception.audio.AudioCaptureLifecycleEvent
import com.aipet.brain.perception.audio.AudioCaptureLifecycleListener
import com.aipet.brain.perception.audio.AudioEnergyMetricsListener
import com.aipet.brain.perception.audio.AudioKeywordDetectionListener
import com.aipet.brain.perception.audio.AudioVadResultListener
import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordDetectionType
import com.aipet.brain.perception.audio.model.VadResult
import com.aipet.brain.perception.audio.model.VadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class AudioCaptureLifecycleEventPublisher(
    private val eventBus: EventBus,
    private val coroutineScope: CoroutineScope,
    private val energyEventIntervalMs: Long = DEFAULT_ENERGY_EVENT_INTERVAL_MS,
    private val soundDetectedEventIntervalMs: Long = DEFAULT_SOUND_DETECTED_EVENT_INTERVAL_MS
) : AudioCaptureLifecycleListener,
    AudioEnergyMetricsListener,
    AudioVadResultListener,
    AudioKeywordDetectionListener,
    AudioRuntimeDebugStateProvider {
    init {
        require(energyEventIntervalMs > 0L) { "energyEventIntervalMs must be > 0" }
        require(soundDetectedEventIntervalMs > 0L) { "soundDetectedEventIntervalMs must be > 0" }
    }

    override fun onCaptureStarted(event: AudioCaptureLifecycleEvent) {
        resetEnergyRateLimit()
        resetVadEventState()
        resetRuntimeDebugState()
        publishLifecycleEvent(
            eventType = EventType.AUDIO_CAPTURE_STARTED,
            event = event
        )
    }

    override fun onCaptureStopped(event: AudioCaptureLifecycleEvent) {
        publishLifecycleEvent(
            eventType = EventType.AUDIO_CAPTURE_STOPPED,
            event = event
        )
        resetEnergyRateLimit()
        resetVadEventState()
        resetRuntimeDebugState()
    }

    override fun onEnergyMetrics(metrics: AudioEnergyMetrics) {
        cacheLatestEnergyMetrics(metrics)
        if (!shouldPublishEnergyMetrics(frameTimestampMs = metrics.frameTimestampMs)) {
            return
        }

        val eventTimestampMs = frameElapsedRealtimeToEpochMs(
            frameTimestampMs = metrics.frameTimestampMs
        )
        updateLatestEnergyMetrics(
            metrics = metrics,
            timestampMs = eventTimestampMs
        )
        val payloadJson = SoundEnergyPayload(
            rms = metrics.rms,
            peak = metrics.peak,
            smoothedEnergy = metrics.smoothed,
            timestamp = eventTimestampMs
        ).toJson()

        coroutineScope.launch {
            try {
                eventBus.publish(
                    EventEnvelope.create(
                        type = EventType.SOUND_ENERGY_CHANGED,
                        timestampMs = eventTimestampMs,
                        payloadJson = payloadJson
                    )
                )
                Log.d(
                    TAG,
                    "Published ${EventType.SOUND_ENERGY_CHANGED.name}. " +
                        "rms=${formatMetric(metrics.rms)} " +
                        "peak=${formatMetric(metrics.peak)} " +
                        "smoothed=${formatMetric(metrics.smoothed)} " +
                        "sampleRate=${metrics.sampleRate} " +
                        "sampleCount=${metrics.sampleCount}"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish ${EventType.SOUND_ENERGY_CHANGED.name}.", error)
            }
        }
    }

    override fun onVadResult(result: VadResult) {
        updateVadState(result.state)
        val transition = resolveVadTransition(result) ?: return
        val eventTimestampMs = frameElapsedRealtimeToEpochMs(
            frameTimestampMs = result.timestampMs
        )
        if (transition.publishSoundDetected) {
            publishSoundDetected(
                result = result,
                eventTimestampMs = eventTimestampMs,
                metrics = transition.metricsSnapshot
            )
        }
        if (transition.publishVoiceStarted) {
            publishVoiceActivity(
                eventType = EventType.VOICE_ACTIVITY_STARTED,
                state = VoiceActivityState.STARTED,
                vadResult = result,
                eventTimestampMs = eventTimestampMs
            )
        }
        if (transition.publishVoiceEnded) {
            publishVoiceActivity(
                eventType = EventType.VOICE_ACTIVITY_ENDED,
                state = VoiceActivityState.ENDED,
                vadResult = result,
                eventTimestampMs = eventTimestampMs
            )
        }
    }

    private fun publishLifecycleEvent(
        eventType: EventType,
        event: AudioCaptureLifecycleEvent
    ) {
        val payloadJson = AudioCaptureEventPayload(
            sampleRate = event.sampleRate,
            frameSize = event.frameSize,
            channelCount = event.channelCount,
            timestamp = event.timestampMs
        ).toJson()

        coroutineScope.launch {
            try {
                eventBus.publish(
                    EventEnvelope.create(
                        type = eventType,
                        timestampMs = event.timestampMs,
                        payloadJson = payloadJson
                    )
                )
                Log.d(
                    TAG,
                    "Published ${eventType.name}. " +
                        "sampleRate=${event.sampleRate} frameSize=${event.frameSize} " +
                        "channelCount=${event.channelCount}"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish ${eventType.name}.", error)
            }
        }
    }

    private fun shouldPublishEnergyMetrics(frameTimestampMs: Long): Boolean {
        synchronized(energyRateLimitLock) {
            val previousTimestamp = lastPublishedEnergyFrameTimestampMs
            if (previousTimestamp != null) {
                val elapsedSincePreviousMs = frameTimestampMs - previousTimestamp
                if (elapsedSincePreviousMs in 0 until energyEventIntervalMs) {
                    return false
                }
            }
            lastPublishedEnergyFrameTimestampMs = frameTimestampMs
            return true
        }
    }

    private fun cacheLatestEnergyMetrics(metrics: AudioEnergyMetrics) {
        synchronized(vadTransitionLock) {
            latestEnergyMetrics = metrics
        }
    }

    private fun resolveVadTransition(result: VadResult): VadTransitionDecision? {
        synchronized(vadTransitionLock) {
            val previousState = lastVadState
            val currentState = result.state
            val enteredSoundActive = !previousState.isSoundActive() && currentState.isSoundActive()
            val publishVoiceStarted = previousState != VadState.VOICE_LIKELY &&
                currentState == VadState.VOICE_LIKELY
            val publishVoiceEnded = previousState == VadState.VOICE_LIKELY &&
                currentState != VadState.VOICE_LIKELY
            val publishSoundDetected = enteredSoundActive &&
                shouldPublishSoundDetectedLocked(frameTimestampMs = result.timestampMs)
            val snapshot = if (publishSoundDetected) latestEnergyMetrics else null
            lastVadState = currentState
            if (!publishSoundDetected && !publishVoiceStarted && !publishVoiceEnded) {
                return null
            }
            return VadTransitionDecision(
                publishSoundDetected = publishSoundDetected,
                publishVoiceStarted = publishVoiceStarted,
                publishVoiceEnded = publishVoiceEnded,
                metricsSnapshot = snapshot
            )
        }
    }

    private fun shouldPublishSoundDetectedLocked(frameTimestampMs: Long): Boolean {
        val previousTimestamp = lastPublishedSoundDetectedFrameTimestampMs
        if (previousTimestamp != null) {
            val elapsedSincePreviousMs = frameTimestampMs - previousTimestamp
            if (elapsedSincePreviousMs in 0 until soundDetectedEventIntervalMs) {
                Log.d(
                    TAG,
                    "Suppressed ${EventType.SOUND_DETECTED.name} within cooldown. " +
                        "elapsedMs=$elapsedSincePreviousMs intervalMs=$soundDetectedEventIntervalMs"
                )
                return false
            }
        }
        lastPublishedSoundDetectedFrameTimestampMs = frameTimestampMs
        return true
    }

    private fun publishSoundDetected(
        result: VadResult,
        eventTimestampMs: Long,
        metrics: AudioEnergyMetrics?
    ) {
        val activeMetrics = metrics
        if (activeMetrics == null) {
            Log.w(TAG, "Skipped ${EventType.SOUND_DETECTED.name}: metrics snapshot unavailable.")
            return
        }
        val payloadJson = SoundEnergyPayload(
            rms = activeMetrics.rms,
            peak = activeMetrics.peak,
            smoothedEnergy = activeMetrics.smoothed,
            timestamp = eventTimestampMs,
            kind = result.state.name
        ).toJson()
        coroutineScope.launch {
            try {
                eventBus.publish(
                    EventEnvelope.create(
                        type = EventType.SOUND_DETECTED,
                        timestampMs = eventTimestampMs,
                        payloadJson = payloadJson
                    )
                )
                updateLastSoundEvent(
                    eventType = EventType.SOUND_DETECTED,
                    timestampMs = eventTimestampMs
                )
                Log.d(
                    TAG,
                    "Published ${EventType.SOUND_DETECTED.name}. " +
                        "state=${result.state.name} " +
                        "rms=${formatMetric(activeMetrics.rms)} " +
                        "peak=${formatMetric(activeMetrics.peak)} " +
                        "smoothed=${formatMetric(activeMetrics.smoothed)}"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish ${EventType.SOUND_DETECTED.name}.", error)
            }
        }
    }

    private fun publishVoiceActivity(
        eventType: EventType,
        state: VoiceActivityState,
        vadResult: VadResult,
        eventTimestampMs: Long
    ) {
        // VAD-light is energy based; VOICE_LIKELY is a heuristic voice proxy, not a speech classifier.
        val payloadJson = VoiceActivityPayload(
            state = state,
            confidence = vadResult.confidence,
            timestamp = eventTimestampMs,
            vadState = vadResult.state.name
        ).toJson()
        coroutineScope.launch {
            try {
                eventBus.publish(
                    EventEnvelope.create(
                        type = eventType,
                        timestampMs = eventTimestampMs,
                        payloadJson = payloadJson
                    )
                )
                updateLastSoundEvent(
                    eventType = eventType,
                    timestampMs = eventTimestampMs
                )
                Log.d(
                    TAG,
                    "Published ${eventType.name}. " +
                        "vadState=${vadResult.state.name} " +
                        "confidence=${String.format(java.util.Locale.US, "%.2f", vadResult.confidence)}"
                )
            } catch (error: Throwable) {
                Log.e(TAG, "Failed to publish ${eventType.name}.", error)
            }
        }
    }

    private fun resetEnergyRateLimit() {
        synchronized(energyRateLimitLock) {
            lastPublishedEnergyFrameTimestampMs = null
        }
    }

    private fun resetVadEventState() {
        synchronized(vadTransitionLock) {
            latestEnergyMetrics = null
            lastVadState = VadState.SILENT
            lastPublishedSoundDetectedFrameTimestampMs = null
        }
    }

    private fun resetRuntimeDebugState() {
        synchronized(runtimeDebugStateLock) {
            soundEventSequence = 0L
            runtimeDebugState.value = AudioRuntimeDebugState()
        }
    }

    private fun updateVadState(vadState: VadState) {
        synchronized(runtimeDebugStateLock) {
            val current = runtimeDebugState.value
            if (current.vadState == vadState) {
                return
            }
            runtimeDebugState.value = current.copy(vadState = vadState)
        }
    }

    private fun updateLatestEnergyMetrics(
        metrics: AudioEnergyMetrics,
        timestampMs: Long
    ) {
        synchronized(runtimeDebugStateLock) {
            runtimeDebugState.value = runtimeDebugState.value.copy(
                latestEnergySmoothed = metrics.smoothed,
                latestEnergyRms = metrics.rms,
                latestEnergyPeak = metrics.peak,
                latestEnergyTimestampMs = timestampMs
            )
        }
    }

    override fun onKeywordDetected(result: KeywordDetectionResult) {
        Log.d(
            TAG,
            "Received keyword detection. " +
                "keywordId=${result.keywordId} " +
                "keywordText=${result.keywordText ?: "-"} " +
                "confidence=${String.format(java.util.Locale.US, "%.3f", result.confidence)} " +
                "engine=${result.engineName} " +
                "kind=${result.detectionType.name}"
        )
        val eventKind = when (result.detectionType) {
            KeywordDetectionType.WAKE_WORD -> KeywordDetectionEventKind.WAKE_WORD
            KeywordDetectionType.KEYWORD -> KeywordDetectionEventKind.KEYWORD
        }
        val eventInput = KeywordDetectionEventInput(
            kind = eventKind,
            keywordId = result.keywordId,
            keywordText = result.keywordText,
            confidence = result.confidence,
            timestampMs = result.timestampMs,
            engine = result.engineName
        )
        val eventEnvelope = keywordDetectionEventMapper.toEventEnvelope(eventInput)
        if (eventEnvelope == null) {
            Log.w(
                TAG,
                "Skipped keyword detection event publication due to invalid mapping input. " +
                    "keywordId=${result.keywordId} type=${result.detectionType.name} engine=${result.engineName}"
            )
            return
        }
        publishKeywordDetectionEvent(eventEnvelope, result)
    }

    private fun publishKeywordDetectionEvent(
        eventEnvelope: EventEnvelope,
        result: KeywordDetectionResult
    ) {
        coroutineScope.launch {
            try {
                eventBus.publish(eventEnvelope)
                updateLastSoundEvent(
                    eventType = eventEnvelope.type,
                    timestampMs = eventEnvelope.timestampMs
                )
                Log.d(
                    TAG,
                    "Published ${eventEnvelope.type.name}. " +
                        "keywordId=${result.keywordId} " +
                        "keywordText=${result.keywordText ?: "-"} " +
                        "confidence=${String.format(java.util.Locale.US, "%.3f", result.confidence)} " +
                        "engine=${result.engineName} " +
                        "kind=${result.detectionType.name}"
                )
            } catch (error: Throwable) {
                Log.e(
                    TAG,
                    "Failed to publish ${eventEnvelope.type.name}. " +
                        "keywordId=${result.keywordId} engine=${result.engineName}",
                    error
                )
            }
        }
    }

    private fun updateLastSoundEvent(
        eventType: EventType,
        timestampMs: Long
    ) {
        synchronized(runtimeDebugStateLock) {
            soundEventSequence += 1L
            runtimeDebugState.value = runtimeDebugState.value.copy(
                lastSoundEventType = eventType,
                lastSoundEventTimestampMs = timestampMs,
                lastSoundEventSequence = soundEventSequence
            )
        }
    }

    private fun frameElapsedRealtimeToEpochMs(frameTimestampMs: Long): Long {
        val nowElapsedRealtimeMs = SystemClock.elapsedRealtime()
        val nowEpochMs = System.currentTimeMillis()
        val frameAgeMs = nowElapsedRealtimeMs - frameTimestampMs
        return if (frameAgeMs >= 0L) {
            nowEpochMs - frameAgeMs
        } else {
            nowEpochMs
        }
    }

    private fun formatMetric(value: Double): String {
        return String.format(java.util.Locale.US, "%.4f", value)
    }

    private fun VadState.isSoundActive(): Boolean {
        return this != VadState.SILENT
    }

    private val energyRateLimitLock = Any()
    private var lastPublishedEnergyFrameTimestampMs: Long? = null
    private val vadTransitionLock = Any()
    private var latestEnergyMetrics: AudioEnergyMetrics? = null
    private var lastVadState: VadState = VadState.SILENT
    private var lastPublishedSoundDetectedFrameTimestampMs: Long? = null
    private val runtimeDebugStateLock = Any()
    private val runtimeDebugState = MutableStateFlow(AudioRuntimeDebugState())
    private var soundEventSequence: Long = 0L
    private val keywordDetectionEventMapper = KeywordDetectionEventMapper()

    override fun currentRuntimeDebugState(): AudioRuntimeDebugState = runtimeDebugState.value

    override fun observeRuntimeDebugState(): StateFlow<AudioRuntimeDebugState> = runtimeDebugState

    private data class VadTransitionDecision(
        val publishSoundDetected: Boolean,
        val publishVoiceStarted: Boolean,
        val publishVoiceEnded: Boolean,
        val metricsSnapshot: AudioEnergyMetrics?
    )

    companion object {
        private const val TAG = "AudioCaptureEventPublisher"
        private const val DEFAULT_ENERGY_EVENT_INTERVAL_MS = 300L
        private const val DEFAULT_SOUND_DETECTED_EVENT_INTERVAL_MS = 1_000L
    }
}
