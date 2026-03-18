package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import com.aipet.brain.ui.avatar.model.AvatarEyeState
import com.aipet.brain.ui.avatar.model.AvatarMouthState
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePetAnimatorTest {
    @Test
    fun `playGreeting temporarily overrides mood and then restores it`() = runTest {
        val animator = FakePetAnimator(
            scope = backgroundScope,
            clock = { testScheduler.currentTime }
        )
        animator.syncInputFrame(
            PetAnimationFrame(emotion = PetAnimationEmotion.CALM)
        )

        animator.playTrigger(
            trigger = PetAnimationTrigger(
                reactionType = PetAnimationReactionType.GREETING,
                emotion = PetAnimationEmotion.HAPPY,
                greetingType = PetAnimationGreetingType.WARM
            ),
            durationMs = 500L
        )

        assertEquals(PetEmotion.HAPPY, animator.state.value.activeEmotion)
        assertEquals(PetAnimationReactionType.GREETING, animator.state.value.inputFrame.trigger.reactionType)

        advanceTimeBy(500L)

        assertEquals(PetEmotion.IDLE, animator.state.value.activeEmotion)
        assertEquals(PetAnimationSource.MOOD, animator.state.value.source)
        assertEquals(PetAnimationReactionType.NONE, animator.state.value.inputFrame.trigger.reactionType)
    }

    @Test
    fun `long press and tap expose different visible mouth semantics`() = runTest {
        val animator = FakePetAnimator(
            scope = backgroundScope,
            clock = { testScheduler.currentTime }
        )
        animator.syncInputFrame(PetAnimationFrame(emotion = PetAnimationEmotion.HAPPY))

        animator.onTap(PetEmotion.HAPPY)
        val tapSurface = animator.state.value.surfaceState as PetAnimationSurfaceState.AvatarFaceSurface
        animator.onLongPress(PetEmotion.HAPPY)
        val longPressSurface = animator.state.value.surfaceState as PetAnimationSurfaceState.AvatarFaceSurface

        assertEquals(AvatarMouthState.SMALL_O, tapSurface.avatarState.mouthState)
        assertEquals(AvatarMouthState.SMILE, longPressSurface.avatarState.mouthState)
        assertEquals(AvatarEyeState.HALF_OPEN, longPressSurface.avatarState.eyeState)
    }

    @Test
    fun `activity and sound triggers map to clearer temporary poses`() = runTest {
        val animator = FakePetAnimator(
            scope = backgroundScope,
            clock = { testScheduler.currentTime }
        )

        animator.onActivityResult(PetActivityType.PLAY, PetEmotion.EXCITED)
        val playSurface = animator.state.value.surfaceState as PetAnimationSurfaceState.AvatarFaceSurface

        animator.onSoundReaction(AudioCategory.SURPRISED)
        val soundSurface = animator.state.value.surfaceState as PetAnimationSurfaceState.AvatarFaceSurface

        assertEquals(AvatarEmotion.SURPRISED, playSurface.avatarState.emotion)
        assertEquals(AvatarMouthState.OPEN, playSurface.avatarState.mouthState)
        assertEquals(AvatarEmotion.SURPRISED, soundSurface.avatarState.emotion)
        assertEquals(AvatarMouthState.SMALL_O, soundSurface.avatarState.mouthState)
    }

    @Test
    fun `state exposes avatar surface through abstraction and honors passive input frame`() = runTest {
        val animator = FakePetAnimator(
            scope = backgroundScope,
            clock = { testScheduler.currentTime }
        )

        animator.syncInputFrame(
            PetAnimationFrame(
                emotion = PetAnimationEmotion.CURIOUS,
                energyBand = PetAnimationBand.MID,
                hungerBand = PetAnimationBand.MID,
                socialBand = PetAnimationBand.HIGH,
                flavor = PetAnimationFlavor.AFFECTIONATE
            )
        )

        val surfaceState = animator.state.value.surfaceState

        assertTrue(surfaceState is PetAnimationSurfaceState.AvatarFaceSurface)
        val avatarSurface = surfaceState as PetAnimationSurfaceState.AvatarFaceSurface
        assertEquals(AvatarEmotion.CURIOUS, avatarSurface.avatarState.emotion)
        assertEquals(AvatarMouthState.SMILE, avatarSurface.avatarState.mouthState)
    }
}
