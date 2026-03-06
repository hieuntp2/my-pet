package com.aipet.brain.ui.avatar.model

object AvatarStateRules {
    fun stateForEmotion(emotion: AvatarEmotion): AvatarState {
        return when (emotion) {
            AvatarEmotion.NEUTRAL -> AvatarState(
                emotion = AvatarEmotion.NEUTRAL,
                eyeState = AvatarEyeState.OPEN,
                mouthState = AvatarMouthState.NEUTRAL
            )

            AvatarEmotion.HAPPY -> AvatarState(
                emotion = AvatarEmotion.HAPPY,
                eyeState = AvatarEyeState.OPEN,
                mouthState = AvatarMouthState.SMILE
            )

            AvatarEmotion.CURIOUS -> AvatarState(
                emotion = AvatarEmotion.CURIOUS,
                eyeState = AvatarEyeState.HALF_OPEN,
                mouthState = AvatarMouthState.SMALL_O
            )

            AvatarEmotion.SLEEPY -> AvatarState(
                emotion = AvatarEmotion.SLEEPY,
                eyeState = AvatarEyeState.HALF_OPEN,
                mouthState = AvatarMouthState.NEUTRAL
            )

            AvatarEmotion.SURPRISED -> AvatarState(
                emotion = AvatarEmotion.SURPRISED,
                eyeState = AvatarEyeState.OPEN,
                mouthState = AvatarMouthState.OPEN
            )
        }
    }

    fun normalizeForRender(state: AvatarState): AvatarState {
        val eyeState = when (state.emotion) {
            AvatarEmotion.SLEEPY -> {
                if (state.eyeState == AvatarEyeState.OPEN) AvatarEyeState.HALF_OPEN else state.eyeState
            }

            AvatarEmotion.SURPRISED -> {
                if (state.eyeState == AvatarEyeState.CLOSED || state.eyeState == AvatarEyeState.BLINK) {
                    AvatarEyeState.OPEN
                } else {
                    state.eyeState
                }
            }

            else -> state.eyeState
        }

        val mouthState = when (state.emotion) {
            AvatarEmotion.HAPPY -> {
                if (state.mouthState == AvatarMouthState.NEUTRAL) AvatarMouthState.SMILE else state.mouthState
            }

            AvatarEmotion.SURPRISED -> {
                if (state.mouthState == AvatarMouthState.NEUTRAL) AvatarMouthState.OPEN else state.mouthState
            }

            else -> state.mouthState
        }

        return state.copy(eyeState = eyeState, mouthState = mouthState)
    }

    fun applyTransientOverrides(
        baseState: AvatarState,
        idleEyeOverride: AvatarEyeState?,
        idleMouthOverride: AvatarMouthState?,
        blinkEyeOverride: AvatarEyeState?
    ): AvatarState {
        // Priority: blink > idle > base for eyes. Idle can override mouth, blink does not.
        return baseState.copy(
            eyeState = blinkEyeOverride ?: idleEyeOverride ?: baseState.eyeState,
            mouthState = idleMouthOverride ?: baseState.mouthState
        )
    }
}
