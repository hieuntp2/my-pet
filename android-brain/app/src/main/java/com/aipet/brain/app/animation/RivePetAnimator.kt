package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Placeholder runtime seam for future Rive integration.
 *
 * This adapter intentionally does not claim Rive assets exist yet. It keeps the current app
 * runnable by delegating motion semantics to the fake animator while exposing a distinct runtime
 * mode so the future asset-backed implementation can replace only this adapter.
 */
class RivePetAnimator(
    scope: CoroutineScope,
    private val fallback: PetAnimator = FakePetAnimator(scope = scope)
) : PetAnimator {
    private val mutableState = MutableStateFlow(
        fallback.state.value.copy(runtimeMode = PetAnimationRuntimeMode.RIVE_PLACEHOLDER)
    )

    override val state: StateFlow<PetAnimationState> = mutableState.asStateFlow()

    init {
        scope.launch {
            fallback.state.collect { fallbackState ->
                mutableState.value = fallbackState.copy(runtimeMode = PetAnimationRuntimeMode.RIVE_PLACEHOLDER)
            }
        }
    }

    override fun syncInputFrame(frame: PetAnimationFrame) {
        fallback.syncInputFrame(frame)
    }

    override fun playTrigger(trigger: PetAnimationTrigger, durationMs: Long) {
        fallback.playTrigger(trigger, durationMs)
    }

    override fun syncMood(emotion: PetEmotion) {
        fallback.syncMood(emotion)
    }

    override fun playGreeting(greeting: PetGreetingReaction, durationMs: Long) {
        fallback.playGreeting(greeting, durationMs)
    }

    override fun playReaction(emotion: PetEmotion, source: PetAnimationSource, durationMs: Long) {
        fallback.playReaction(emotion, source, durationMs)
    }

    override fun onTap(resultingEmotion: PetEmotion) {
        fallback.onTap(resultingEmotion)
    }

    override fun onLongPress(resultingEmotion: PetEmotion) {
        fallback.onLongPress(resultingEmotion)
    }

    override fun onActivityResult(activityType: PetActivityType, resultingEmotion: PetEmotion) {
        fallback.onActivityResult(activityType, resultingEmotion)
    }

    override fun onSoundReaction(category: AudioCategory) {
        fallback.onSoundReaction(category)
    }
}
