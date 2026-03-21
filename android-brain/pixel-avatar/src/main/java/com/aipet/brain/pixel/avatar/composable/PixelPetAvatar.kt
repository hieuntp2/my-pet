package com.aipet.brain.pixel.avatar.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipet.brain.pixel.avatar.model.PixelFrame64
import com.aipet.brain.pixel.avatar.renderer.toImageBitmap

/**
 * Displays a single [PixelFrame64] at the requested size with crisp nearest-neighbor scaling.
 *
 * This composable is stateless: it only renders whatever frame it receives.
 * Animation timing and frame selection belong to [PixelAnimationController], not here.
 *
 * @param frame       The frame to display.
 * @param displaySize The target square display size in dp. Default is 320dp (standard).
 * @param modifier    Optional modifier forwarded to the outer Box.
 * @param showDebugGrid If true, overlays a center-cross guide (debug mode only).
 */
@Composable
fun PixelPetAvatar(
    frame: PixelFrame64,
    displaySize: Dp = 320.dp,
    modifier: Modifier = Modifier,
    showDebugGrid: Boolean = false
) {
    val imageBitmap = remember(frame) { frame.toImageBitmap() }

    Box(
        modifier = modifier.size(displaySize),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = imageBitmap,
            contentDescription = null,
            modifier = Modifier.size(displaySize),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None // nearest-neighbor — never blur pixel art
        )

        if (showDebugGrid) {
            PixelDebugOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PixelDebugOverlay(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Vertical center line
        Box(
            modifier = Modifier
                .size(width = 1.dp, height = 9999.dp)
                .align(Alignment.Center)
                .background(Color(0x55FF0000))
        )
        // Horizontal center line
        Box(
            modifier = Modifier
                .size(width = 9999.dp, height = 1.dp)
                .align(Alignment.Center)
                .background(Color(0x55FF0000))
        )
    }
}
