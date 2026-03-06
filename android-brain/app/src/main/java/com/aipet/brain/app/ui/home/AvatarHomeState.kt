package com.aipet.brain.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import com.aipet.brain.ui.avatar.model.AvatarEyeState
import com.aipet.brain.ui.avatar.model.AvatarMouthState
import com.aipet.brain.ui.avatar.model.AvatarState
import kotlinx.coroutines.delay
import kotlin.random.Random

class AvatarHomeState(
    initialAvatarState: AvatarState = AvatarState()
) {
    var currentAvatarState by mutableStateOf(initialAvatarState)
        private set

    private var selectedEmotion: AvatarEmotion = initialAvatarState.emotion
    private var eyeOverride: AvatarEyeState? = null
    private var mouthOverride: AvatarMouthState? = null

    fun updateAvatarState(newAvatarState: AvatarState) {
        selectedEmotion = newAvatarState.emotion
        eyeOverride = null
        mouthOverride = null
        currentAvatarState = newAvatarState
    }

    fun setEmotion(emotion: AvatarEmotion) {
        selectedEmotion = emotion
        eyeOverride = null
        mouthOverride = null
        recomputeCurrentState()
    }

    suspend fun runBlinkLoop() {
        while (true) {
            delay(Random.nextLong(from = 3_000L, until = 6_001L))
            eyeOverride = AvatarEyeState.CLOSED
            recomputeCurrentState()
            delay(150L)
            eyeOverride = null
            recomputeCurrentState()
        }
    }

    suspend fun runIdleLoop() {
        while (true) {
            delay(Random.nextLong(from = 5_000L, until = 10_001L))

            if (Random.nextBoolean()) {
                eyeOverride = AvatarEyeState.HALF_OPEN
                recomputeCurrentState()
                delay(350L)
                eyeOverride = null
                recomputeCurrentState()
            } else {
                mouthOverride = AvatarMouthState.SMALL_O
                recomputeCurrentState()
                delay(450L)
                mouthOverride = null
                recomputeCurrentState()
            }
        }
    }

    private fun recomputeCurrentState() {
        val baseState = stateForEmotion(selectedEmotion)
        currentAvatarState = baseState.copy(
            eyeState = eyeOverride ?: baseState.eyeState,
            mouthState = mouthOverride ?: baseState.mouthState
        )
    }

    private fun stateForEmotion(emotion: AvatarEmotion): AvatarState {
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
}
