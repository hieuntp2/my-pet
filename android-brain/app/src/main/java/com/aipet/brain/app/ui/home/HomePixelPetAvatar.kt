package com.aipet.brain.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.ui.avatar.pixel.animation.AnimationPriority
import com.aipet.brain.ui.avatar.pixel.animation.FaceAnimationContext
import com.aipet.brain.ui.avatar.pixel.animation.FaceReactionType
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState
import com.aipet.brain.ui.avatar.pixel.runtime.FaceAnimationRuntime
import com.aipet.brain.ui.avatar.pixel.ui.LiveFaceAnimationCanvas
import kotlin.random.Random

/**
 * Home-stage pixel pet avatar using the layered FaceAnimationRuntime.
 *
 * Replaces the clip-based OrchestratedPixelPetAvatar pipeline with a real-time layer
 * compositor that produces frames from floating-point layer state each tick.
 *
 *  - Breathing idle float (±FLOAT_AMPLITUDE_DP via graphicsLayer draw-phase — no recomposition)
 *  - FaceAnimationRuntime drives all layers (blink, eyelid, gaze, expression)
 *  - IdleDirector selects behavior family (CALM / CURIOUS / SLEEPY / EXPECTANT / PLAYFUL)
 *    from the bridge intent and pet vitals.
 *  - PetReactionController triggers map to FaceReactionType sequences on the runtime.
 *  - State leakage: sleepiness → drooped lids, hunger → concerned brow, low energy → slow motion.
 *
 * @param displaySize Controls the rendered pixel pet size. Default 300.dp for the Home stage.
 */
@Composable
fun HomePixelPetAvatar(
    bridgeState: PixelPetBridgeState,
    reactionController: PetReactionController,
    petState: PetState? = null,
    modifier: Modifier = Modifier,
    displaySize: Dp = 300.dp,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    // ── Breathing idle float (draw-phase only — no layout invalidation at 60fps) ─────────────
    val infiniteTransition = rememberInfiniteTransition(label = "idle_float")
    val floatYState = infiniteTransition.animateFloat(
        initialValue = -FLOAT_AMPLITUDE_DP,
        targetValue = FLOAT_AMPLITUDE_DP,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FLOAT_CYCLE_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // ── Layered animation runtime ─────────────────────────────────────────────────────────────
    val runtime = remember { FaceAnimationRuntime() }

    // Wire reaction-complete callback back to PetReactionController
    val currentController by rememberUpdatedState(reactionController)
    DisposableEffect(runtime) {
        runtime.reactionCompleteListener = { currentController.onReactionClipCompleted() }
        onDispose { runtime.reactionCompleteListener = null }
    }

    // Forward active reactions from PetReactionController → FaceAnimationRuntime
    val activeReaction = reactionController.activeReaction
    LaunchedEffect(activeReaction) {
        if (activeReaction != null) {
            val reactionType = activeReaction.intent.toFaceReactionType() ?: return@LaunchedEffect
            runtime.triggerReaction(reactionType, activeReaction.priority)
        }
    }

    // Build context from bridge state + pet vitals for state leakage
    val context = FaceAnimationContext(
        intent = bridgeState.intent,
        energy = petState?.energy ?: 100,
        hunger = petState?.hunger ?: 0,
        sleepiness = petState?.sleepiness ?: 0,
        social = petState?.social ?: 50,
        bond = petState?.bond ?: 50
    )

    LiveFaceAnimationCanvas(
        runtime = runtime,
        context = context,
        // graphicsLayer confines floatYState.value read to draw phase — no recomposition cost.
        modifier = modifier.graphicsLayer { translationY = floatYState.value * density },
        size = displaySize,
        onTap = onTap,
        onLongPress = onLongPress
    )
}

/**
 * Maps a PixelPetAvatarIntent that originates from PetReactionController into the
 * corresponding FaceReactionType used by FaceAnimationRuntime.
 * Returns null for intents that have no specific reaction sequence (handled as idle state).
 *
 * ENGAGED randomly picks between TAP_ENGAGED (70%) and TAP_PLAYFUL (30%) for variety.
 */
private fun PixelPetAvatarIntent.toFaceReactionType(): FaceReactionType? = when (this) {
    PixelPetAvatarIntent.SURPRISED -> FaceReactionType.STARTLED_SNAP
    PixelPetAvatarIntent.EXCITED -> FaceReactionType.EXCITED_GREETING
    PixelPetAvatarIntent.ENGAGED -> if (Random.nextFloat() < 0.30f) FaceReactionType.TAP_PLAYFUL else FaceReactionType.TAP_ENGAGED
    PixelPetAvatarIntent.LONG_PRESS -> FaceReactionType.LONG_PRESS_CUDDLE
    PixelPetAvatarIntent.ANNOYED -> FaceReactionType.TAP_ANNOYED
    PixelPetAvatarIntent.ATTENTIVE -> FaceReactionType.KEYWORD_ATTENTIVE
    PixelPetAvatarIntent.GAME_CELEBRATE -> FaceReactionType.GAME_CELEBRATE
    PixelPetAvatarIntent.GAME_FAIL -> FaceReactionType.GAME_FAIL
    else -> null
}

private const val FLOAT_AMPLITUDE_DP = 3f
private const val FLOAT_CYCLE_MS = 3200

