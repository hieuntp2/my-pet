package com.aipet.brain.app.animation

import kotlinx.coroutines.CoroutineScope

fun interface PetAnimatorFactory {
    fun create(scope: CoroutineScope): PetAnimator
}

enum class PetAnimationRuntimeMode {
    FAKE,
    RIVE_PLACEHOLDER,
    PIXEL
}

class DefaultPetAnimatorFactory(
    private val runtimeMode: PetAnimationRuntimeMode = PetAnimationRuntimeMode.PIXEL
) : PetAnimatorFactory {
    override fun create(scope: CoroutineScope): PetAnimator {
        return when (runtimeMode) {
            PetAnimationRuntimeMode.FAKE -> FakePetAnimator(scope = scope)
            PetAnimationRuntimeMode.RIVE_PLACEHOLDER -> RivePetAnimator(scope = scope)
            PetAnimationRuntimeMode.PIXEL -> PixelPetAnimator(scope = scope)
        }
    }
}
