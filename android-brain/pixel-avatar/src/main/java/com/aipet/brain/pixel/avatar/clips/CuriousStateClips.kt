package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationClip
import com.aipet.brain.pixel.avatar.model.AnimationFrameEntry
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Curious animation state pack — 3 variants.
 *
 * Visual theme: attentive gaze, brief inspect motions, slight focus squint.
 */
object CuriousStateClips {

    /** Curious_A_LeftInspect — look left to inspect, return to center */
    private val leftInspectClip = AnimationClip(
        name = "Curious_A_LeftInspect",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 1200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 500)
        )
    )

    /** Curious_B_RightInspect — look right to inspect, return to center */
    private val rightInspectClip = AnimationClip(
        name = "Curious_B_RightInspect",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_RIGHT, holdMs = 1000),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_RIGHT, holdMs = 700),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400)
        )
    )

    /** Curious_C_FocusSquint — narrow eyes on a point and hold */
    private val focusSquintClip = AnimationClip(
        name = "Curious_C_FocusSquint",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 800),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400)
        )
    )

    val STATE_SET = AnimationStateSet(
        state = PetVisualState.CURIOUS,
        variants = listOf(
            AnimationVariant(clip = leftInspectClip, weight = 4.0f, name = "LeftInspect"),
            AnimationVariant(clip = rightInspectClip, weight = 4.0f, name = "RightInspect"),
            AnimationVariant(clip = focusSquintClip, weight = 2.0f, name = "FocusSquint")
        )
    )
}
