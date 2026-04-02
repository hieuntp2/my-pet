package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.personality.PetTrait
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalityEvolutionEngineTest {

    @Test
    fun `evolve returns unchanged trait when evidence is insufficient`() {
        val engine = PersonalityEvolutionEngine(nowProvider = { 9000L })
        val current = trait()
        val episodes = listOf(
            episode(id = "ep1", careScoreDelta = 10, tag = "PLAY"),
            episode(id = "ep2", careScoreDelta = 12, tag = "PLAY"),
            episode(id = "ep3", careScoreDelta = 11, tag = "PLAY")
        )

        val evolved = engine.evolve(current, episodes)

        assertEquals(current, evolved)
    }

    @Test
    fun `evolve drifts playful social and attachment traits from repeated positive routines`() {
        val engine = PersonalityEvolutionEngine(nowProvider = { 10_000L })
        val current = trait()
        val episodes = listOf(
            episode(id = "ep1", careScoreDelta = 12, tag = "PLAY"),
            episode(id = "ep2", careScoreDelta = 13, tag = "PLAY"),
            episode(id = "ep3", careScoreDelta = 11, tag = "PLAY"),
            episode(id = "ep4", careScoreDelta = 14, tag = "PLAY")
        )

        val evolved = engine.evolve(current, episodes)

        assertTrue(evolved.playful > current.playful)
        assertTrue(evolved.social > current.social)
        assertTrue(evolved.attachment > current.attachment)
        assertEquals(10_000L, evolved.updatedAt)
    }

    private fun trait(): PetTrait {
        return PetTrait(
            petId = "pet-1",
            playful = 0.4f,
            lazy = 0.3f,
            curious = 0.5f,
            social = 0.4f,
            patience = 0.4f,
            attachment = 0.35f,
            energyProfile = 0.45f,
            updatedAt = 1000L
        )
    }

    private fun episode(id: String, careScoreDelta: Int, tag: String): MemoryEpisode {
        return MemoryEpisode(
            id = id,
            startTimeMs = 1000L,
            endTimeMs = 1000L + 6 * 60_000L,
            durationMs = 6 * 60_000L,
            eventCount = 8,
            interactionCount = 6,
            interactionTypesJson = "[\"PLAY\"]",
            dominantPetEmotion = "EXCITED",
            dominantPetMood = "HAPPY",
            careScoreDelta = careScoreDelta,
            bondDelta = 2,
            neglectSignal = false,
            reunionType = ReunionType.ROUTINE_RETURN.name,
            userBehaviorTag = tag,
            importanceScore = 0.7f,
            summaryText = "test",
            createdAtMs = 2000L
        )
    }
}
