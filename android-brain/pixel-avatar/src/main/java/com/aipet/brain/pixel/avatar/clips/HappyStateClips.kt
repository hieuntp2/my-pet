package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationClip
import com.aipet.brain.pixel.avatar.model.AnimationFrameEntry
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Happy animation state pack — 3 variants.
 *
 * Visual theme: warm squints, slight bounce energy, brief widen.
 */
object HappyStateClips {

    /** Happy_A_SoftSquint — happy blink loop with squinted eyes (primary) */
    private val softSquintClip = AnimationClip(
        name = "Happy_A_SoftSquint",
        loop = true,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 1600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 1200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 900)
        )
    )

    /** Happy_B_OpenBounce — bright open eyes doing a quick double-blink bounce */
    private val openBounceClip = AnimationClip(
        name = "Happy_B_OpenBounce",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 120),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 150),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 120),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300)
        )
    )

    /** Happy_C_WinkAsymmetry — blink one side slightly ahead of the other */
    private val winkClip = AnimationClip(
        name = "Happy_C_WinkAsymmetry",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HAPPY_SQUINT, holdMs = 100),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 120),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400)
        )
    )

    val STATE_SET = AnimationStateSet(
        state = PetVisualState.HAPPY,
        variants = listOf(
            AnimationVariant(clip = softSquintClip, weight = 5.0f, name = "SoftSquint"),
            AnimationVariant(clip = openBounceClip, weight = 3.0f, name = "OpenBounce"),
            AnimationVariant(clip = winkClip, weight = 2.0f, name = "WinkAsymmetry")
        )
    )
}
