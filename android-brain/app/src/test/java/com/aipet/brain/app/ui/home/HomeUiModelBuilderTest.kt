package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiModelBuilderTest {
    @Test
    fun `build uses real pet name and calm status for balanced state`() {
        val model = HomeUiModelBuilder.build(
            petName = "Cun",
            petState = state(),
            emotion = PetEmotion.IDLE,
            conditions = setOf(PetCondition.CALM)
        )

        assertEquals("Cun", model.petName)
        assertEquals("Your digital pet companion", model.identityLine)
        assertEquals("Cun feels calm right now.", model.statusLine)
        assertEquals("Calm", model.moodLabel)
        assertEquals(null, model.personalityLabel)
    }

    @Test
    fun `build surfaces lightweight personality label when traits are available`() {
        val model = HomeUiModelBuilder.build(
            petName = "Cun",
            petState = state(),
            emotion = PetEmotion.HAPPY,
            conditions = emptySet(),
            traits = PetTrait(
                petId = "pet-1",
                playful = 0.90f,
                lazy = 0.20f,
                curious = 0.55f,
                social = 0.48f,
                updatedAt = 1_500L
            )
        )

        assertEquals("Playful", model.personalityLabel)
    }

    @Test
    fun `build carries today summary into home ui model`() {
        val model = HomeUiModelBuilder.build(
            petName = "Cun",
            petState = state(),
            emotion = PetEmotion.HAPPY,
            conditions = emptySet(),
            todaySummary = HomeTodaySummary(
                title = "Today with your pet",
                body = "2 saved moments today: 1 meal, 1 greeting."
            )
        )

        assertEquals("Today with your pet", model.todaySummary?.title)
    }

    @Test
    fun `build carries known people and known object counts`() {
        val model = HomeUiModelBuilder.build(
            petName = "Cun",
            petState = state(),
            emotion = PetEmotion.HAPPY,
            conditions = emptySet(),
            knownPersons = listOf(
                HomeKnownEntityCount(
                    name = "Hieu",
                    seenCount = 7
                )
            ),
            knownObjects = listOf(
                HomeKnownEntityCount(
                    name = "Red ball",
                    seenCount = 4
                )
            )
        )

        assertEquals("Hieu", model.knownPersons.first().name)
        assertEquals(7, model.knownPersons.first().seenCount)
        assertEquals("Red ball", model.knownObjects.first().name)
        assertEquals(4, model.knownObjects.first().seenCount)
    }

    @Test
    fun `build prioritizes hungry status and user friendly indicator labels`() {
        val model = HomeUiModelBuilder.build(
            petName = "Cun",
            petState = state(
                energy = 75,
                hunger = 82,
                social = 25
            ),
            emotion = PetEmotion.HUNGRY,
            conditions = setOf(PetCondition.HUNGRY, PetCondition.LONELY)
        )

        assertEquals("Cun is getting hungry.", model.statusLine)
        assertEquals("Hungry", model.moodLabel)
        assertEquals("High", model.indicators.first { it.label == "Energy" }.value)
        assertEquals("Hungry", model.indicators.first { it.label == "Hunger" }.value)
        assertEquals("Needs attention", model.indicators.first { it.label == "Company" }.value)
    }

    private fun state(
        energy: Int = 55,
        hunger: Int = 30,
        social: Int = 60
    ): PetState {
        return PetState(
            mood = PetMood.NEUTRAL,
            energy = energy,
            hunger = hunger,
            sleepiness = 25,
            social = social,
            bond = 10,
            lastUpdatedAt = 1_000L
        )
    }
}
