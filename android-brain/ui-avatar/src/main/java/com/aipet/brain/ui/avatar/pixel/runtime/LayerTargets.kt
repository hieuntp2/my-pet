package com.aipet.brain.ui.avatar.pixel.runtime

/**
 * Normalized floating-point targets for each animation layer.
 *
 * Lid values: 0 = fully open, 1 = 8 rows of coverage (maximum).
 * Gaze: -1 = full left offset (-2 pixels), +1 = full right (+2 pixels).
 * BrowY: -1 = raised 3 rows, +1 = lowered 3 rows.
 * BrowWarmth: 0 = accent colour, 1 = highlight colour.
 */
internal data class LayerTargets(
    val leftTopLid: Float = 0f,
    val rightTopLid: Float = 0f,
    val leftBottomLid: Float = 0f,
    val rightBottomLid: Float = 0f,
    val gazeX: Float = 0f,
    val gazeAsymmetry: Float = 0f,
    val leftBrowY: Float = 0f,
    val rightBrowY: Float = 0f,
    val browWarmth: Float = 0f,
    val extraHighlight: Boolean = false
)

/**
 * Additive modifiers that state leakage applies continuously on top of beat targets.
 */
internal data class LayerLeakModifiers(
    /** Added to topLid targets (sleepiness -> drooped lids). */
    val topLidAdd: Float = 0f,
    /** Added to brow Y targets (hunger/lonely -> concerned brow). */
    val browYAdd: Float = 0f,
    /** Minimum warmth floor from long-term bond and social comfort. */
    val browWarmthFloor: Float = 0f,
    /** Multiplies all transition speeds (low energy / sleepiness -> sluggish motion). */
    val speedMultiplier: Float = 1f
)
