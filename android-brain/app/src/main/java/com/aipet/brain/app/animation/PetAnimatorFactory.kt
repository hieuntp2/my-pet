package com.aipet.brain.app.animation

import kotlinx.coroutines.CoroutineScope

fun interface PetAnimatorFactory {
    fun create(scope: CoroutineScope): PetAnimator
}

enum class PetAnimationRuntimeMode {
    FAKE,
    RIVE_PLACEHOLDER
}

class DefaultPetAnimatorFactory(
    private val runtimeMode: PetAnimationRuntimeMode = PetAnimationRuntimeMode.FAKE
) : PetAnimatorFactory {
    override fun create(scope: CoroutineScope): PetAnimator {
        return when (runtimeMode) {
            PetAnimationRuntimeMode.FAKE -> FakePetAnimator(scope = scope)
            PetAnimationRuntimeMode.RIVE_PLACEHOLDER -> RivePetAnimator(scope = scope)
        }
    }
}
