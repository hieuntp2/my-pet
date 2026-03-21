package com.aipet.brain.ui.avatar.pixel.playback

import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationFrameEntry
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64

data class PixelAnimationPlaybackState(
    val clip: PixelAnimationClip,
    val frameIndex: Int = FIRST_FRAME_INDEX,
    val elapsedFrameMillis: Long = INITIAL_ELAPSED_MILLIS,
    val completed: Boolean = false
) {
    init {
        require(frameIndex in clip.frames.indices) {
            "frameIndex must be within the clip frame range: $frameIndex"
        }
        require(elapsedFrameMillis >= 0L) {
            "elapsedFrameMillis must be greater than or equal to 0."
        }
    }

    val currentFrameEntry: PixelAnimationFrameEntry = clip.frames[frameIndex]
    val currentFrame: PixelFrame64 = currentFrameEntry.frame

    companion object {
        private const val FIRST_FRAME_INDEX = 0
        private const val INITIAL_ELAPSED_MILLIS = 0L
    }
}
