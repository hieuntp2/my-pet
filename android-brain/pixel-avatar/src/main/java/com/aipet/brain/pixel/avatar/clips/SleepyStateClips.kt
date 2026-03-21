package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationClip
import com.aipet.brain.pixel.avatar.model.AnimationFrameEntry
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Sleepy animation state pack — 3 variants.
 *
 * Visual theme: drooped lids, slow long blinks, low energy. Never sad.
 */
object SleepyStateClips {

    /** Sleepy_A_HalfLidLoop — drooped idle loop (primary) */
    private val halfLidClip = AnimationClip(
        name = "Sleepy_A_HalfLidLoop",
        loop = true,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 2000),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 120),
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 1800),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 120),
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 2200)
        )
    )

    /** Sleepy_B_LongBlink — very slow blink with delayed reopen */
    private val longBlinkClip = AnimationClip(
        name = "Sleepy_B_LongBlink",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 150),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 600),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 200),
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 400)
        )
    )

    /** Sleepy_C_DroopDrift — lids droop further then slowly recover */
    private val droopDriftClip = AnimationClip(
        name = "Sleepy_C_DroopDrift",
        loop = false,
        frames = listOf(
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 400),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_CLOSED, holdMs = 400),
            AnimationFrameEntry(BaseEyeTemplate.EYES_HALF_OPEN, holdMs = 300),
            AnimationFrameEntry(BaseEyeTemplate.EYES_SLEEPY_DROOP, holdMs = 500)
        )
    )

    val STATE_SET = AnimationStateSet(
        state = PetVisualState.SLEEPY,
        variants = listOf(
            AnimationVariant(clip = halfLidClip, weight = 5.0f, name = "HalfLidLoop"),
            AnimationVariant(clip = longBlinkClip, weight = 3.0f, name = "LongBlink"),
            AnimationVariant(clip = droopDriftClip, weight = 2.0f, name = "DroopDrift")
        )
    )
}
