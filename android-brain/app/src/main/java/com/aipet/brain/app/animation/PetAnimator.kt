package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.ui.avatar.model.AvatarState
import kotlinx.coroutines.flow.StateFlow

interface PetAnimator {
    val state: StateFlow<PetAnimationState>

    fun syncInputFrame(frame: PetAnimationFrame)

    fun playTrigger(
        trigger: PetAnimationTrigger,
        durationMs: Long = DEFAULT_REACTION_DURATION_MS
    )

    fun syncMood(emotion: PetEmotion)

    fun playGreeting(greeting: PetGreetingReaction, durationMs: Long = DEFAULT_GREETING_DURATION_MS)

    fun playReaction(
        emotion: PetEmotion,
        source: PetAnimationSource,
        durationMs: Long
    )

    fun onTap(resultingEmotion: PetEmotion)

    fun onLongPress(resultingEmotion: PetEmotion)

    fun onActivityResult(
        activityType: PetActivityType,
        resultingEmotion: PetEmotion
    )

    fun onSoundReaction(category: AudioCategory)

    companion object {
        const val DEFAULT_GREETING_DURATION_MS: Long = 2_000L
        const val DEFAULT_REACTION_DURATION_MS: Long = 1_000L
    }
}

data class PetAnimationState(
    val baseEmotion: PetEmotion = PetEmotion.IDLE,
    val activeEmotion: PetEmotion = PetEmotion.IDLE,
    val source: PetAnimationSource = PetAnimationSource.MOOD,
    val updatedAtMs: Long = 0L,
    val inputFrame: PetAnimationFrame = PetAnimationFrame(),
    val runtimeMode: PetAnimationRuntimeMode = PetAnimationRuntimeMode.FAKE,
    val surfaceState: PetAnimationSurfaceState = PetAnimationSurfaceState.AvatarFaceSurface(
        avatarState = AvatarState()
    )
)

sealed interface PetAnimationSurfaceState {
    data class AvatarFaceSurface(
        val avatarState: AvatarState
    ) : PetAnimationSurfaceState
}

enum class PetAnimationSource {
    MOOD,
    GREETING,
    REACTION,
    TAP,
    LONG_PRESS,
    FEED,
    PLAY,
    REST,
    SOUND
}
