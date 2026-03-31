package com.aipet.brain.ui.avatar.pixel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.pixel.animation.FaceAnimationContext
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationFrameTicker
import com.aipet.brain.ui.avatar.pixel.runtime.FaceAnimationRuntime
import kotlinx.coroutines.isActive

/**
 * Composable that drives a [FaceAnimationRuntime] via a withFrameNanos loop and
 * renders the resulting [PixelFrame64] frame each tick.
 *
 * Unlike [OrchestratedPixelPetAvatar] (which plays pre-baked clip files),
 * this composable produces every frame by compositing live layer state through
 * [FaceAnimationRuntime.update] → PetEyeRenderer each render pass.
 *
 * @param runtime The FaceAnimationRuntime instance (created + held externally via remember).
 * @param context Current animation context (intent + pet vitals). Reread each frame.
 * @param isPlaying When false the animation loop pauses (frame ticker halts delta output).
 */
@Composable
fun LiveFaceAnimationCanvas(
    runtime: FaceAnimationRuntime,
    context: FaceAnimationContext,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    isPlaying: Boolean = true,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
) {
    var currentFrame: PixelFrame64? by remember(runtime) { mutableStateOf(null) }
    val currentContext by rememberUpdatedState(context)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val frameTicker = remember(runtime) { PixelAnimationFrameTicker() }

    LaunchedEffect(runtime) {
        frameTicker.reset()
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val deltaMillis = frameTicker.nextDeltaMillis(
                    frameTimeNanos = frameTimeNanos,
                    isPlaying = currentIsPlaying,
                    speedMultiplier = 1f
                )
                if (deltaMillis > 0L) {
                    currentFrame = runtime.update(deltaMillis, currentContext)
                }
            }
        }
    }

    PixelPetAvatar(
        frame = currentFrame,
        modifier = modifier,
        size = size,
        onTap = onTap,
        onLongPress = onLongPress
    )
}
