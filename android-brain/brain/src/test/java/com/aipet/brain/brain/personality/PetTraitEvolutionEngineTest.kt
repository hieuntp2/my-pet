package com.aipet.brain.brain.personality

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetTraitEvolutionEngineTest {
    private val engine = PetTraitEvolutionEngine()

    @Test
    fun `single gameplay action keeps trait drift bounded`() {
        val initial = traits()

        val updated = engine.applyActivity(
            current = initial,
            activityType = PetActivityType.PLAY,
            appliedAtMs = 2_000L
        )

        assertTrue(updated.playful - initial.playful < 0.02f)
        assertTrue(initial.lazy - updated.lazy < 0.01f)
        assertEquals(2_000L, updated.updatedAt)
    }

    @Test
    fun `repeated play gradually increases playful trait without spiking`() {
        var current = traits(playful = 0.55f, lazy = 0.35f, curious = 0.60f, social = 0.50f)

        repeat(12) { index ->
            current = engine.applyActivity(
                current = current,
                activityType = PetActivityType.PLAY,
                appliedAtMs = 3_000L + index
            )
        }

        assertTrue(current.playful > 0.60f)
        assertTrue(current.playful < 0.75f)
        assertTrue(current.lazy < 0.35f)
    }

    @Test
    fun `rest and long press nudge calmer social personality gradually`() {
        val afterLongPress = engine.applyInteraction(
            current = traits(lazy = 0.35f, social = 0.50f),
            interactionType = PetInteractionType.LONG_PRESS,
            appliedAtMs = 2_500L
        )
        val afterRest = engine.applyActivity(
            current = afterLongPress,
            activityType = PetActivityType.REST,
            appliedAtMs = 3_000L
        )

        assertTrue(afterLongPress.social > 0.50f)
        assertTrue(afterRest.lazy > afterLongPress.lazy)
        assertTrue(afterRest.playful <= afterLongPress.playful)
    }

    private fun traits(
        playful: Float = 0.55f,
        lazy: Float = 0.35f,
        curious: Float = 0.60f,
        social: Float = 0.50f
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
