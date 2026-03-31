package com.aipet.brain.app.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders ephemeral particle FX overlaid on the Home stage scene.
 * Particles are drawn per-frame on a Canvas; no heavy allocation per frame.
 *
 * [fxType] controls which particles to draw. [onDone] is invoked when the animation
 * cycle is complete so the caller can clear the active FX type.
 */
@Composable
fun HomeFxOverlay(
    fxType: HomeFxType,
    modifier: Modifier = Modifier,
    onDone: () -> Unit
) {
    val progress = remember(fxType) { Animatable(0f) }

    LaunchedEffect(fxType) {
        progress.snapTo(0f)
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fx_duration_ms(fxType), easing = LinearEasing)
            )
            onDone()
        }
    }

    // Read in composable scope so recomposition fires on each animation frame
    val p = progress.value

    Canvas(modifier = modifier.fillMaxSize()) {
        when (fxType) {
            HomeFxType.HEARTS -> drawHeartsFx(p, center)
            HomeFxType.SPARKS -> drawSparksFx(p, center)
            HomeFxType.ZZZ -> drawZzzFx(p, center)
            HomeFxType.EXCLAMATION -> drawExclamationFx(p, center)
        }
    }
}

private fun fx_duration_ms(type: HomeFxType): Int = when (type) {
    HomeFxType.HEARTS -> 2_000
    HomeFxType.SPARKS -> 1_200
    HomeFxType.ZZZ -> 2_500
    HomeFxType.EXCLAMATION -> 900
}

// ─── Hearts ─────────────────────────────────────────────────────────────────

private val HEART_OFFSETS = listOf(
    Offset(-60f, -80f),
    Offset(30f, -110f),
    Offset(90f, -60f),
    Offset(-40f, -140f),
    Offset(60f, -130f)
)
private val heartColors = listOf(
    HomeColors.heartPrimary,
    HomeColors.heartLight,
    HomeColors.heartVibrant,
    HomeColors.heartPale,
    HomeColors.heartWarm
)

// PERF: Pre-allocated Compose Path reused across drawHeart calls. Canvas draw lambdas
// always execute on the main thread, so a single shared instance is safe here.
private val sharedHeartPath = Path()


private fun DrawScope.drawHeartsFx(progress: Float, stageCentre: Offset) {
    HEART_OFFSETS.forEachIndexed { index, baseOffset ->
        val delay = index * 0.12f
        val localProgress = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (localProgress <= 0f) return@forEachIndexed
        val opacity = (1f - localProgress).coerceIn(0f, 1f)
        val rise = localProgress * 120f
        val cx = stageCentre.x + baseOffset.x
        val cy = stageCentre.y + baseOffset.y - rise
        drawHeart(
            centre = Offset(cx, cy),
            size = 18f,
            color = heartColors[index % heartColors.size].copy(alpha = opacity)
        )
    }
}

private fun DrawScope.drawHeart(centre: Offset, size: Float, color: Color) {
    // PERF: Reset and reuse the shared path to avoid per-frame Path allocation.
    sharedHeartPath.reset()
    sharedHeartPath.apply {
        moveTo(centre.x, centre.y + size * 0.35f)
        cubicTo(
            centre.x - size * 0.9f, centre.y - size * 0.1f,
            centre.x - size * 0.9f, centre.y - size * 0.8f,
            centre.x, centre.y - size * 0.3f
        )
        cubicTo(
            centre.x + size * 0.9f, centre.y - size * 0.8f,
            centre.x + size * 0.9f, centre.y - size * 0.1f,
            centre.x, centre.y + size * 0.35f
        )
        close()
    }
    drawPath(path = sharedHeartPath, color = color)
}

// ─── Sparks ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawSparksFx(progress: Float, stageCentre: Offset) {
    val sparkCount = 8
    repeat(sparkCount) { i ->
        val angle = (360f / sparkCount) * i
        val rad = Math.toRadians(angle.toDouble())
        val maxLen = 80f
        val startLen = maxLen * 0.3f
        val endLen = maxLen * progress
        val opacity = (1f - progress).coerceIn(0f, 1f)
        val startX = stageCentre.x + (cos(rad) * startLen).toFloat()
        val startY = stageCentre.y + (sin(rad) * startLen).toFloat()
        val endX = stageCentre.x + (cos(rad) * endLen).toFloat()
        val endY = stageCentre.y + (sin(rad) * endLen).toFloat()
        drawLine(
            color = HomeColors.accentCyan.copy(alpha = opacity),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.5f
        )
    }
}

// ─── ZZZ ─────────────────────────────────────────────────────────────────────

private fun DrawScope.drawZzzFx(progress: Float, stageCentre: Offset) {
    val zPositions = listOf(
        Offset(stageCentre.x + 80f, stageCentre.y - 80f),
        Offset(stageCentre.x + 110f, stageCentre.y - 130f),
        Offset(stageCentre.x + 90f, stageCentre.y - 180f)
    )
    val sizes = listOf(14f, 18f, 22f)
    zPositions.forEachIndexed { index, basePos ->
        val delay = index * 0.2f
        val localProgress = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
        if (localProgress <= 0f) return@forEachIndexed
        val opacity = (sin(localProgress * Math.PI.toFloat())).coerceIn(0f, 1f) * 0.85f
        val rise = localProgress * 30f
        drawZGlyph(
            centre = Offset(basePos.x, basePos.y - rise),
            size = sizes[index],
            color = HomeColors.zzzBlue.copy(alpha = opacity)
        )
    }
}

private fun DrawScope.drawZGlyph(centre: Offset, size: Float, color: Color) {
    val half = size / 2f
    // Top horizontal bar
    drawLine(color = color, start = Offset(centre.x - half, centre.y - half),
        end = Offset(centre.x + half, centre.y - half), strokeWidth = size * 0.2f)
    // Diagonal
    drawLine(color = color, start = Offset(centre.x + half, centre.y - half),
        end = Offset(centre.x - half, centre.y + half), strokeWidth = size * 0.2f)
    // Bottom horizontal bar
    drawLine(color = color, start = Offset(centre.x - half, centre.y + half),
        end = Offset(centre.x + half, centre.y + half), strokeWidth = size * 0.2f)
}

// ─── Exclamation ─────────────────────────────────────────────────────────────

private fun DrawScope.drawExclamationFx(progress: Float, stageCentre: Offset) {
    val opacity = (1f - progress * 1.5f).coerceIn(0f, 1f)
    val scale = if (progress < 0.3f) (progress / 0.3f) else 1f
    val cx = stageCentre.x
    val cy = stageCentre.y - 140f
    drawCircle(
        color = HomeColors.exclamationYellow.copy(alpha = opacity * 0.25f * scale),
        radius = 30f * scale,
        center = Offset(cx, cy)
    )
    drawLine(
        color = HomeColors.exclamationYellow.copy(alpha = opacity),
        start = Offset(cx, cy - 20f * scale),
        end = Offset(cx, cy + 8f * scale),
        strokeWidth = 5f * scale
    )
    drawCircle(
        color = HomeColors.exclamationYellow.copy(alpha = opacity),
        radius = 3.5f * scale,
        center = Offset(cx, cy + 16f * scale)
    )
}
