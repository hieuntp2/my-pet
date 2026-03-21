package com.aipet.brain.ui.avatar.pixel.model

data class PixelColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = OPAQUE_ALPHA
) {
    init {
        require(red in COLOR_CHANNEL_RANGE) { "red must be between 0 and 255: $red" }
        require(green in COLOR_CHANNEL_RANGE) { "green must be between 0 and 255: $green" }
        require(blue in COLOR_CHANNEL_RANGE) { "blue must be between 0 and 255: $blue" }
        require(alpha in COLOR_CHANNEL_RANGE) { "alpha must be between 0 and 255: $alpha" }
    }

    val isTransparent: Boolean = alpha == TRANSPARENT_ALPHA

    companion object {
        private const val TRANSPARENT_ALPHA = 0
        private const val OPAQUE_ALPHA = 255
        private val COLOR_CHANNEL_RANGE = 0..255

        val Transparent = PixelColor(red = 0, green = 0, blue = 0, alpha = TRANSPARENT_ALPHA)
    }
}
