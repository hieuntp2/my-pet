package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.evolution.domain.MemoryEpisode

/**
 * Slowly evolves personality traits based on repeated interaction patterns.
 *
 * Changes are deliberately small and bounded to prevent wild swings.
 * Traits only drift given enough repeated evidence across multiple episodes.
 *
 * Design: each episode contributes a tiny impulse vector. The engine
 * applies a weighted sum and clamps to valid range.
 */
class PersonalityEvolutionEngine(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Update traits based on a batch of recent episodes.
     * @param current   Current personality traits.
     * @param episodes  Recent episodes (5–20 recommended).
     * @return Updated traits (may be unchanged if evidence is insufficient).
     */
    fun evolve(current: PetTrait, episodes: List<MemoryEpisode>): PetTrait {
        if (episodes.size < MIN_EPISODES_FOR_DRIFT) return current

        val playfulImpulse = computePlayfulImpulse(episodes)
        val socialImpulse = computeSocialImpulse(episodes)
        val curiousImpulse = computeCuriousImpulse(episodes)
        val attachmentImpulse = computeAttachmentImpulse(episodes)
        val patienceImpulse = computePatienceImpulse(episodes)
        val energyImpulse = computeEnergyImpulse(episodes)
        // lazy := inverse of sociability trend (pet becomes less social after repeated neglect)
        val lazyImpulse = computeLazyImpulse(episodes)

        val now = nowProvider()
        return current.copy(
            playful = (current.playful + playfulImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            social = (current.social + socialImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            curious = (current.curious + curiousImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            attachment = (current.attachment + attachmentImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            patience = (current.patience + patienceImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            energyProfile = (current.energyProfile + energyImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            lazy = (current.lazy + lazyImpulse * DRIFT_RATE).coerceIn(0f, 1f),
            updatedAt = now
        )
    }

    private fun computePlayfulImpulse(episodes: List<MemoryEpisode>): Float {
        val playEpisodes = episodes.count { it.userBehaviorTag == "PLAY" || "play" in it.interactionTypesJson.lowercase() }
        return if (playEpisodes >= 3) +1f else if (playEpisodes == 0) -0.5f else 0f
    }

    private fun computeSocialImpulse(episodes: List<MemoryEpisode>): Float {
        val avgCare = episodes.map { it.careScoreDelta }.average().toFloat()
        return when {
            avgCare > 8 -> +1f
            avgCare < -8 -> -1f
            else -> 0f
        }
    }

    private fun computeCuriousImpulse(episodes: List<MemoryEpisode>): Float {
        val highStimulation = episodes.count { it.interactionCount >= 5 && it.durationMs > 3 * 60_000L }
        return if (highStimulation >= 2) +0.5f else 0f
    }

    private fun computeAttachmentImpulse(episodes: List<MemoryEpisode>): Float {
        val routineReturns = episodes.count { it.reunionType == "ROUTINE_RETURN" }
        val neglectEpisodes = episodes.count { it.neglectSignal }
        return when {
            routineReturns >= 3 -> +1f
            neglectEpisodes >= 2 -> -0.5f
            else -> 0f
        }
    }

    private fun computePatienceImpulse(episodes: List<MemoryEpisode>): Float {
        // Consistent medium-length sessions increase patience
        val mediumSessions = episodes.count { it.durationMs in 5 * 60_000L..20 * 60_000L }
        return if (mediumSessions >= 3) +0.5f else 0f
    }

    private fun computeEnergyImpulse(episodes: List<MemoryEpisode>): Float {
        val highInteraction = episodes.count { it.interactionCount >= 4 }
        return when {
            highInteraction >= 3 -> +0.5f
            highInteraction == 0 -> -0.3f
            else -> 0f
        }
    }

    private fun computeLazyImpulse(episodes: List<MemoryEpisode>): Float {
        val neglectEpisodes = episodes.count { it.neglectSignal }
        val shortSessions = episodes.count { it.durationMs < 2 * 60_000L }
        return when {
            neglectEpisodes >= 3 -> +0.5f
            shortSessions == episodes.size && episodes.size >= 3 -> +0.3f
            else -> 0f
        }
    }

    private companion object {
        /** Minimum episodes before any trait drift is applied. */
        const val MIN_EPISODES_FOR_DRIFT = 4
        /** Per-episode weight applied to trait impulse. Very small to prevent chaos. */
        const val DRIFT_RATE = 0.005f
    }
}
