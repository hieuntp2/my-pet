package com.aipet.brain.ui.avatar.pixel.playback

import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import kotlin.math.max

class PixelAnimationController {
    private var playbackState: PixelAnimationPlaybackState? = null
    private var lastUpdateTimeMillis: Long? = null
    private var clipActivationCount: Int = 0
    private var clipRestartCount: Int = 0
    private val recentClipHistory = mutableListOf<String>()

    fun setClip(clip: PixelAnimationClip) {
        val currentClipId = playbackState?.clip?.id
        if (currentClipId == clip.id) {
            clipRestartCount += 1
        }
        playbackState = PixelAnimationPlaybackState(clip = clip)
        lastUpdateTimeMillis = null
        clipActivationCount += 1
        recordClipHistory(clip.id)
    }

    fun setClipIfDifferent(clip: PixelAnimationClip): Boolean {
        if (playbackState?.clip?.id == clip.id) {
            return false
        }
        setClip(clip)
        return true
    }

    fun clearClip() {
        playbackState = null
        lastUpdateTimeMillis = null
    }

    fun update(deltaMillis: Long) {
        require(deltaMillis >= 0L) { "deltaMillis must be greater than or equal to 0." }

        val currentState = playbackState ?: return
        playbackState = advanceState(
            state = currentState,
            deltaMillis = deltaMillis
        )
    }

    fun update(clock: PixelAnimationClock) {
        val nowMillis = clock.nowMillis()
        val deltaMillis = lastUpdateTimeMillis
            ?.let { previousTimeMillis -> max(0L, nowMillis - previousTimeMillis) }
            ?: 0L

        lastUpdateTimeMillis = nowMillis
        update(deltaMillis = deltaMillis)
    }

    fun getCurrentFrame(): PixelFrame64? = playbackState?.currentFrame

    fun getPlaybackState(): PixelAnimationPlaybackState? = playbackState

    fun getDiagnostics(): PixelAnimationControllerDiagnostics {
        return PixelAnimationControllerDiagnostics(
            currentClipId = playbackState?.clip?.id,
            clipActivationCount = clipActivationCount,
            clipRestartCount = clipRestartCount,
            recentClipHistory = recentClipHistory.toList()
        )
    }

    fun isFinished(): Boolean = playbackState?.completed ?: false

    private fun advanceState(
        state: PixelAnimationPlaybackState,
        deltaMillis: Long
    ): PixelAnimationPlaybackState {
        if (deltaMillis == 0L || state.completed) {
            return state
        }

        val clip = state.clip
        val lastFrameIndex = clip.frames.lastIndex
        var frameIndex = state.frameIndex
        var elapsedFrameMillis = state.elapsedFrameMillis + deltaMillis
        var completed = state.completed

        while (!completed) {
            val frameDurationMillis = clip.frames[frameIndex].durationMillis.toLong()
            if (elapsedFrameMillis < frameDurationMillis) {
                break
            }

            elapsedFrameMillis -= frameDurationMillis

            if (frameIndex == lastFrameIndex) {
                when (clip.playbackMode) {
                    PixelAnimationPlaybackMode.LOOP -> frameIndex = FIRST_FRAME_INDEX
                    PixelAnimationPlaybackMode.ONE_SHOT -> {
                        completed = true
                        elapsedFrameMillis = frameDurationMillis
                    }
                }
            } else {
                frameIndex += 1
            }
        }

        return PixelAnimationPlaybackState(
            clip = clip,
            frameIndex = frameIndex,
            elapsedFrameMillis = elapsedFrameMillis,
            completed = completed
        )
    }

    private fun recordClipHistory(clipId: String) {
        recentClipHistory += clipId
        if (recentClipHistory.size > MAX_HISTORY_SIZE) {
            recentClipHistory.removeAt(0)
        }
    }

    private companion object {
        private const val FIRST_FRAME_INDEX = 0
        private const val MAX_HISTORY_SIZE = 8
    }
}

data class PixelAnimationControllerDiagnostics(
    val currentClipId: String?,
    val clipActivationCount: Int,
    val clipRestartCount: Int,
    val recentClipHistory: List<String>
)
