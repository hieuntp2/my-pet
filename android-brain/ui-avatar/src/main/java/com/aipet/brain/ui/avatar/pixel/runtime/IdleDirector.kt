package com.aipet.brain.ui.avatar.pixel.runtime

import kotlin.random.Random

/**
 * Manages the idle animation loop across five behavior families:
 * CALM, CURIOUS, SLEEPY, EXPECTANT, PLAYFUL.
 *
 * Each family has a set of named beats. IdleDirector picks beats sequentially using
 * AntiRepeatGuard + weighted randomness for holdDurationRange timing variation.
 * Family-specific transition profiles and targets produce visually distinct feels.
 */
internal class IdleDirector(private val antiRepeatGuard: AntiRepeatGuard) {

    private var currentBeat: IdleBeat? = null
    private var holdTimer = 0L
    private var currentHoldDuration = 0L

    /**
     * Advances the idle loop and returns the current active beat.
     * Automatically selects a new beat when the hold duration expires.
     */
    fun advance(deltaMillis: Long, family: IdleBehaviorFamily): IdleBeat {
        antiRepeatGuard.tick(deltaMillis)
        holdTimer += deltaMillis

        val beat = currentBeat
        if (beat == null || holdTimer >= currentHoldDuration) {
            val next = pickNextBeat(family)
            currentBeat = next
            currentHoldDuration = Random.nextLong(next.holdDurationRange.first, next.holdDurationRange.last + 1)
            holdTimer = 0L
            antiRepeatGuard.record(next.id, next.cooldownMs)
        }
        return currentBeat!!
    }

    private fun pickNextBeat(family: IdleBehaviorFamily): IdleBeat {
        val pool = beatsByFamily[family] ?: beatsByFamily[IdleBehaviorFamily.CALM]!!
        val candidates = antiRepeatGuard.filter(pool)
        return weightedRandom(candidates)
    }

    private fun weightedRandom(beats: List<IdleBeat>): IdleBeat {
        if (beats.size == 1) return beats.first()
        val total = beats.sumOf { it.weight.toDouble() }.toFloat()
        var roll = Random.nextFloat() * total
        for (beat in beats) {
            roll -= beat.weight
            if (roll <= 0f) return beat
        }
        return beats.last()
    }

    companion object {
        private val beatsByFamily: Map<IdleBehaviorFamily, List<IdleBeat>> = buildBeatLibrary()

        @Suppress("LongMethod")
        private fun buildBeatLibrary(): Map<IdleBehaviorFamily, List<IdleBeat>> = mapOf(

            IdleBehaviorFamily.CALM to listOf(
                beat("calm_center", IdleBehaviorFamily.CALM, 2.5f, 1200L..2100L, LayerTargets(gazeX = 0f)),
                beat("calm_soft_squint", IdleBehaviorFamily.CALM, 1.5f, 600L..1000L,
                    LayerTargets(leftTopLid = 0.18f, rightTopLid = 0.18f, leftBottomLid = 0.10f, rightBottomLid = 0.10f)),
                beat("calm_glance_left", IdleBehaviorFamily.CALM, 1.2f, 380L..640L,
                    LayerTargets(gazeX = -0.75f, leftTopLid = 0.12f, rightTopLid = 0.12f), TransitionProfile.SMOOTH),
                beat("calm_glance_right", IdleBehaviorFamily.CALM, 1.2f, 380L..640L,
                    LayerTargets(gazeX = 0.75f, leftTopLid = 0.12f, rightTopLid = 0.12f), TransitionProfile.SMOOTH),
                beat("calm_settle", IdleBehaviorFamily.CALM, 1.0f, 300L..500L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.10f, rightTopLid = 0.10f)),
                beat("calm_side_glance_drift", IdleBehaviorFamily.CALM, 0.4f, 500L..700L,
                    LayerTargets(gazeX = -0.90f, gazeAsymmetry = 0.10f, leftTopLid = 0.08f),
                    TransitionProfile.SLEEPY_DRIFT, isRare = true, cooldownMs = 5000L),
                beat("calm_half_squint", IdleBehaviorFamily.CALM, 0.8f, 400L..700L,
                    LayerTargets(leftTopLid = 0.22f, rightTopLid = 0.22f, leftBrowY = 0.05f, rightBrowY = 0.05f))
            ),

            IdleBehaviorFamily.CURIOUS to listOf(
                beat("curious_center", IdleBehaviorFamily.CURIOUS, 1.5f, 500L..900L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.15f, rightTopLid = 0.15f, leftBrowY = -0.30f, rightBrowY = -0.30f)),
                beat("curious_scan_left", IdleBehaviorFamily.CURIOUS, 2.0f, 350L..580L,
                    LayerTargets(gazeX = -0.85f, leftTopLid = 0.12f, rightTopLid = 0.20f, leftBrowY = -0.30f, rightBrowY = -0.10f), TransitionProfile.SNAP),
                beat("curious_hold_left", IdleBehaviorFamily.CURIOUS, 1.5f, 280L..460L,
                    LayerTargets(gazeX = -0.80f, leftTopLid = 0.18f, rightTopLid = 0.18f, leftBrowY = -0.25f, rightBrowY = -0.10f)),
                beat("curious_scan_right", IdleBehaviorFamily.CURIOUS, 2.0f, 350L..580L,
                    LayerTargets(gazeX = 0.85f, leftTopLid = 0.20f, rightTopLid = 0.12f, leftBrowY = -0.10f, rightBrowY = -0.30f), TransitionProfile.SNAP),
                beat("curious_hold_right", IdleBehaviorFamily.CURIOUS, 1.5f, 280L..460L,
                    LayerTargets(gazeX = 0.80f, leftTopLid = 0.18f, rightTopLid = 0.18f, leftBrowY = -0.10f, rightBrowY = -0.25f)),
                beat("curious_focus_center", IdleBehaviorFamily.CURIOUS, 1.2f, 500L..800L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.25f, rightTopLid = 0.25f, leftBrowY = -0.25f, rightBrowY = -0.25f)),
                beat("curious_widen", IdleBehaviorFamily.CURIOUS, 0.5f, 200L..360L,
                    LayerTargets(gazeX = 0f, leftBrowY = -0.50f, rightBrowY = -0.50f, extraHighlight = true),
                    TransitionProfile.SNAP, isRare = true, cooldownMs = 5000L),
                beat("curious_micro_tilt", IdleBehaviorFamily.CURIOUS, 0.3f, 180L..280L,
                    LayerTargets(gazeX = 0.20f, gazeAsymmetry = 0.18f, leftTopLid = 0.10f, leftBrowY = -0.35f, rightBrowY = -0.20f),
                    isRare = true, cooldownMs = 6000L)
            ),

            IdleBehaviorFamily.SLEEPY to listOf(
                beat("sleepy_half_lid", IdleBehaviorFamily.SLEEPY, 3.0f, 1600L..2600L,
                    LayerTargets(leftTopLid = 0.50f, rightTopLid = 0.50f, leftBottomLid = 0.25f, rightBottomLid = 0.25f, leftBrowY = 0.40f, rightBrowY = 0.40f), TransitionProfile.SLEEPY_DRIFT),
                beat("sleepy_heavy_lid", IdleBehaviorFamily.SLEEPY, 2.0f, 1000L..1600L,
                    LayerTargets(leftTopLid = 0.65f, rightTopLid = 0.65f, leftBottomLid = 0.32f, rightBottomLid = 0.32f, leftBrowY = 0.60f, rightBrowY = 0.60f), TransitionProfile.SLEEPY_DRIFT),
                beat("sleepy_drift_left", IdleBehaviorFamily.SLEEPY, 1.2f, 900L..1300L,
                    LayerTargets(gazeX = -0.30f, gazeAsymmetry = 0.05f, leftTopLid = 0.60f, rightTopLid = 0.50f, leftBottomLid = 0.25f, rightBottomLid = 0.20f, leftBrowY = 0.50f, rightBrowY = 0.40f), TransitionProfile.SLEEPY_DRIFT),
                beat("sleepy_drift_right", IdleBehaviorFamily.SLEEPY, 1.2f, 900L..1300L,
                    LayerTargets(gazeX = 0.30f, gazeAsymmetry = -0.05f, leftTopLid = 0.50f, rightTopLid = 0.60f, leftBottomLid = 0.20f, rightBottomLid = 0.25f, leftBrowY = 0.40f, rightBrowY = 0.50f), TransitionProfile.SLEEPY_DRIFT),
                beat("sleepy_reopen", IdleBehaviorFamily.SLEEPY, 1.0f, 600L..900L,
                    LayerTargets(leftTopLid = 0.35f, rightTopLid = 0.35f, leftBottomLid = 0.12f, rightBottomLid = 0.12f, leftBrowY = 0.28f, rightBrowY = 0.28f), TransitionProfile.SLEEPY_DRIFT),
                beat("sleepy_near_doze", IdleBehaviorFamily.SLEEPY, 0.6f, 1200L..1800L,
                    LayerTargets(leftTopLid = 0.82f, rightTopLid = 0.82f, leftBottomLid = 0.42f, rightBottomLid = 0.42f, leftBrowY = 0.70f, rightBrowY = 0.70f), TransitionProfile.SLEEPY_DRIFT, isRare = true, cooldownMs = 6000L),
                beat("sleepy_stagger_asymm", IdleBehaviorFamily.SLEEPY, 0.3f, 600L..900L,
                    LayerTargets(gazeX = -0.10f, leftTopLid = 0.65f, rightTopLid = 0.55f, leftBottomLid = 0.30f, rightBottomLid = 0.20f, leftBrowY = 0.60f, rightBrowY = 0.50f), TransitionProfile.SLEEPY_DRIFT, isRare = true, cooldownMs = 7000L)
            ),

            IdleBehaviorFamily.EXPECTANT to listOf(
                beat("expectant_center", IdleBehaviorFamily.EXPECTANT, 2.5f, 1200L..1800L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.12f, rightTopLid = 0.12f, leftBrowY = 0.30f, rightBrowY = 0.30f)),
                beat("expectant_glance_right", IdleBehaviorFamily.EXPECTANT, 1.5f, 380L..620L,
                    LayerTargets(gazeX = 0.55f, leftTopLid = 0.12f, rightTopLid = 0.18f, leftBrowY = 0.28f, rightBrowY = 0.16f)),
                beat("expectant_glance_left", IdleBehaviorFamily.EXPECTANT, 1.5f, 380L..620L,
                    LayerTargets(gazeX = -0.55f, leftTopLid = 0.18f, rightTopLid = 0.12f, leftBrowY = 0.16f, rightBrowY = 0.28f)),
                beat("expectant_return", IdleBehaviorFamily.EXPECTANT, 1.8f, 280L..480L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.12f, rightTopLid = 0.12f, leftBrowY = 0.30f, rightBrowY = 0.30f)),
                beat("expectant_scan_wide_right", IdleBehaviorFamily.EXPECTANT, 0.8f, 320L..520L,
                    LayerTargets(gazeX = 0.80f, leftTopLid = 0.10f, rightTopLid = 0.15f, leftBrowY = 0.25f, rightBrowY = 0.15f), TransitionProfile.SMOOTH),
                beat("expectant_scan_wide_left", IdleBehaviorFamily.EXPECTANT, 0.8f, 320L..520L,
                    LayerTargets(gazeX = -0.80f, leftTopLid = 0.15f, rightTopLid = 0.10f, leftBrowY = 0.15f, rightBrowY = 0.25f), TransitionProfile.SMOOTH),
                beat("expectant_hold_long", IdleBehaviorFamily.EXPECTANT, 0.8f, 800L..1400L,
                    LayerTargets(gazeX = 0f, leftTopLid = 0.10f, rightTopLid = 0.10f, leftBrowY = 0.32f, rightBrowY = 0.32f)),
                beat("expectant_tentative_dip", IdleBehaviorFamily.EXPECTANT, 0.4f, 360L..500L,
                    LayerTargets(leftTopLid = 0.28f, rightTopLid = 0.28f, leftBottomLid = 0.12f, rightBottomLid = 0.12f, leftBrowY = 0.60f, rightBrowY = 0.60f),
                    isRare = true, cooldownMs = 5000L)
            ),

            IdleBehaviorFamily.PLAYFUL to listOf(
                beat("playful_open", IdleBehaviorFamily.PLAYFUL, 1.5f, 300L..500L,
                    LayerTargets(gazeX = 0f, leftBrowY = -0.24f, rightBrowY = -0.24f, browWarmth = 0.80f, extraHighlight = true)),
                beat("playful_squint", IdleBehaviorFamily.PLAYFUL, 2.0f, 500L..750L,
                    LayerTargets(leftTopLid = 0.25f, rightTopLid = 0.25f, leftBottomLid = 0.20f, rightBottomLid = 0.20f, leftBrowY = 0.15f, rightBrowY = 0.15f, browWarmth = 1.0f)),
                beat("playful_scan_right", IdleBehaviorFamily.PLAYFUL, 1.8f, 260L..440L,
                    LayerTargets(gazeX = 0.80f, leftBrowY = -0.20f, rightBrowY = -0.20f, browWarmth = 0.70f), TransitionProfile.SNAP),
                beat("playful_scan_left", IdleBehaviorFamily.PLAYFUL, 1.8f, 260L..440L,
                    LayerTargets(gazeX = -0.80f, leftBrowY = -0.20f, rightBrowY = -0.20f, browWarmth = 0.70f), TransitionProfile.SNAP),
                beat("playful_bounce", IdleBehaviorFamily.PLAYFUL, 1.0f, 220L..340L,
                    LayerTargets(gazeX = 0.20f, gazeAsymmetry = 0.15f, leftBrowY = -0.28f, rightBrowY = -0.28f, browWarmth = 0.90f, extraHighlight = true), TransitionProfile.PLAYFUL_POP),
                beat("playful_asymm_squint", IdleBehaviorFamily.PLAYFUL, 0.4f, 280L..400L,
                    LayerTargets(gazeX = -0.35f, leftTopLid = 0.30f, rightTopLid = 0.12f, browWarmth = 0.80f), isRare = true, cooldownMs = 5500L),
                beat("playful_wink_prep", IdleBehaviorFamily.PLAYFUL, 0.35f, 200L..320L,
                    LayerTargets(gazeX = 0.30f, leftTopLid = 0.10f, rightTopLid = 0.10f, browWarmth = 0.90f, extraHighlight = true), TransitionProfile.PLAYFUL_POP, isRare = true, cooldownMs = 6500L)
            )
        )

        private fun beat(
            id: String,
            family: IdleBehaviorFamily,
            weight: Float,
            holdRange: LongRange,
            targets: LayerTargets,
            transition: TransitionProfile = TransitionProfile.SMOOTH,
            isRare: Boolean = false,
            cooldownMs: Long = 2800L
        ): IdleBeat = IdleBeat(
            id = id,
            family = family,
            weight = if (isRare) weight * 0.30f else weight,
            holdDurationRange = holdRange,
            targets = targets,
            transitionProfile = transition,
            cooldownMs = cooldownMs,
            isRare = isRare
        )
    }
}
