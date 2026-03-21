package com.aipet.brain.ui.avatar.pixel.render

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.pixel.model.PixelColor
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette

@Composable
fun PixelFrameCanvas(
    frame: PixelFrame64,
    modifier: Modifier = Modifier,
    size: Dp = 256.dp,
    canvasBackgroundColor: Color = Color.Transparent
) {
    val renderBuffer = remember(frame) {
        PixelRenderBuffer64.fromFrame(frame)
    }

    Canvas(
        modifier = modifier.size(size)
    ) {
        if (canvasBackgroundColor.alpha > 0f) {
            drawRect(color = canvasBackgroundColor)
        }

        val layout = PixelCanvasScaling.calculateLayout(
            canvasWidthPx = this.size.width,
            canvasHeightPx = this.size.height,
            frameWidth = PixelFrame64.WIDTH,
            frameHeight = PixelFrame64.HEIGHT
        )
        val pixelSize = layout.pixelSizePx.toFloat()

        for (y in 0 until PixelFrame64.HEIGHT) {
            for (x in 0 until PixelFrame64.WIDTH) {
                val color = renderBuffer[x, y]
                if (color.isTransparent) {
                    continue
                }

                drawRect(
                    color = color.toComposeColor(),
                    topLeft = Offset(
                        x = layout.offsetXPx + (x * pixelSize),
                        y = layout.offsetYPx + (y * pixelSize)
                    ),
                    size = Size(width = pixelSize, height = pixelSize)
                )
            }
        }
    }
}

private fun PixelColor.toComposeColor(): Color = Color(
    red = red / 255f,
    green = green / 255f,
    blue = blue / 255f,
    alpha = alpha / 255f
)

@Preview(showBackground = true, backgroundColor = 0xFFE8EEF8)
@Composable
private fun PixelFrameCanvasPreview() {
    PixelFrameCanvas(
        frame = sampleEyeOnlyPreviewFrame(),
        size = 256.dp
    )
}

private fun sampleEyeOnlyPreviewFrame(): PixelFrame64 {
    val palette = PixelPetDefaultPalette.palette
    val transparent = palette.indexOf(PixelPetDefaultPalette.TransparentKey)
    val eyeBase = palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
    val pupil = palette.indexOf(PixelPetDefaultPalette.PupilKey)
    val highlight = palette.indexOf(PixelPetDefaultPalette.HighlightKey)
    val accent = palette.indexOf(PixelPetDefaultPalette.AccentKey)

    val pixels = MutableList(PixelFrame64.PIXEL_COUNT) { transparent }

    fun fillRect(xRange: IntRange, yRange: IntRange, colorIndex: Int) {
        for (y in yRange) {
            for (x in xRange) {
                pixels[(y * PixelFrame64.WIDTH) + x] = colorIndex
            }
        }
    }

    fillRect(xRange = 14..25, yRange = 22..37, colorIndex = eyeBase)
    fillRect(xRange = 38..49, yRange = 22..37, colorIndex = eyeBase)
    fillRect(xRange = 18..21, yRange = 26..31, colorIndex = pupil)
    fillRect(xRange = 42..45, yRange = 26..31, colorIndex = pupil)
    fillRect(xRange = 20..20, yRange = 27..27, colorIndex = highlight)
    fillRect(xRange = 44..44, yRange = 27..27, colorIndex = highlight)
    fillRect(xRange = 12..27, yRange = 18..19, colorIndex = accent)
    fillRect(xRange = 36..51, yRange = 18..19, colorIndex = accent)

    return PixelFrame64(
        palette = palette,
        pixelIndices = pixels
    )
}
