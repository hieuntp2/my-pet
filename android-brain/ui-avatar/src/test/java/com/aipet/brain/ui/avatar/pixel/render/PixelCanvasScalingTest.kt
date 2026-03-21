package com.aipet.brain.ui.avatar.pixel.render

import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import org.junit.Assert.assertEquals
import org.junit.Test

class PixelCanvasScalingTest {

    @Test
    fun `calculateLayout rounds down to integer pixel size and centers remaining space`() {
        val layout = PixelCanvasScaling.calculateLayout(
            canvasWidthPx = 258f,
            canvasHeightPx = 256f,
            frameWidth = PixelFrame64.WIDTH,
            frameHeight = PixelFrame64.HEIGHT
        )

        assertEquals(4, layout.pixelSizePx)
        assertEquals(256, layout.contentWidthPx)
        assertEquals(256, layout.contentHeightPx)
        assertEquals(1f, layout.offsetXPx)
        assertEquals(0f, layout.offsetYPx)
    }
}
