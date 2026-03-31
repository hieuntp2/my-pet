package com.aipet.brain.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight floating text bubble that appears just below the pet face.
 * [bubble] should be null when no message is active; the composable fades in/out gracefully.
 */
@Composable
fun HomeTalkBubble(
    bubble: HomeTalkBubble?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bubble != null,
        enter = fadeIn(animationSpec = tween(durationMillis = 280)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 280),
                    initialOffsetY = { it / 3 }
                ),
        exit = fadeOut(animationSpec = tween(durationMillis = 220)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 220),
                    targetOffsetY = { it / 4 }
                ),
        modifier = modifier
    ) {
        val message = bubble?.message ?: return@AnimatedVisibility
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    color = Color(0xCC1A1A2E),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = message,
                color = Color(0xE0E8F4FF),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
