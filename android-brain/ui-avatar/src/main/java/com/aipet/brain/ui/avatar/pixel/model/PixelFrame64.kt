package com.aipet.brain.ui.avatar.pixel.model

data class PixelFrame64(
    val palette: PixelPalette,
    val pixelIndices: List<Int>
) {
    init {
        require(pixelIndices.size == PIXEL_COUNT) {
            "PixelFrame64 must contain exactly $PIXEL_COUNT pixels but had ${pixelIndices.size}."
        }
        require(pixelIndices.all { it in 0 until palette.size }) {
            "PixelFrame64 contains a palette index outside 0..${palette.size - 1}."
        }
    }

    operator fun get(x: Int, y: Int): PixelPaletteEntry = palette[colorIndexAt(x = x, y = y)]

    fun colorIndexAt(x: Int, y: Int): Int {
        require(x in 0 until WIDTH) { "x must be between 0 and ${WIDTH - 1}: $x" }
        require(y in 0 until HEIGHT) { "y must be between 0 and ${HEIGHT - 1}: $y" }
        return pixelIndices[(y * WIDTH) + x]
    }

    companion object {
        const val WIDTH: Int = 64
        const val HEIGHT: Int = 64
        const val PIXEL_COUNT: Int = WIDTH * HEIGHT

        fun filled(
            palette: PixelPalette,
            colorIndex: Int
        ): PixelFrame64 {
            require(colorIndex in 0 until palette.size) {
                "colorIndex must be between 0 and ${palette.size - 1}: $colorIndex"
            }
            return PixelFrame64(
                palette = palette,
                pixelIndices = List(PIXEL_COUNT) { colorIndex }
            )
        }
    }
}
