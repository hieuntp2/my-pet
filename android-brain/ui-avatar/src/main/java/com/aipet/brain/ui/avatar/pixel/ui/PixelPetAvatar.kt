package com.aipet.brain.ui.avatar.pixel.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.pixel.bridge.DefaultPixelPetStateMapper
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestrator
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState
import com.aipet.brain.ui.avatar.pixel.catalog.AuthoredPixelPetAnimationPack
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationFrameTicker
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationPlaybackState
import com.aipet.brain.ui.avatar.pixel.render.PixelFrameCanvas
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionStrategy
import kotlinx.coroutines.isActive

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PixelPetAvatar(
    frame: PixelFrame64?,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = { onTap?.invoke() },
                onLongClick = { onLongPress?.invoke() }
            )
    ) {
        frame?.let { safeFrame ->
            PixelFrameCanvas(
                frame = safeFrame,
                modifier = Modifier.fillMaxSize(),
                size = size
            )
        }
    }
}

@Composable
fun AnimatedPixelPetAvatar(
    animationController: PixelAnimationController,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    isPlaying: Boolean = true,
    speedMultiplier: Float = 1f,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onPlaybackStateChanged: ((PixelAnimationPlaybackState?) -> Unit)? = null
) {
    require(speedMultiplier > 0f) { "speedMultiplier must be greater than 0." }

    var currentFrame by remember(animationController) {
        mutableStateOf(animationController.getCurrentFrame())
    }
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val currentSpeedMultiplier by rememberUpdatedState(speedMultiplier)
    val frameTicker = remember(animationController) { PixelAnimationFrameTicker() }

    LaunchedEffect(animationController) {
        frameTicker.reset()
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                val deltaMillis = frameTicker.nextDeltaMillis(
                    frameTimeNanos = frameTimeNanos,
                    isPlaying = currentIsPlaying,
                    speedMultiplier = currentSpeedMultiplier
                )
                if (deltaMillis > 0L) {
                    animationController.update(deltaMillis = deltaMillis)
                }
                val playbackState = animationController.getPlaybackState()
                currentFrame = playbackState?.currentFrame
                onPlaybackStateChanged?.invoke(playbackState)
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

@Composable
fun OrchestratedPixelPetAvatar(
    orchestrator: PixelAnimationOrchestrator<PixelPetBridgeState>,
    animationController: PixelAnimationController,
    bridgeState: PixelPetBridgeState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    isPlaying: Boolean = true,
    speedMultiplier: Float = 1f,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onPlaybackStateChanged: ((PixelAnimationPlaybackState?) -> Unit)? = null
) {
    LaunchedEffect(orchestrator, bridgeState) {
        orchestrator.synchronize(bridgeState)
    }

    AnimatedPixelPetAvatar(
        animationController = animationController,
        modifier = modifier,
        size = size,
        isPlaying = isPlaying,
        speedMultiplier = speedMultiplier,
        onTap = onTap,
        onLongPress = onLongPress,
        onPlaybackStateChanged = onPlaybackStateChanged
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F6FA)
@Composable
private fun NeutralVariantPackPreview() {
    val registry = remember { AuthoredPixelPetAnimationPack.createRegistry() }
    val controller = remember { PixelAnimationController() }
    val neutralVariants = remember(registry) {
        registry.requireAnimationSet(Neutral).variants
    }
    var variantIndex by remember { mutableStateOf(0) }
    val inspectionMode = LocalInspectionMode.current

    LaunchedEffect(neutralVariants, variantIndex) {
        controller.setClip(neutralVariants[variantIndex].clip)
    }

    LaunchedEffect(neutralVariants, inspectionMode) {
        if (!inspectionMode) {
            while (isActive) {
                kotlinx.coroutines.delay(NEUTRAL_PREVIEW_VARIANT_DURATION_MILLIS)
                variantIndex = (variantIndex + 1) % neutralVariants.size
            }
        }
    }

    Surface {
        AnimatedPixelPetAvatar(
            animationController = controller,
            size = 220.dp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF4F6FA)
@Composable
private fun PixelPetAvatarPreview() {
    val registry = remember { AuthoredPixelPetAnimationPack.createRegistry() }
    val controller = remember { PixelAnimationController() }
    val selector = remember {
        PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.SimpleRoundRobin
        )
    }
    val orchestrator = remember {
        PixelAnimationOrchestrator(
            stateMapper = DefaultPixelPetStateMapper(),
            animationSetRegistry = registry,
            variantSelector = selector,
            animationController = controller
        )
    }
    val previewStates = remember {
        listOf(
            PixelPetBridgeState(PixelPetAvatarIntent.NEUTRAL),
            PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED),
            PixelPetBridgeState(PixelPetAvatarIntent.ATTENTIVE),
            PixelPetBridgeState(PixelPetAvatarIntent.PROCESSING),
            PixelPetBridgeState(PixelPetAvatarIntent.LOW_ENERGY)
        )
    }
    var previewIndex by remember { mutableStateOf(0) }
    val inspectionMode = LocalInspectionMode.current

    LaunchedEffect(inspectionMode) {
        if (!inspectionMode) {
            while (isActive) {
                kotlinx.coroutines.delay(PREVIEW_STATE_DURATION_MILLIS)
                previewIndex = (previewIndex + 1) % previewStates.size
            }
        }
    }

    Surface {
        OrchestratedPixelPetAvatar(
            orchestrator = orchestrator,
            animationController = controller,
            bridgeState = previewStates[previewIndex],
            size = 220.dp
        )
    }
}

private val PixelAnimationPlaybackState.currentFrame: PixelFrame64
    get() = clip.frames[frameIndex].frame

val PixelAnimationVariant.debugSummary: String
    get() = "${clip.id} (${clip.frames.size}f, ${clip.totalDurationMillis}ms, ${clip.playbackMode.debugLabel})"

val PixelAnimationPlaybackMode.debugLabel: String
    get() = when (this) {
        PixelAnimationPlaybackMode.LOOP -> "loop"
        PixelAnimationPlaybackMode.ONE_SHOT -> "one-shot"
    }

private const val PREVIEW_STATE_DURATION_MILLIS = 1_800L
private const val NEUTRAL_PREVIEW_VARIANT_DURATION_MILLIS = 1_600L
