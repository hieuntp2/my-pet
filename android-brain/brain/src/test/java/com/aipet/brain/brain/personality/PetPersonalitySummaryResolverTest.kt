package com.aipet.brain.brain.personality

import org.junit.Assert.assertEquals
import org.junit.Test

class PetPersonalitySummaryResolverTest {
    private val resolver = PetPersonalitySummaryResolver()

    @Test
    fun `resolve returns playful label when playful is clearly dominant`() {
        val summary = resolver.resolve(
            traits(playful = 0.88f, lazy = 0.20f, curious = 0.55f, social = 0.45f)
        )

        assertEquals("Playful", summary?.label)
    }

    @Test
    fun `resolve returns calm label when lazy is clearly dominant`() {
        val summary = resolver.resolve(
            traits(playful = 0.35f, lazy = 0.82f, curious = 0.45f, social = 0.50f)
        )

        assertEquals("Calm", summary?.label)
    }

    @Test
    fun `resolve returns balanced label when no trait strongly dominates`() {
        val summary = resolver.resolve(
            traits(playful = 0.54f, lazy = 0.49f, curious = 0.57f, social = 0.53f)
        )

        assertEquals("Balanced", summary?.label)
    }

    private fun traits(
        playful: Float,
        lazy: Float,
        curious: Float,
        social: Float
    ): PetTrait {
        return PetTrait(
            petId = "pet-1",
            playful = playful,
            lazy = lazy,
            curious = curious,
            social = social,
            updatedAt = 1_000L
        )
    }
}
