package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationClip
import com.aipet.brain.pixel.avatar.model.AnimationFrameEntry
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Neutral / Idle animation state pack — 4 variants.
 *
 * Weights: A=50%, B=20%, C=20%, D=10%
 */
object NeutralStateClips {

    /** Neutral_A_SlowBlink — calm main idle loop with gentle blink */
    private val slowBlinkClip = AnimationClip(
        name = "Neutral_A_SlowBlink",
        loop = true,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 1800),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 100),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 2200)
        )
    )

    /** Neutral_B_GlanceLeft — subtle side glance toward left */
    private val glanceLeftClip = AnimationClip(
        name = "Neutral_B_GlanceLeft",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 800),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_LEFT, holdMs = 600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300)
        )
    )

    /** Neutral_C_GlanceRight — subtle side glance toward right */
    private val glanceRightClip = AnimationClip(
        name = "Neutral_C_GlanceRight",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 400),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_RIGHT, holdMs = 900),
            AnimationFrameEntry(BaseEyeTemplate.EYES_GLANCE_RIGHT, holdMs = 500),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300)
        )
    )

    /** Neutral_D_DoubleBlink — rare idle flavor: two quick blinks */
    private val doubleBlinkClip = AnimationClip(
        name = "Neutral_D_DoubleBlink",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 80),
            AnimationFrameEntry(BaseEyeTemplate.EYES_OPEN, holdMs = 600)
        )
    )

    val STATE_SET = AnimationStateSet(
        state = PetVisualState.NEUTRAL,
        variants = listOf(
            AnimationVariant(clip = slowBlinkClip, weight = 5.0f, name = "SlowBlink"),
            AnimationVariant(clip = glanceLeftClip, weight = 2.0f, name = "GlanceLeft"),
            AnimationVariant(clip = glanceRightClip, weight = 2.0f, name = "GlanceRight"),
            AnimationVariant(clip = doubleBlinkClip, weight = 1.0f, name = "DoubleBlink")
        )
    )
}
