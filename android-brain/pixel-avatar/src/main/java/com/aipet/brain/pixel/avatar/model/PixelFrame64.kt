package com.aipet.brain.pixel.avatar.model

/**
 * A single 64×64 pixel frame for the pet avatar.
 *
 * Represented as 64 strings, each exactly 64 chars wide. Every char is a key
 * into [palette]. The resolved ARGB pixel array is built lazily and cached.
 *
 * Construction validates dimensions eagerly so malformed frames are caught
 * at definition time, not at render time.
 */
class PixelFrame64(
    val rows: List<String>,
    val palette: PixelPalette = PixelPalette.DEFAULT_EYE_PALETTE
) {
    init {
        require(rows.size == HEIGHT) {
            "PixelFrame64 requires exactly $HEIGHT rows, got ${rows.size}"
        }
        rows.forEachIndexed { index, row ->
            require(row.length == WIDTH) {
                "PixelFrame64 row $index must be exactly $WIDTH chars, got ${row.length}"
            }
        }
    }

    /** Lazily-built flat ARGB pixel array in row-major order (top-left to bottom-right). */
    val pixels: IntArray by lazy {
        IntArray(WIDTH * HEIGHT) { i ->
            val row = i / WIDTH
            val col = i % WIDTH
            palette.colorFor(rows[row][col])
        }
    }

    companion object {
        const val WIDTH = 64
        const val HEIGHT = 64
    }
}
