package com.aipet.brain.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion

/**
 * Full-screen Canvas that draws a subtle radial glow centred on the pet face stage.
 * The glow color and intensity adapt to the current pet emotion and conditions,
 * giving the stage subtle ambient life without heavy recompositions.
 */
@Composable
fun HomeAmbientGlow(
    emotion: PetEmotion,
    conditions: Set<PetCondition>,
    modifier: Modifier = Modifier
) {
    val targetGlowColor = glowColorFor(emotion, conditions)
    val glowAlpha = remember { Animatable(0.12f) }

    LaunchedEffect(emotion, conditions) {
        val targetAlpha = glowAlphaFor(emotion, conditions)
        glowAlpha.animateTo(
            targetValue = targetAlpha,
            animationSpec = tween(durationMillis = 1_200)
        )
    }

    // Read .value in composable scope so state changes trigger recomposition + redraw
    val currentAlpha = glowAlpha.value

    Canvas(modifier = modifier.fillMaxSize()) {
        val centreX = size.width / 2f
        val centreY = size.height * 0.48f
        val radius = size.width * 0.65f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    targetGlowColor.copy(alpha = currentAlpha),
                    Color.Transparent
                ),
                center = Offset(centreX, centreY),
                radius = radius,
                tileMode = TileMode.Clamp
            ),
            radius = radius,
            center = Offset(centreX, centreY)
        )
    }
}

private fun glowColorFor(emotion: PetEmotion, conditions: Set<PetCondition>): Color {
    return when {
        PetCondition.HUNGRY in conditions -> HomeColors.glowHungry
        PetCondition.SLEEPY in conditions -> HomeColors.glowSleepy
        PetCondition.LONELY in conditions -> HomeColors.glowLonely
        emotion == PetEmotion.HAPPY || emotion == PetEmotion.EXCITED -> HomeColors.glowHappy
        emotion == PetEmotion.CURIOUS -> HomeColors.glowCurious
        emotion == PetEmotion.SAD -> HomeColors.glowSad
        emotion == PetEmotion.SLEEPY -> HomeColors.glowSleepy
        emotion == PetEmotion.HUNGRY -> HomeColors.glowHungry
        else -> HomeColors.glowDefault
    }
}

private fun glowAlphaFor(emotion: PetEmotion, conditions: Set<PetCondition>): Float {
    return when {
        emotion == PetEmotion.EXCITED -> 0.22f
        emotion == PetEmotion.HAPPY -> 0.18f
        PetCondition.HUNGRY in conditions -> 0.16f
        PetCondition.SLEEPY in conditions -> 0.14f
        PetCondition.LONELY in conditions -> 0.13f
        emotion == PetEmotion.SAD -> 0.06f
        else -> 0.11f
    }
}
