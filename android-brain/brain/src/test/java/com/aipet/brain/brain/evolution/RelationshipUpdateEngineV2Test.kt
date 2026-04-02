package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import org.junit.Assert.assertTrue
import org.junit.Test

class RelationshipUpdateEngineV2Test {

    @Test
    fun `update increases bond dimensions for consistent positive routine sessions`() {
        val engine = RelationshipUpdateEngineV2(nowProvider = { 5000L })
        val current = BondStateV2.DEFAULT.copy(
            affection = 0.3f,
            trust = 0.35f,
            dependency = 0.2f,
            stability = 0.3f,
            lastUpdatedAtMs = 1000L
        )
        val recentEpisodes = listOf(
            episode(id = "ep1", careScoreDelta = 9, neglectSignal = false),
            episode(id = "ep2", careScoreDelta = 11, neglectSignal = false)
        )

        val updated = engine.update(
            current = current,
            outcome = RelationshipUpdateEngineV2.SessionOutcome(
                careScoreDelta = 12,
                interactionCount = 6,
                hadNeglect = false,
                reunionType = ReunionType.ROUTINE_RETURN,
                sessionDurationMs = 10 * 60_000L
            ),
            recentEpisodes = recentEpisodes
        )

        assertTrue(updated.affection > current.affection)
        assertTrue(updated.trust > current.trust)
        assertTrue(updated.dependency > current.dependency)
        assertTrue(updated.stability > current.stability)
    }

    @Test
    fun `update decreases trust and affection after neglect-heavy outcome`() {
        val engine = RelationshipUpdateEngineV2(nowProvider = { 6000L })
        val current = BondStateV2.DEFAULT.copy(
            affection = 0.7f,
            trust = 0.7f,
            dependency = 0.6f,
            stability = 0.6f,
            lastUpdatedAtMs = 1000L
        )
        val recentEpisodes = listOf(
            episode(id = "ep3", careScoreDelta = -10, neglectSignal = true),
            episode(id = "ep4", careScoreDelta = 8, neglectSignal = false)
        )

        val updated = engine.update(
            current = current,
            outcome = RelationshipUpdateEngineV2.SessionOutcome(
                careScoreDelta = -6,
                interactionCount = 0,
                hadNeglect = true,
                reunionType = ReunionType.LONG_ABSENCE,
                sessionDurationMs = 2 * 60_000L
            ),
            recentEpisodes = recentEpisodes
        )

        assertTrue(updated.affection < current.affection)
        assertTrue(updated.trust < current.trust)
        assertTrue(updated.dependency < current.dependency)
    }

    private fun episode(
        id: String,
        careScoreDelta: Int,
        neglectSignal: Boolean
    ): MemoryEpisode {
        return MemoryEpisode(
            id = id,
            startTimeMs = 1000L,
            endTimeMs = 2000L,
            durationMs = 1000L,
            eventCount = 4,
            interactionCount = 2,
            interactionTypesJson = "[\"TOUCH\"]",
            dominantPetEmotion = "HAPPY",
            dominantPetMood = "HAPPY",
            careScoreDelta = careScoreDelta,
            bondDelta = 1,
            neglectSignal = neglectSignal,
            reunionType = ReunionType.ROUTINE_RETURN.name,
            userBehaviorTag = "CARE",
            importanceScore = 0.5f,
            summaryText = "test",
            createdAtMs = 3000L
        )
    }
}
