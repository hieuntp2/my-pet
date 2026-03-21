package com.aipet.brain.pixel.avatar.model

/**
 * A stable small palette for pixel art eye animation frames.
 *
 * Each frame is a 64×64 grid of palette chars. This type maps each char key
 * to an ARGB Int color. Unknown chars resolve to fully transparent (0x00000000).
 */
data class PixelPalette(val entries: Map<Char, Int>) {

    /** Resolve a palette char to its ARGB Int. Returns transparent if not found. */
    fun colorFor(key: Char): Int = entries[key] ?: TRANSPARENT

    companion object {
        const val TRANSPARENT = 0x00000000

        /**
         * Default 6-color palette for eye-only pet frames.
         *
         *  '.' = transparent / void
         *  'B' = background face fill (dark near-black)
         *  'W' = eye white / sclera
         *  'P' = pupil / dark iris
         *  'H' = highlight dot
         *  'A' = accent / emotion color (e.g. sparkle, blush)
         */
        val DEFAULT_EYE_PALETTE: PixelPalette = PixelPalette(
            entries = mapOf(
                '.' to 0x00000000, // transparent
                'B' to 0xFF1A1A2E.toInt(), // dark background
                'W' to 0xFFE8E0D0.toInt(), // eye sclera / white
                'P' to 0xFF0D0D0D.toInt(), // pupil dark
                'H' to 0xFFFFFFFF.toInt(), // highlight pure white
                'A' to 0xFF5BBFFF.toInt()  // accent blue (calm)
            )
        )
    }
}
