package com.aipet.brain.ui.avatar.pixel.render

import com.aipet.brain.ui.avatar.pixel.model.PixelColor
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64

data class PixelRenderBuffer64(
    val rows: List<List<PixelColor>>
) {
    init {
        require(rows.size == PixelFrame64.HEIGHT) {
            "PixelRenderBuffer64 must contain exactly ${PixelFrame64.HEIGHT} rows but had ${rows.size}."
        }
        require(rows.all { it.size == PixelFrame64.WIDTH }) {
            "Each PixelRenderBuffer64 row must contain exactly ${PixelFrame64.WIDTH} pixels."
        }
    }

    operator fun get(x: Int, y: Int): PixelColor {
        require(x in 0 until PixelFrame64.WIDTH) {
            "x must be between 0 and ${PixelFrame64.WIDTH - 1}: $x"
        }
        require(y in 0 until PixelFrame64.HEIGHT) {
            "y must be between 0 and ${PixelFrame64.HEIGHT - 1}: $y"
        }
        return rows[y][x]
    }

    companion object {
        fun fromFrame(frame: PixelFrame64): PixelRenderBuffer64 {
            val rows = List(PixelFrame64.HEIGHT) { y ->
                List(PixelFrame64.WIDTH) { x ->
                    frame[x, y].color
                }
            }
            return PixelRenderBuffer64(rows = rows)
        }
    }
}
