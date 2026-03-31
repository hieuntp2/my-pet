package com.aipet.brain.ui.avatar.pixel.runtime

import kotlin.random.Random

/**
 * Autonomous blink layer that operates independently of base idle/reaction layer state.
 *
 * Maintains its own blink phase, timing, and type selection. Outputs additive contributions
 * to the top-lid coverage and slit-closed flags consumed by FaceAnimationRuntime.renderFrame().
 *
 * Blink timing varies per sleepiness level. Type selection is weighted-random per frame.
 * Rare behaviour: asymmetric blinks inject micro-liveliness at low probability.
 */
internal class IndependentBlinkLayer {

    // -- Outputs (read by runtime each frame) --
    var leftContrib: Float = 0f
        private set
    var rightContrib: Float = 0f
        private set
    var leftClosed: Boolean = false
        private set
    var rightClosed: Boolean = false
        private set

    // -- Internal blink state --
    private var blinkPhase = InternalPhase.REST
    private var blinkProgress = 0f
    private var phaseTimer = 0L
    private var restTimer = 0L
    private var nextBlinkInterval = randomInterval(2200L, 4200L)
    private var currentType = BlinkType.NORMAL
    private var isSecondBlink = false

    // Duration tuning per blink type (ms)
    private var closeTimeMs = 0L
    private var holdTimeMs = 0L
    private var openTimeMs = 0L
    private var secondBlinkWaitMs = 0L

    /**
     * Force an immediate fast startled blink, interrupting any current blink.
     * Called by FaceAnimationRuntime when a STARTLED_SNAP reaction fires.
     */
    fun triggerStartledBlink() {
        if (blinkPhase == InternalPhase.REST || blinkPhase == InternalPhase.SECOND_WAIT) {
            currentType = BlinkType.STARTLED
            isSecondBlink = false
            blinkProgress = 0f
            phaseTimer = 0L
            blinkPhase = InternalPhase.CLOSING
            closeTimeMs = 55; holdTimeMs = 35; openTimeMs = 65
        }
    }

    fun advance(deltaMillis: Long, sleepiness: Int) {
        when (blinkPhase) {
            InternalPhase.REST -> {
                restTimer += deltaMillis
                if (restTimer >= nextBlinkInterval) {
                    restTimer = 0L
                    startBlink(sleepiness)
                }
            }
            InternalPhase.CLOSING -> {
                phaseTimer += deltaMillis
                blinkProgress = (phaseTimer.toFloat() / closeTimeMs.toFloat()).coerceIn(0f, 1f)
                if (phaseTimer >= closeTimeMs) {
                    blinkProgress = 1f
                    phaseTimer = 0L
                    blinkPhase = InternalPhase.HOLDING
                }
                applyOutputs()
            }
            InternalPhase.HOLDING -> {
                phaseTimer += deltaMillis
                blinkProgress = 1f
                if (phaseTimer >= holdTimeMs) {
                    phaseTimer = 0L
                    blinkPhase = InternalPhase.OPENING
                }
                applyOutputs()
            }
            InternalPhase.OPENING -> {
                phaseTimer += deltaMillis
                blinkProgress = (1f - phaseTimer.toFloat() / openTimeMs.toFloat()).coerceIn(0f, 1f)
                if (phaseTimer >= openTimeMs) {
                    blinkProgress = 0f
                    phaseTimer = 0L
                    if (currentType == BlinkType.DOUBLE && !isSecondBlink) {
                        isSecondBlink = true
                        blinkPhase = InternalPhase.SECOND_WAIT
                        secondBlinkWaitMs = randomInterval(70L, 130L)
                    } else {
                        blinkPhase = InternalPhase.REST
                        isSecondBlink = false
                        nextBlinkInterval = nextRestInterval(sleepiness)
                    }
                }
                applyOutputs()
            }
            InternalPhase.SECOND_WAIT -> {
                phaseTimer += deltaMillis
                blinkProgress = 0f
                if (phaseTimer >= secondBlinkWaitMs) {
                    phaseTimer = 0L
                    blinkPhase = InternalPhase.CLOSING
                }
                applyOutputs()
            }
        }
    }

    private fun startBlink(sleepiness: Int) {
        currentType = pickType(sleepiness)
        isSecondBlink = false
        blinkProgress = 0f
        phaseTimer = 0L
        blinkPhase = InternalPhase.CLOSING
        when (currentType) {
            BlinkType.NORMAL -> { closeTimeMs = 115; holdTimeMs = 55; openTimeMs = 85 }
            BlinkType.DOUBLE -> { closeTimeMs = 95; holdTimeMs = 45; openTimeMs = 70 }
            BlinkType.SLEEPY -> { closeTimeMs = 320; holdTimeMs = 200; openTimeMs = 240 }
            BlinkType.STARTLED -> { closeTimeMs = 55; holdTimeMs = 35; openTimeMs = 65 }
            BlinkType.ASYMMETRIC_LEFT_LEAD,
            BlinkType.ASYMMETRIC_RIGHT_LEAD -> { closeTimeMs = 120; holdTimeMs = 55; openTimeMs = 90 }
        }
    }

    private fun applyOutputs() {
        val p = blinkProgress.coerceIn(0f, 1f)
        when (currentType) {
            BlinkType.ASYMMETRIC_LEFT_LEAD -> {
                leftClosed = p >= 0.95f
                rightClosed = false
                leftContrib = if (leftClosed) 0f else p
                rightContrib = (p * 0.60f).coerceAtMost(0.85f)
            }
            BlinkType.ASYMMETRIC_RIGHT_LEAD -> {
                rightClosed = p >= 0.95f
                leftClosed = false
                rightContrib = if (rightClosed) 0f else p
                leftContrib = (p * 0.60f).coerceAtMost(0.85f)
            }
            else -> {
                leftClosed = p >= 0.95f
                rightClosed = p >= 0.95f
                leftContrib = if (leftClosed) 0f else p
                rightContrib = leftContrib
            }
        }
    }

    private fun pickType(sleepiness: Int): BlinkType {
        val r = Random.nextFloat()
        return when {
            sleepiness > 70 && r < 0.50f -> BlinkType.SLEEPY
            r < 0.04f -> BlinkType.ASYMMETRIC_LEFT_LEAD
            r < 0.09f -> BlinkType.ASYMMETRIC_RIGHT_LEAD
            r < 0.14f -> BlinkType.DOUBLE
            else -> BlinkType.NORMAL
        }
    }

    private fun nextRestInterval(sleepiness: Int): Long = when {
        sleepiness > 70 -> randomInterval(1500L, 2800L)
        sleepiness > 40 -> randomInterval(2000L, 3800L)
        else -> randomInterval(2400L, 4600L)
    }

    private enum class InternalPhase { REST, CLOSING, HOLDING, OPENING, SECOND_WAIT }

    private fun randomInterval(min: Long, max: Long): Long = min + Random.nextLong(max - min + 1)
}
