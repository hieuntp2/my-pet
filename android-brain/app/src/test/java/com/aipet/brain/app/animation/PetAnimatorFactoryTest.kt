package com.aipet.brain.app.animation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetAnimatorFactoryTest {
    @Test
    fun `default factory returns fake animator runtime`() = runTest {
        val animator = DefaultPetAnimatorFactory().create(backgroundScope)

        assertTrue(animator is FakePetAnimator)
        assertEquals(PetAnimationRuntimeMode.FAKE, animator.state.value.runtimeMode)
    }

    @Test
    fun `factory can create rive placeholder runtime without assets`() = runTest {
        val animator = DefaultPetAnimatorFactory(
            runtimeMode = PetAnimationRuntimeMode.RIVE_PLACEHOLDER
        ).create(backgroundScope)

        assertTrue(animator is RivePetAnimator)
        assertEquals(PetAnimationRuntimeMode.RIVE_PLACEHOLDER, animator.state.value.runtimeMode)
    }
}
