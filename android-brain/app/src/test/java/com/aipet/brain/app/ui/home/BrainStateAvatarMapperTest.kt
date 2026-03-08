package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import org.junit.Assert.assertEquals
import org.junit.Test

class BrainStateAvatarMapperTest {
    @Test
    fun mapsIdleToNeutral() {
        assertEquals(
            AvatarEmotion.NEUTRAL,
            BrainStateAvatarMapper.toAvatarEmotion(BrainState.IDLE)
        )
    }

    @Test
    fun mapsCuriousToCurious() {
        assertEquals(
            AvatarEmotion.CURIOUS,
            BrainStateAvatarMapper.toAvatarEmotion(BrainState.CURIOUS)
        )
    }

    @Test
    fun mapsHappyToHappy() {
        assertEquals(
            AvatarEmotion.HAPPY,
            BrainStateAvatarMapper.toAvatarEmotion(BrainState.HAPPY)
        )
    }

    @Test
    fun mapsSleepyToSleepy() {
        assertEquals(
            AvatarEmotion.SLEEPY,
            BrainStateAvatarMapper.toAvatarEmotion(BrainState.SLEEPY)
        )
    }
}
