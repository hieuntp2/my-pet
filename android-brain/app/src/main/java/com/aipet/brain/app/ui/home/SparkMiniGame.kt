package com.aipet.brain.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ─── Game constants ───────────────────────────────────────────────────────────

private const val SPARK_COUNT = 5
private const val GAME_DURATION_MS = 8_000L
private const val RESULT_DISPLAY_MS = 1_800L
private const val COOLDOWN_MS = 20_000L
private const val TICK_INTERVAL_MS = 50L
// How long the pet waits for the user to accept an invite before dismissing it.
private const val INVITE_TIMEOUT_MS = 10_000L

// Spark tap radius in Dp converted to a fraction of the overlay container width.
// This represents ~52 px on a 400px-wide screen — generous enough for a finger tap.
private const val SPARK_TAP_FRACTION = 0.13f

// Sparks orbit the face centre which is at (0.5, 0.48) of the stage.
private const val CENTRE_X = 0.5f
private const val CENTRE_Y = 0.48f

// Sparks are distributed on an ellipse around the face to avoid occlusion.
private const val ORBIT_RADIUS_X = 0.28f
private const val ORBIT_RADIUS_Y = 0.22f

// ─── State management ─────────────────────────────────────────────────────────

/**
 * Creates and manages the state for the Catch-the-Spark mini-game.
 * The returned state is read by [SparkGameOverlay].
 *
 * @param onWin         Called when all sparks are caught; the caller should update pet state via play.
 * @param onFail        Called when the timer expires; caller may show a mild sadness reaction.
 * @param onInviteIgnored Called when the pet's autonomous invitation times out without user response.
 */
@Composable
fun rememberSparkGameController(
    onWin: () -> Unit,
    onFail: () -> Unit,
    onInviteIgnored: () -> Unit = {}
): SparkGameController {
    val onWinRef = rememberUpdatedState(onWin)
    val onFailRef = rememberUpdatedState(onFail)
    val onInviteIgnoredRef = rememberUpdatedState(onInviteIgnored)

    return remember {
        SparkGameController(
            onWin = { onWinRef.value() },
            onFail = { onFailRef.value() },
            onInviteIgnored = { onInviteIgnoredRef.value() }
        )
    }
}

class SparkGameController(
    private val onWin: () -> Unit,
    private val onFail: () -> Unit,
    private val onInviteIgnored: () -> Unit = {}
) {
    var state by mutableStateOf(SparkGameState())
        private set

    private var lastTickMs by mutableLongStateOf(0L)

    /** Called by the autonomous invitation engine to begin the pet-driven invite sequence. */
    fun startInvite() {
        if (state.phase != SparkGamePhase.INACTIVE) return
        state = SparkGameState(phase = SparkGamePhase.INVITE)
    }

    /** Called when the user taps the pet during the INVITE phase to accept and begin the game. */
    fun acceptInvite() {
        if (state.phase != SparkGamePhase.INVITE) return
        val sparks = buildSparkPositions()
        state = SparkGameState(
            phase = SparkGamePhase.ACTIVE,
            sparks = sparks,
            remainingMs = GAME_DURATION_MS,
            caughtCount = 0
        )
        lastTickMs = System.currentTimeMillis()
    }

    /** Dismisses the invite (e.g. timeout, higher-priority event). Records ignore. */
    fun dismissInvite() {
        if (state.phase != SparkGamePhase.INVITE) return
        state = SparkGameState(phase = SparkGamePhase.INACTIVE)
        onInviteIgnored()
    }

    fun startGame() {
        if (state.phase != SparkGamePhase.INACTIVE) return
        val sparks = buildSparkPositions()
        state = SparkGameState(
            phase = SparkGamePhase.ACTIVE,
            sparks = sparks,
            remainingMs = GAME_DURATION_MS,
            caughtCount = 0
        )
        lastTickMs = System.currentTimeMillis()
    }

    fun tapSpark(xFraction: Float, yFraction: Float) {
        if (state.phase != SparkGamePhase.ACTIVE) return
        val tapped = state.sparks.firstOrNull { spark ->
            spark.alive &&
                    abs(spark.xFraction - xFraction) < SPARK_TAP_FRACTION &&
                    abs(spark.yFraction - yFraction) < SPARK_TAP_FRACTION
        } ?: return
        val updatedSparks = state.sparks.map { if (it.id == tapped.id) it.copy(alive = false) else it }
        val caught = state.caughtCount + 1
        state = state.copy(sparks = updatedSparks, caughtCount = caught)
        if (updatedSparks.none { it.alive }) {
            triggerWin()
        }
    }

    fun tick() {
        if (state.phase != SparkGamePhase.ACTIVE) return
        val now = System.currentTimeMillis()
        val elapsed = if (lastTickMs > 0L) now - lastTickMs else TICK_INTERVAL_MS
        lastTickMs = now
        val remaining = state.remainingMs - elapsed
        if (remaining <= 0L) {
            state = state.copy(remainingMs = 0L)
            triggerFail()
        } else {
            state = state.copy(remainingMs = remaining)
        }
    }

    private fun triggerWin() {
        state = state.copy(phase = SparkGamePhase.WIN)
        onWin()
    }

    private fun triggerFail() {
        state = state.copy(phase = SparkGamePhase.LOSE)
        onFail()
    }

    fun transitionToCooldown() {
        if (state.phase == SparkGamePhase.WIN || state.phase == SparkGamePhase.LOSE) {
            state = state.copy(phase = SparkGamePhase.COOLDOWN)
        }
    }

    fun resetToInactive() {
        state = SparkGameState(phase = SparkGamePhase.INACTIVE)
    }

    suspend fun awaitResultAndCooldown() {
        // Only relevant when coming from WIN or LOSE phase
        if (state.phase !in listOf(SparkGamePhase.WIN, SparkGamePhase.LOSE)) return
        delay(RESULT_DISPLAY_MS)
        transitionToCooldown()
        delay(COOLDOWN_MS)
        resetToInactive()
    }
}

private fun buildSparkPositions(): List<HomeSpark> {
    return (0 until SPARK_COUNT).map { i ->
        val angleRad = (2 * Math.PI / SPARK_COUNT) * i + Random.nextDouble(-0.3, 0.3)
        val jitter = Random.nextFloat() * 0.05f - 0.025f
        HomeSpark(
            id = i,
            xFraction = CENTRE_X + (cos(angleRad) * (ORBIT_RADIUS_X + jitter)).toFloat(),
            yFraction = CENTRE_Y + (sin(angleRad) * (ORBIT_RADIUS_Y + jitter)).toFloat(),
            alive = true
        )
    }
}

// ─── Overlay composable ───────────────────────────────────────────────────────

/**
 * Full-stage overlay that renders sparks and handles tap detection during an active game.
 */
@Composable
fun SparkGameOverlay(
    controller: SparkGameController,
    modifier: Modifier = Modifier
) {
    val state = controller.state

    // Phase-specific lifecycle: each phase has its own coroutine keyed on that phase value.
    // Reading controller.state.phase inside coroutines avoids stale captures.
    LaunchedEffect(state.phase) {
        when (state.phase) {
            SparkGamePhase.INVITE -> {
                // Timeout: if user doesn't respond within INVITE_TIMEOUT_MS, dismiss the invite.
                delay(INVITE_TIMEOUT_MS)
                if (controller.state.phase == SparkGamePhase.INVITE) {
                    controller.dismissInvite()
                }
            }
            SparkGamePhase.ACTIVE -> {
                while (controller.state.phase == SparkGamePhase.ACTIVE) {
                    delay(TICK_INTERVAL_MS)
                    controller.tick()
                }
            }
            SparkGamePhase.WIN, SparkGamePhase.LOSE -> {
                delay(RESULT_DISPLAY_MS)
                controller.transitionToCooldown()
            }
            SparkGamePhase.COOLDOWN -> {
                delay(COOLDOWN_MS)
                controller.resetToInactive()
            }
            SparkGamePhase.INACTIVE -> { /* nothing to do */ }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.phase) {
                if (state.phase == SparkGamePhase.ACTIVE) {
                    detectTapGestures { tapOffset ->
                        val xFraction = tapOffset.x / size.width.toFloat()
                        val yFraction = tapOffset.y / size.height.toFloat()
                        controller.tapSpark(xFraction, yFraction)
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            state.sparks.filter { it.alive }.forEach { spark ->
                val cx = spark.xFraction * size.width
                val cy = spark.yFraction * size.height
                drawSparkOrb(centre = Offset(cx, cy))
            }
            // Show countdown ring when < 3s remain
            if (state.phase == SparkGamePhase.ACTIVE && state.remainingMs < 3_000L) {
                val sweepFraction = (state.remainingMs / 3_000f).coerceIn(0f, 1f)
                drawArc(
                    color = HomeColors.heartVibrant.copy(alpha = 0.27f),
                    startAngle = -90f,
                    sweepAngle = 360f * sweepFraction,
                    useCenter = false,
                    topLeft = Offset(size.width / 2f - 30f, size.height / 2f - 30f),
                    size = androidx.compose.ui.geometry.Size(60f, 60f)
                )
            }
        }
    }
}

// ─── Spark orb draw helpers ───────────────────────────────────────────────────

// PERF: Pre-allocated color lists for radial gradient brushes. The lists are stable
// module-level constants so Brush.radialGradient() doesn't allocate a new List per draw call.
private val SPARK_HALO_COLORS = listOf(HomeColors.accentCyan.copy(alpha = 0.35f), Color.Transparent)
private val SPARK_CORE_COLORS = listOf(Color.White, HomeColors.accentCyan)

private fun DrawScope.drawSparkOrb(centre: Offset) {
    // Outer soft halo — uses pre-allocated color list to avoid per-call List allocation
    drawCircle(
        brush = Brush.radialGradient(
            colors = SPARK_HALO_COLORS,
            center = centre,
            radius = 40f,
            tileMode = TileMode.Clamp
        ),
        radius = 40f,
        center = centre
    )
    // Core glow — uses pre-allocated color list
    drawCircle(
        brush = Brush.radialGradient(
            colors = SPARK_CORE_COLORS,
            center = centre,
            radius = 14f,
            tileMode = TileMode.Clamp
        ),
        radius = 14f,
        center = centre
    )
    // Tiny cross sparkle lines
    val arm = 20f
    repeat(4) { i ->
        val angle = Math.toRadians((45 * i).toDouble())
        drawLine(
            color = HomeColors.accentCyan.copy(alpha = 0.67f),
            start = centre + Offset((-cos(angle) * arm).toFloat(), (-sin(angle) * arm).toFloat()),
            end = centre + Offset((cos(angle) * arm).toFloat(), (sin(angle) * arm).toFloat()),
            strokeWidth = 1.5f
        )
    }
}
