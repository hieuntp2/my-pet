package com.aipet.brain.ui.avatar.pixel.renderer

import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette

/**
 * Internal pixel-level eye renderer extracted from AuthoredPixelPetAnimationPack's
 * CanonicalEyeRenderer. Used by FaceAnimationRuntime to produce frames from layer state.
 *
 * All parameters are identical to the original CanonicalEyeRenderer but coerced instead
 * of throwing for out-of-range values (safe for runtime-generated inputs).
 */
internal object PetEyeRenderer {

    private val palette = PixelPetDefaultPalette.palette
    private val transparent = palette.indexOf(PixelPetDefaultPalette.TransparentKey)
    private val eyeBase = palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
    private val pupil = palette.indexOf(PixelPetDefaultPalette.PupilKey)
    val highlight: Int = palette.indexOf(PixelPetDefaultPalette.HighlightKey)
    val accent: Int = palette.indexOf(PixelPetDefaultPalette.AccentKey)

    private const val EYE_TOP = 21
    private const val LEFT_EYE_START_X = 12
    private const val RIGHT_EYE_START_X = 37
    private const val PUPIL_BASE_START_X = 5
    private const val PUPIL_TOP_OFFSET = 5
    private const val EYEBROW_TOP_Y = 17
    const val MAX_PUPIL_OFFSET = 2

    private val eyeRowWidths = listOf(
        4..10, 2..12, 1..13, 0..14, 0..14, 0..14, 0..14, 0..14,
        0..14, 0..14, 0..14, 0..14, 0..14, 1..13, 2..12, 4..10
    )

    fun buildFrame(
        leftPupilOffset: Int = 0,
        rightPupilOffset: Int = 0,
        leftTopLidRows: Int = 0,
        rightTopLidRows: Int = 0,
        leftBottomLidRows: Int = 0,
        rightBottomLidRows: Int = 0,
        leftClosedSlit: Boolean = false,
        rightClosedSlit: Boolean = false,
        eyebrowLeftYOffset: Int = 0,
        eyebrowRightYOffset: Int = 0,
        eyebrowLeftColor: Int = accent,
        eyebrowRightColor: Int = accent,
        extraHighlight: Boolean = false
    ): PixelFrame64 {
        val safeLeftPupil = leftPupilOffset.coerceIn(-MAX_PUPIL_OFFSET, MAX_PUPIL_OFFSET)
        val safeRightPupil = rightPupilOffset.coerceIn(-MAX_PUPIL_OFFSET, MAX_PUPIL_OFFSET)

        val pixels = MutableList(PixelFrame64.PIXEL_COUNT) { transparent }

        drawEyebrowBand(pixels, LEFT_EYE_START_X, eyebrowLeftYOffset, eyebrowLeftColor)
        drawEyebrowBand(pixels, RIGHT_EYE_START_X, eyebrowRightYOffset, eyebrowRightColor)
        drawEyeShell(pixels, LEFT_EYE_START_X)
        drawEyeShell(pixels, RIGHT_EYE_START_X)

        if (leftClosedSlit) {
            drawClosedBlink(pixels, LEFT_EYE_START_X)
        } else {
            drawPupil(pixels, LEFT_EYE_START_X, safeLeftPupil)
            applyLids(pixels, LEFT_EYE_START_X, leftTopLidRows, leftBottomLidRows)
            drawHighlights(pixels, LEFT_EYE_START_X, safeLeftPupil, extraHighlight)
        }

        if (rightClosedSlit) {
            drawClosedBlink(pixels, RIGHT_EYE_START_X)
        } else {
            drawPupil(pixels, RIGHT_EYE_START_X, safeRightPupil)
            applyLids(pixels, RIGHT_EYE_START_X, rightTopLidRows, rightBottomLidRows)
            drawHighlights(pixels, RIGHT_EYE_START_X, safeRightPupil, extraHighlight)
        }

        return PixelFrame64(palette = palette, pixelIndices = pixels)
    }

    private fun drawEyebrowBand(pixels: MutableList<Int>, startX: Int, yOffset: Int, colorIndex: Int) {
        fillRect(pixels, (startX + 3)..(startX + 11), (EYEBROW_TOP_Y + yOffset)..(EYEBROW_TOP_Y + yOffset), colorIndex)
        fillRect(pixels, (startX + 2)..(startX + 12), (EYEBROW_TOP_Y + yOffset + 1)..(EYEBROW_TOP_Y + yOffset + 1), colorIndex)
    }

    private fun drawEyeShell(pixels: MutableList<Int>, startX: Int) {
        eyeRowWidths.forEachIndexed { rowIndex, xRange ->
            fillRect(
                pixels,
                (startX + xRange.first)..(startX + xRange.last),
                (EYE_TOP + rowIndex)..(EYE_TOP + rowIndex),
                eyeBase
            )
        }
    }

    private fun drawPupil(pixels: MutableList<Int>, startX: Int, horizontalOffset: Int) {
        fillRect(
            pixels,
            (startX + PUPIL_BASE_START_X + horizontalOffset)..(startX + PUPIL_BASE_START_X + horizontalOffset + 3),
            (EYE_TOP + PUPIL_TOP_OFFSET)..(EYE_TOP + PUPIL_TOP_OFFSET + 4),
            pupil
        )
        fillRect(
            pixels,
            (startX + PUPIL_BASE_START_X + horizontalOffset + 1)..(startX + PUPIL_BASE_START_X + horizontalOffset + 2),
            (EYE_TOP + PUPIL_TOP_OFFSET - 1)..(EYE_TOP + PUPIL_TOP_OFFSET - 1),
            pupil
        )
    }

    private fun drawHighlights(pixels: MutableList<Int>, startX: Int, horizontalOffset: Int, extraHighlight: Boolean) {
        setPixel(pixels, startX + PUPIL_BASE_START_X + horizontalOffset, EYE_TOP + PUPIL_TOP_OFFSET, highlight)
        setPixel(pixels, startX + PUPIL_BASE_START_X + horizontalOffset + 1, EYE_TOP + PUPIL_TOP_OFFSET + 1, highlight)
        if (extraHighlight) {
            setPixel(pixels, startX + PUPIL_BASE_START_X + horizontalOffset + 2, EYE_TOP + PUPIL_TOP_OFFSET, highlight)
        }
    }

    private fun applyLids(pixels: MutableList<Int>, startX: Int, topRows: Int, bottomRows: Int) {
        val maxRows = eyeRowWidths.size / 2
        repeat(topRows.coerceAtMost(maxRows)) { lidIndex ->
            val row = EYE_TOP + lidIndex
            fillRect(
                pixels,
                (startX + eyeRowWidths[lidIndex].first)..(startX + eyeRowWidths[lidIndex].last),
                row..row,
                accent
            )
        }
        repeat(bottomRows.coerceAtMost(maxRows)) { lidIndex ->
            val templateIndex = eyeRowWidths.lastIndex - lidIndex
            val row = EYE_TOP + templateIndex
            fillRect(
                pixels,
                (startX + eyeRowWidths[templateIndex].first)..(startX + eyeRowWidths[templateIndex].last),
                row..row,
                accent
            )
        }
    }

    private fun drawClosedBlink(pixels: MutableList<Int>, startX: Int) {
        fillRect(pixels, (startX + 2)..(startX + 12), (EYE_TOP + 7)..(EYE_TOP + 7), accent)
        fillRect(pixels, (startX + 1)..(startX + 13), (EYE_TOP + 8)..(EYE_TOP + 8), accent)
        fillRect(pixels, (startX + 2)..(startX + 12), (EYE_TOP + 9)..(EYE_TOP + 9), highlight)
    }

    private fun fillRect(pixels: MutableList<Int>, xRange: IntRange, yRange: IntRange, colorIndex: Int) {
        for (y in yRange) for (x in xRange) setPixel(pixels, x, y, colorIndex)
    }

    private fun setPixel(pixels: MutableList<Int>, x: Int, y: Int, colorIndex: Int) {
        if (x in 0 until PixelFrame64.WIDTH && y in 0 until PixelFrame64.HEIGHT) {
            pixels[(y * PixelFrame64.WIDTH) + x] = colorIndex
        }
    }
}
