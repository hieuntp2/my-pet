package com.aipet.brain.app.ui.home

/**
 * Describes a single floating talk bubble displayed near the pet face on the Home stage.
 * A unique [displayId] is stamped at creation time so recomposition detects when the message
 * is replaced even if two different triggers share the same text.
 */
data class HomeTalkBubble(
    val message: String,
    val displayId: Long = System.currentTimeMillis()
)

/**
 * Types of scene-level particle FX drawn on the Home overlay layer.
 * Each type is ephemeral and governed by a timer inside [HomeFxOverlay].
 */
enum class HomeFxType {
    /** Small hearts rising from the face area — played on happy/cuddle reactions. */
    HEARTS,
    /** Radiating spark lines — played on excited greeting or play game win. */
    SPARKS,
    /** Floating "Z" glyphs — played when pet is sleepy. */
    ZZZ,
    /** Brief exclamation glyph — played on audio surprise. */
    EXCLAMATION
}

/**
 * Runtime phase of the embedded Catch-the-Spark mini-game.
 */
enum class SparkGamePhase {
    /** No game in progress. Can be started by [HomeMenuSheet], game invite event, or autonomous invitation. */
    INACTIVE,
    /** Pet autonomously initiated an invitation sequence; tap the pet to accept and start the game. */
    INVITE,
    /** Game is active; sparks are visible and user should tap them. */
    ACTIVE,
    /** All sparks caught; brief celebration before returning to INACTIVE. */
    WIN,
    /** Timer expired before all sparks caught; brief reaction before returning to INACTIVE. */
    LOSE,
    /** Cooldown period after a completed game before another can begin. */
    COOLDOWN
}

/**
 * A single tap-target spark shown during the mini-game.
 * Positions are expressed as fractions [0, 1] of the spark overlay container size.
 */
data class HomeSpark(
    val id: Int,
    /** Horizontal position as fraction of container width, centred around 0.5. */
    val xFraction: Float,
    /** Vertical position as fraction of container height, centred around 0.5. */
    val yFraction: Float,
    /** True while the spark should be rendered and can be tapped. */
    val alive: Boolean = true
)

/**
 * Complete snapshot of mini-game state read by the spark game overlay composable.
 */
data class SparkGameState(
    val phase: SparkGamePhase = SparkGamePhase.INACTIVE,
    val sparks: List<HomeSpark> = emptyList(),
    /** Remaining millis in the active phase, used to drive the countdown UI. */
    val remainingMs: Long = 0L,
    /** How many sparks the user tapped; used for result display. */
    val caughtCount: Int = 0
) {
    val isActive: Boolean get() = phase == SparkGamePhase.ACTIVE
    val allCaught: Boolean get() = sparks.isNotEmpty() && sparks.none { it.alive }
}
