package com.aipet.brain.ui.avatar.pixel.render

import com.aipet.brain.ui.avatar.pixel.model.PixelColor
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import org.junit.Assert.assertEquals
import org.junit.Test

class PixelRenderBuffer64Test {

    @Test(expected = IllegalArgumentException::class)
    fun `render buffer rejects invalid row counts`() {
        PixelRenderBuffer64(
            rows = List(PixelFrame64.HEIGHT - 1) {
                List(PixelFrame64.WIDTH) { PixelColor.Transparent }
            }
        )
    }

    @Test
    fun `fromFrame expands palette indices into a 64x64 color grid`() {
        val palette = PixelPetDefaultPalette.palette
        val eyeBase = palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
        val frame = PixelFrame64.filled(
            palette = palette,
            colorIndex = eyeBase
        )

        val renderBuffer = PixelRenderBuffer64.fromFrame(frame)

        assertEquals(PixelFrame64.HEIGHT, renderBuffer.rows.size)
        assertEquals(PixelFrame64.WIDTH, renderBuffer.rows.first().size)
        assertEquals(palette[eyeBase].color, renderBuffer[0, 0])
        assertEquals(palette[eyeBase].color, renderBuffer[63, 63])
    }
}
