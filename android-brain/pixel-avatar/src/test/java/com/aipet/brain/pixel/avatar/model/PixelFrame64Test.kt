package com.aipet.brain.pixel.avatar.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PixelFrame64Test {

    @Test
    fun `valid 64x64 frame constructs without error`() {
        val row = ".".repeat(64)
        val frame = PixelFrame64(List(64) { row })
        assertNotNull(frame)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `too few rows throws`() {
        val row = ".".repeat(64)
        PixelFrame64(List(63) { row }) // 63 rows — should throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `too many rows throws`() {
        val row = ".".repeat(64)
        PixelFrame64(List(65) { row }) // 65 rows — should throw
    }

    @Test(expected = IllegalArgumentException::class)
    fun `row too short throws`() {
        val goodRow = ".".repeat(64)
        val badRow = ".".repeat(63)
        PixelFrame64(List(64) { i -> if (i == 0) badRow else goodRow })
    }

    @Test
    fun `pixels array has correct size`() {
        val row = ".".repeat(64)
        val frame = PixelFrame64(List(64) { row })
        assertEquals(64 * 64, frame.pixels.size)
    }

    @Test
    fun `pixel value resolved from palette`() {
        val palette = PixelPalette(mapOf('W' to 0xFFFFFFFF.toInt(), '.' to 0x00000000))
        val row = "W".repeat(64)
        val frame = PixelFrame64(List(64) { row }, palette)
        assertEquals(0xFFFFFFFF.toInt(), frame.pixels[0])
    }

    @Test
    fun `unknown palette char resolves to transparent`() {
        val palette = PixelPalette(mapOf('W' to 0xFFFFFFFF.toInt()))
        val row = "X".repeat(64) // 'X' not in palette
        val frame = PixelFrame64(List(64) { row }, palette)
        assertEquals(0x00000000, frame.pixels[0])
    }
}
