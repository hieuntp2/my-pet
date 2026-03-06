package com.aipet.brain.ui.avatar.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarStateRulesTest {
    @Test
    fun stateForEmotion_producesDistinctVisualsForPrimaryEmotions() {
        val neutral = AvatarStateRules.normalizeForRender(AvatarStateRules.stateForEmotion(AvatarEmotion.NEUTRAL))
        val happy = AvatarStateRules.normalizeForRender(AvatarStateRules.stateForEmotion(AvatarEmotion.HAPPY))
        val curious = AvatarStateRules.normalizeForRender(AvatarStateRules.stateForEmotion(AvatarEmotion.CURIOUS))
        val sleepy = AvatarStateRules.normalizeForRender(AvatarStateRules.stateForEmotion(AvatarEmotion.SLEEPY))

        assertNotEquals(neutral.eyeState to neutral.mouthState, happy.eyeState to happy.mouthState)
        assertNotEquals(neutral.eyeState to neutral.mouthState, curious.eyeState to curious.mouthState)
        assertNotEquals(neutral.eyeState to neutral.mouthState, sleepy.eyeState to sleepy.mouthState)
        assertNotEquals(happy.eyeState to happy.mouthState, curious.eyeState to curious.mouthState)
    }

    @Test
    fun normalizeForRender_enforcesSleepyEyeConstraint() {
        val invalidSleepy = AvatarState(
            emotion = AvatarEmotion.SLEEPY,
            eyeState = AvatarEyeState.OPEN,
            mouthState = AvatarMouthState.NEUTRAL
        )

        val normalized = AvatarStateRules.normalizeForRender(invalidSleepy)

        assertEquals(AvatarEyeState.HALF_OPEN, normalized.eyeState)
    }

    @Test
    fun applyTransientOverrides_prioritizesBlinkOverIdleForEyes() {
        val base = AvatarStateRules.stateForEmotion(AvatarEmotion.NEUTRAL)

        val resolved = AvatarStateRules.applyTransientOverrides(
            baseState = base,
            idleEyeOverride = AvatarEyeState.HALF_OPEN,
            idleMouthOverride = AvatarMouthState.SMALL_O,
            blinkEyeOverride = AvatarEyeState.CLOSED
        )

        assertEquals(AvatarEyeState.CLOSED, resolved.eyeState)
        assertEquals(AvatarMouthState.SMALL_O, resolved.mouthState)
    }
}
