package com.aipet.brain.app.audio

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.perception.audio.model.VadState
import kotlinx.coroutines.flow.StateFlow

/**
 * Debug-facing snapshot derived from the shared audio runtime path.
 *
 * This state is produced by the same listener boundary that publishes
 * audio lifecycle/sound events into EventBus, so the Audio Debug UI can
 * inspect runtime and event sequencing without introducing a parallel path.
 */
data class AudioRuntimeDebugState(
    val vadState: VadState? = null,
    val lastSoundEventType: EventType? = null,
    val lastSoundEventTimestampMs: Long? = null,
    val lastSoundEventSequence: Long = 0L
)

interface AudioRuntimeDebugStateProvider {
    fun currentRuntimeDebugState(): AudioRuntimeDebugState

    fun observeRuntimeDebugState(): StateFlow<AudioRuntimeDebugState>
}

