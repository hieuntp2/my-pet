package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationClip
import com.aipet.brain.pixel.avatar.model.AnimationFrameEntry
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Thinking animation state pack — 3 variants.
 *
 * Visual theme: focused, restrained, internal. Asymmetric gaze holds.
 */
object ThinkingStateClips {

    /** Thinking_A_SideHold — slow glance left with a long deliberate hold */
    private val sideHoldClip = AnimationClip(
        name = "Thinking_A_SideHold",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 1800),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 1000),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400)
        )
    )

    /** Thinking_B_AlternatingSquint — asymmetric squint, alternates sides */
    private val alternatingSquintClip = AnimationClip(
        name = "Thinking_B_AlternatingSquint",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_THINKING_SQUINT, holdMs = 900),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_RIGHT, holdMs = 700),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300)
        )
    )

    /** Thinking_C_FocusPulse — minimal blink with long centered stare */
    private val focusPulseClip = AnimationClip(
        name = "Thinking_C_FocusPulse",
        loop = true,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 2500),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 100),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 2000)
        )
    )

    val STATE_SET = AnimationStateSet(
        state = PetVisualState.THINKING,
        variants = listOf(
            AnimationVariant(clip = sideHoldClip, weight = 4.0f, name = "SideHold"),
            AnimationVariant(clip = alternatingSquintClip, weight = 3.0f, name = "AlternatingSquint"),
            AnimationVariant(clip = focusPulseClip, weight = 3.0f, name = "FocusPulse")
        )
    )
}
