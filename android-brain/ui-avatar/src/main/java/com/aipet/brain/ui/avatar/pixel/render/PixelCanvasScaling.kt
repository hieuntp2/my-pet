package com.aipet.brain.ui.avatar.pixel.render

import kotlin.math.floor
import kotlin.math.min

data class PixelCanvasLayout(
    val pixelSizePx: Int,
    val contentWidthPx: Int,
    val contentHeightPx: Int,
    val offsetXPx: Float,
    val offsetYPx: Float
)

object PixelCanvasScaling {
    fun calculateLayout(
        canvasWidthPx: Float,
        canvasHeightPx: Float,
        frameWidth: Int,
        frameHeight: Int
    ): PixelCanvasLayout {
        require(canvasWidthPx > 0f) { "canvasWidthPx must be greater than 0." }
        require(canvasHeightPx > 0f) { "canvasHeightPx must be greater than 0." }
        require(frameWidth > 0) { "frameWidth must be greater than 0." }
        require(frameHeight > 0) { "frameHeight must be greater than 0." }

        val pixelSizePx = floor(
            min(canvasWidthPx / frameWidth, canvasHeightPx / frameHeight)
        ).toInt().coerceAtLeast(1)
        val contentWidthPx = pixelSizePx * frameWidth
        val contentHeightPx = pixelSizePx * frameHeight

        return PixelCanvasLayout(
            pixelSizePx = pixelSizePx,
            contentWidthPx = contentWidthPx,
            contentHeightPx = contentHeightPx,
            offsetXPx = (canvasWidthPx - contentWidthPx) / 2f,
            offsetYPx = (canvasHeightPx - contentHeightPx) / 2f
        )
    }
}
