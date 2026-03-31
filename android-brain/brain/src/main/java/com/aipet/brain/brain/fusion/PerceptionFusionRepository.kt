package com.aipet.brain.brain.fusion

import kotlinx.coroutines.flow.StateFlow

/**
 * The unified perception snapshot consumed by the Behavior Engine.
 * This is the single "world model" the brain reasons over.
 */
data class PerceptionFusionSnapshot(
    val presence: PresenceState = PresenceState.DEFAULT,
    val socialContext: SocialContext = SocialContext.DEFAULT,
    val attentionContext: AttentionContext = AttentionContext.DEFAULT,
    val audioContext: AudioContext = AudioContext.DEFAULT,
    val voiceContext: VoiceContext = VoiceContext.DEFAULT,
    val touchContext: TouchContext = TouchContext.DEFAULT,
    val environmentContext: EnvironmentContext = EnvironmentContext.DEFAULT,
    val recentPerceptionSummary: RecentPerceptionSummary = RecentPerceptionSummary.DEFAULT,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = PerceptionFusionSnapshot()
    }
}

/**
 * Contract for the component that holds and exposes the fused perception snapshot.
 */
interface PerceptionFusionRepository {
    /** Observe the latest fusion snapshot as a reactive flow. */
    fun observeFusionSnapshot(): StateFlow<PerceptionFusionSnapshot>

    /** Get the current snapshot synchronously. */
    fun getCurrentFusionSnapshot(): PerceptionFusionSnapshot
}
