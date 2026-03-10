package com.aipet.brain.app.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import com.aipet.brain.ui.avatar.model.AvatarEyeState
import com.aipet.brain.ui.avatar.model.AvatarMouthState
import com.aipet.brain.ui.avatar.model.AvatarState
import com.aipet.brain.ui.avatar.model.AvatarStateRules
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

class AvatarHomeState(
    initialAvatarState: AvatarState = AvatarState()
) {
    var currentAvatarState by mutableStateOf(initialAvatarState)
        private set

    var selectedEmotion: AvatarEmotion by mutableStateOf(initialAvatarState.emotion)
        private set

    private var temporaryEmotionOverride: AvatarEmotion? = null
    private var idleEyeOverride: AvatarEyeState? = null
    private var idleMouthOverride: AvatarMouthState? = null
    private var blinkEyeOverride: AvatarEyeState? = null
    private var isBlinkLoopRunning = false
    private var isIdleLoopRunning = false

    init {
        recomputeCurrentState()
    }

    fun updateAvatarState(newAvatarState: AvatarState) {
        selectedEmotion = newAvatarState.emotion
        clearMotionOverrides()
        if (temporaryEmotionOverride == null) {
            currentAvatarState = AvatarStateRules.normalizeForRender(newAvatarState)
        } else {
            recomputeCurrentState()
        }
    }

    fun setEmotion(emotion: AvatarEmotion) {
        selectedEmotion = emotion
        clearMotionOverrides()
        recomputeCurrentState()
    }

    fun applyTemporaryEmotionOverride(emotion: AvatarEmotion): Boolean {
        if (temporaryEmotionOverride == emotion) {
            return false
        }
        temporaryEmotionOverride = emotion
        recomputeCurrentState()
        return true
    }

    fun clearTemporaryEmotionOverride(): Boolean {
        if (temporaryEmotionOverride == null) {
            return false
        }
        temporaryEmotionOverride = null
        recomputeCurrentState()
        return true
    }

    suspend fun runBlinkLoop() {
        if (isBlinkLoopRunning) {
            return
        }

        isBlinkLoopRunning = true
        try {
            while (currentCoroutineContext().isActive) {
                delay(Random.nextLong(from = 3_000L, until = 6_001L))
                blinkEyeOverride = AvatarEyeState.CLOSED
                recomputeCurrentState()
                delay(150L)
                blinkEyeOverride = null
                recomputeCurrentState()
            }
        } finally {
            isBlinkLoopRunning = false
            blinkEyeOverride = null
            recomputeCurrentState()
        }
    }

    suspend fun runIdleLoop() {
        if (isIdleLoopRunning) {
            return
        }

        isIdleLoopRunning = true
        try {
            while (currentCoroutineContext().isActive) {
                delay(Random.nextLong(from = 5_000L, until = 10_001L))

                if (Random.nextBoolean()) {
                    idleEyeOverride = AvatarEyeState.HALF_OPEN
                    recomputeCurrentState()
                    delay(350L)
                    idleEyeOverride = null
                    recomputeCurrentState()
                } else {
                    idleMouthOverride = AvatarMouthState.SMALL_O
                    recomputeCurrentState()
                    delay(450L)
                    idleMouthOverride = null
                    recomputeCurrentState()
                }
            }
        } finally {
            isIdleLoopRunning = false
            idleEyeOverride = null
            idleMouthOverride = null
            recomputeCurrentState()
        }
    }

    private fun recomputeCurrentState() {
        val resolvedEmotion = temporaryEmotionOverride ?: selectedEmotion
        val baseState = AvatarStateRules.stateForEmotion(resolvedEmotion)
        val resolvedState = AvatarStateRules.applyTransientOverrides(
            baseState = baseState,
            idleEyeOverride = idleEyeOverride,
            idleMouthOverride = idleMouthOverride,
            blinkEyeOverride = blinkEyeOverride
        )
        currentAvatarState = AvatarStateRules.normalizeForRender(resolvedState)
    }

    private fun clearMotionOverrides() {
        idleEyeOverride = null
        idleMouthOverride = null
        blinkEyeOverride = null
    }
}
