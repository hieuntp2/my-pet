package com.aipet.brain.brain.state

import android.util.Log
import com.aipet.brain.brain.events.BrainStateChangedEventPayload
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.logic.audio.AudioMeaningfulStimulusPolicy
import com.aipet.brain.brain.logic.audio.AudioStimulusMapper
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class BrainTransitionReason {
    PERSON_RECOGNIZED,
    PERSON_UNKNOWN,
    INACTIVITY_TIMEOUT,
    STIMULUS_WAKE,
    PETTED_WHILE_CURIOUS,
    DEBUG_FORCE_SLEEP,
    DEBUG_FORCE_WAKE
}

class BrainInteractionLoop(
    private val eventBus: EventBus,
    private val brainStateStore: BrainStateStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val inactivityThresholdMs: Long = DEFAULT_INACTIVITY_THRESHOLD_MS,
    private val audioStimulusMapper: AudioStimulusMapper = AudioStimulusMapper(),
    private val audioMeaningfulStimulusPolicy: AudioMeaningfulStimulusPolicy = AudioMeaningfulStimulusPolicy()
) {
    private val lock = Mutex()
    private var lastMeaningfulStimulusAtMs: Long = nowProvider()

    suspend fun observeEventsAndApplyTransitions() {
        eventBus.observe().collect { event ->
            when (event.type) {
                EventType.PERSON_SEEN_RECORDED -> {
                    val payload = PersonSeenEventPayload.fromJson(event.payloadJson)
                    if (payload != null) {
                        recordMeaningfulStimulus(payload.seenAtMs)
                        transitionTo(
                            targetState = BrainState.HAPPY,
                            reason = BrainTransitionReason.PERSON_RECOGNIZED,
                            timestampMs = payload.seenAtMs
                        )
                    }
                }

                EventType.PERSON_UNKNOWN_DETECTED -> {
                    recordMeaningfulStimulus(event.timestampMs)
                    transitionTo(
                        targetState = BrainState.CURIOUS,
                        reason = BrainTransitionReason.PERSON_UNKNOWN,
                        timestampMs = event.timestampMs
                    )
                }

                EventType.OBJECT_DETECTED -> {
                    recordMeaningfulStimulus(event.timestampMs)
                    wakeIfSleeping(
                        timestampMs = event.timestampMs,
                        reason = BrainTransitionReason.STIMULUS_WAKE
                    )
                }

                EventType.USER_INTERACTED_PET -> {
                    recordMeaningfulStimulus(event.timestampMs)
                    val current = brainStateStore.currentSnapshot().currentState
                    when {
                        current == BrainState.CURIOUS -> {
                            transitionTo(
                                targetState = BrainState.HAPPY,
                                reason = BrainTransitionReason.PETTED_WHILE_CURIOUS,
                                timestampMs = event.timestampMs
                            )
                        }

                        current == BrainState.SLEEPY -> {
                            transitionTo(
                                targetState = BrainState.CURIOUS,
                                reason = BrainTransitionReason.STIMULUS_WAKE,
                                timestampMs = event.timestampMs
                            )
                        }
                    }
                }

                EventType.SOUND_DETECTED,
                EventType.VOICE_ACTIVITY_STARTED,
                EventType.VOICE_ACTIVITY_ENDED,
                EventType.WAKE_WORD_DETECTED,
                EventType.KEYWORD_DETECTED -> {
                    handleAudioStimulusForInactivity(event)
                }

                else -> Unit
            }
        }
    }

    suspend fun runInactivityMonitor(
        checkIntervalMs: Long = DEFAULT_INACTIVITY_CHECK_INTERVAL_MS
    ) {
        while (currentCoroutineContext().isActive) {
            delay(checkIntervalMs)
            checkInactivity(nowProvider())
        }
    }

    suspend fun checkInactivity(currentTimeMs: Long = nowProvider()) {
        lock.withLock {
            val currentState = brainStateStore.currentSnapshot().currentState
            if (currentState == BrainState.SLEEPY) {
                return
            }
            if (currentTimeMs - lastMeaningfulStimulusAtMs < inactivityThresholdMs) {
                return
            }
            performTransitionLocked(
                targetState = BrainState.SLEEPY,
                reason = BrainTransitionReason.INACTIVITY_TIMEOUT,
                timestampMs = currentTimeMs
            )
        }
    }

    suspend fun forceSleep(timestampMs: Long = nowProvider()) {
        transitionTo(
            targetState = BrainState.SLEEPY,
            reason = BrainTransitionReason.DEBUG_FORCE_SLEEP,
            timestampMs = timestampMs
        )
    }

    suspend fun forceWake(timestampMs: Long = nowProvider()) {
        recordMeaningfulStimulus(timestampMs)
        transitionTo(
            targetState = BrainState.CURIOUS,
            reason = BrainTransitionReason.DEBUG_FORCE_WAKE,
            timestampMs = timestampMs
        )
    }

    private suspend fun wakeIfSleeping(
        timestampMs: Long,
        reason: BrainTransitionReason
    ) {
        val current = brainStateStore.currentSnapshot().currentState
        if (current != BrainState.SLEEPY) {
            return
        }
        transitionTo(
            targetState = BrainState.CURIOUS,
            reason = reason,
            timestampMs = timestampMs
        )
    }

    private suspend fun recordMeaningfulStimulus(timestampMs: Long) {
        lock.withLock {
            if (timestampMs > lastMeaningfulStimulusAtMs) {
                lastMeaningfulStimulusAtMs = timestampMs
            }
        }
    }

    private suspend fun handleAudioStimulusForInactivity(event: EventEnvelope) {
        val stimulus = audioStimulusMapper.map(event) ?: run {
            Log.w(
                TAG,
                "Ignored audio event for inactivity reset due to invalid payload. eventType=${event.type.name}, " +
                    "eventId=${event.eventId}"
            )
            return
        }
        val meaningfulStimulus = audioMeaningfulStimulusPolicy.evaluate(stimulus)
        if (meaningfulStimulus == null) {
            Log.d(
                TAG,
                "Audio stimulus ignored for inactivity reset. source=${stimulus.sourceEventType.name}, " +
                    "stimulusTs=${stimulus.timestampMs}"
            )
            return
        }
        recordMeaningfulStimulus(meaningfulStimulus.timestampMs)
        Log.d(
            TAG,
            "Meaningful audio stimulus accepted. source=${meaningfulStimulus.sourceEventType.name}, " +
                "reason=${meaningfulStimulus.reason}, stimulusTs=${meaningfulStimulus.timestampMs}"
        )
        wakeIfSleeping(
            timestampMs = meaningfulStimulus.timestampMs,
            reason = BrainTransitionReason.STIMULUS_WAKE
        )
    }

    private suspend fun transitionTo(
        targetState: BrainState,
        reason: BrainTransitionReason,
        timestampMs: Long
    ) {
        lock.withLock {
            performTransitionLocked(
                targetState = targetState,
                reason = reason,
                timestampMs = timestampMs
            )
        }
    }

    private suspend fun performTransitionLocked(
        targetState: BrainState,
        reason: BrainTransitionReason,
        timestampMs: Long
    ) {
        val fromState = brainStateStore.currentSnapshot().currentState
        val changed = brainStateStore.setState(
            targetState = targetState,
            timestampMs = timestampMs
        )
        if (!changed) {
            return
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.BRAIN_STATE_CHANGED,
                payloadJson = BrainStateChangedEventPayload(
                    fromState = fromState,
                    toState = targetState,
                    reason = reason.name,
                    changedAtMs = timestampMs
                ).toJson(),
                timestampMs = timestampMs
            )
        )
    }

    companion object {
        private const val TAG = "BrainInteractionLoop"
        private const val DEFAULT_INACTIVITY_THRESHOLD_MS = 60_000L
        private const val DEFAULT_INACTIVITY_CHECK_INTERVAL_MS = 1_000L
    }
}
