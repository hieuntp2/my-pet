package com.aipet.brain.pixel.avatar.renderer

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.aipet.brain.pixel.avatar.model.PixelFrame64

/**
 * Converts a [PixelFrame64] to an [ImageBitmap] suitable for crisp nearest-neighbor rendering.
 *
 * The bitmap is ARGB_8888, 64×64 pixels. Nearest-neighbor scaling is enforced at
 * the composable layer (FilterQuality.None) — this function only handles pixel data.
 *
 * Each call creates a new Bitmap. The caller (PixelAnimationController) is responsible
 * for caching converted bitmaps per clip to avoid per-frame allocation.
 */
fun PixelFrame64.toImageBitmap(): ImageBitmap {
    val bitmap = Bitmap.createBitmap(
        PixelFrame64.WIDTH,
        PixelFrame64.HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    bitmap.setPixels(
        pixels,
        0,                // offset
        PixelFrame64.WIDTH, // stride
        0, 0,
        PixelFrame64.WIDTH,
        PixelFrame64.HEIGHT
    )
    return bitmap.asImageBitmap()
}
