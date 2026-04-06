package com.aipet.brain.ui.avatar.pixel.runtime

import com.aipet.brain.ui.avatar.pixel.animation.FaceAnimationContext
import com.aipet.brain.ui.avatar.pixel.animation.FaceReactionType
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.renderer.PetEyeRenderer
import kotlin.math.roundToInt

/**
 * Core layered animation runtime for the pet face.
 *
 * Replaces the clip-based PixelAnimationOrchestrator pipeline with a continuously running
 * layer compositor. Each frame, the runtime:
 *   1. Advances the autonomous blink layer.
 *   2. Resolves idle family and state leakage from FaceAnimationContext.
 *   3. Advances the idle beat or active reaction phase.
 *   4. Lerps all floating-point layer values toward targets.
 *   5. Renders the current PixelFrame64 via PetEyeRenderer.
 *
 * Reaction triggering is thread-safe to call from a composable LaunchedEffect.
 * All heavy state mutation happens inside [update], which is called from withFrameNanos.
 *
 * State leakage from PetState vitals (energy, hunger, sleepiness, social) affects:
 *   - Idle family selection
 *   - Eyelid baseline (sleepiness adds droop continuously)
 *   - Eyebrow baseline (hunger/lonely adds concerned brow)
 *   - Transition speed multiplier (low energy / sleepiness → sluggish)
 */
class FaceAnimationRuntime {

    // -- External reaction-completion callback --
    var reactionCompleteListener: (() -> Unit)? = null

    // -- Current floating-point layer values --
    private var leftTopLid = 0f
    private var rightTopLid = 0f
    private var leftBottomLid = 0f
    private var rightBottomLid = 0f
    private var gazeX = 0f
    private var gazeAsymmetry = 0f
    private var leftBrowY = 0f
    private var rightBrowY = 0f
    private var browWarmth = 0f
    private var extraHighlight = false

    // -- Current targets (lerped toward) --
    private var targetLeftTopLid = 0f
    private var targetRightTopLid = 0f
    private var targetLeftBottomLid = 0f
    private var targetRightBottomLid = 0f
    private var targetGazeX = 0f
    private var targetGazeAsymmetry = 0f
    private var targetLeftBrowY = 0f
    private var targetRightBrowY = 0f
    private var targetBrowWarmth = 0f
    private var targetExtraHighlight = false

    // -- Active transition profile (set by each beat/reaction) --
    private var activeTransition = TransitionProfile.SMOOTH

    // -- Phase --
    private var phase = AnimationPhase.IDLE
    private var currentFamily = IdleBehaviorFamily.CALM

    // -- Sub-systems --
    private val blinkLayer = IndependentBlinkLayer()
    private val antiRepeatGuard = AntiRepeatGuard()
    private val idleDirector = IdleDirector(antiRepeatGuard)
    private val reactionOrchestrator = ReactionOrchestrator()
    private val faceResolver = FaceResolver()

    // -- Pending reaction (set externally, consumed on next update) --
    @Volatile private var pendingReaction: PendingReaction? = null

    init {
        reactionOrchestrator.onCompleted = {
            phase = AnimationPhase.IDLE
            reactionCompleteListener?.invoke()
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Advance the runtime by [deltaMillis] and return the rendered frame.
     * Call this from a withFrameNanos composable loop at ~60fps.
     */
    fun update(deltaMillis: Long, context: FaceAnimationContext): PixelFrame64 {
        // Consume any pending reaction trigger
        pendingReaction?.let { pending ->
            pendingReaction = null
            reactionOrchestrator.tryTrigger(pending.type, pending.priority)
            if (reactionOrchestrator.isActive) {
                phase = AnimationPhase.REACTION
                // Startled reactions co-fire a fast autonomous blink for extra visual snap
                if (pending.type == FaceReactionType.STARTLED_SNAP ||
                    pending.type == FaceReactionType.LOUD_SOUND_STARTLED) {
                    blinkLayer.triggerStartledBlink()
                }
            }
        }

        // 1. Advance autonomous blink
        blinkLayer.advance(deltaMillis, context.sleepiness)

        // 2. Resolve idle family and leakage
        val family = faceResolver.resolveIdleFamily(context)
        val leakage = faceResolver.resolveStateLeakage(context)

        if (family != currentFamily && phase == AnimationPhase.IDLE) {
            currentFamily = family
        }

        // 3. Advance main phase and compute effective transition profile
        when (phase) {
            AnimationPhase.IDLE -> {
                val beat = idleDirector.advance(deltaMillis, currentFamily)
                applyBeatTargets(beat, leakage)
            }
            AnimationPhase.REACTION -> {
                val directive = reactionOrchestrator.advance(deltaMillis)
                if (directive != null) {
                    applyReactionTargets(directive, leakage)
                } else {
                    // advance() returned null — sequence completed, onCompleted already fired.
                    // Return to idle on this same tick.
                    val beat = idleDirector.advance(deltaMillis, currentFamily)
                    applyBeatTargets(beat, leakage)
                }
            }
        }

        // 4. Lerp layer values toward targets, scaled by leakage speed modifier
        val dt = deltaMillis.toFloat()
        val s = leakage.speedMultiplier
        val p = activeTransition
        leftTopLid = step(leftTopLid, targetLeftTopLid, p.lidSpeed * s, dt)
        rightTopLid = step(rightTopLid, targetRightTopLid, p.lidSpeed * s, dt)
        leftBottomLid = step(leftBottomLid, targetLeftBottomLid, p.lidSpeed * s, dt)
        rightBottomLid = step(rightBottomLid, targetRightBottomLid, p.lidSpeed * s, dt)
        gazeX = step(gazeX, targetGazeX, p.gazeSpeed * s, dt)
        gazeAsymmetry = step(gazeAsymmetry, targetGazeAsymmetry, p.gazeSpeed * s, dt)
        leftBrowY = step(leftBrowY, targetLeftBrowY, p.browSpeed * s, dt)
        rightBrowY = step(rightBrowY, targetRightBrowY, p.browSpeed * s, dt)
        browWarmth = step(browWarmth, targetBrowWarmth, p.warmthSpeed * s, dt)
        if (extraHighlight != targetExtraHighlight) extraHighlight = targetExtraHighlight

        // 5. Render frame
        return renderFrame()
    }

    /**
     * Queue a reaction trigger. Thread-safe; consumed on the next [update] call.
     * Higher-priority reactions interrupt lower-priority ones.
     * STARTLED reactions also trigger an immediate fast blink on the next update tick.
     */
    fun triggerReaction(type: FaceReactionType, priority: Int) {
        val existing = pendingReaction
        if (existing == null || priority >= existing.priority) {
            pendingReaction = PendingReaction(type, priority)
        }
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    val debugSummary: String
        get() = "phase=$phase family=$currentFamily reacting=${reactionOrchestrator.isActive} " +
                "lids=[L:${leftTopLid.f2}|${leftBottomLid.f2} R:${rightTopLid.f2}|${rightBottomLid.f2}] " +
                "gaze=${gazeX.f2}±${gazeAsymmetry.f2} brow=[${leftBrowY.f2}/${rightBrowY.f2}] warm=${browWarmth.f2}"

    // ── Private ───────────────────────────────────────────────────────────────

    private fun applyBeatTargets(beat: IdleBeat, leakage: LayerLeakModifiers) {
        val t = beat.targets
        targetLeftTopLid = (t.leftTopLid + leakage.topLidAdd).coerceIn(0f, 1f)
        targetRightTopLid = (t.rightTopLid + leakage.topLidAdd).coerceIn(0f, 1f)
        targetLeftBottomLid = t.leftBottomLid
        targetRightBottomLid = t.rightBottomLid
        targetGazeX = t.gazeX
        targetGazeAsymmetry = t.gazeAsymmetry
        targetLeftBrowY = (t.leftBrowY + leakage.browYAdd).coerceIn(-1f, 1f)
        targetRightBrowY = (t.rightBrowY + leakage.browYAdd).coerceIn(-1f, 1f)
        targetBrowWarmth = maxOf(t.browWarmth, leakage.browWarmthFloor)
        targetExtraHighlight = t.extraHighlight
        activeTransition = beat.transitionProfile
    }

    private fun applyReactionTargets(directive: ReactionOrchestrator.ReactionDirective, leakage: LayerLeakModifiers) {
        val t = directive.targets
        // Reactions intentionally override leakage on brow (emotional expression takes priority)
        targetLeftTopLid = (t.leftTopLid + leakage.topLidAdd).coerceIn(0f, 1f)
        targetRightTopLid = (t.rightTopLid + leakage.topLidAdd).coerceIn(0f, 1f)
        targetLeftBottomLid = t.leftBottomLid
        targetRightBottomLid = t.rightBottomLid
        targetGazeX = t.gazeX
        targetGazeAsymmetry = t.gazeAsymmetry
        targetLeftBrowY = t.leftBrowY
        targetRightBrowY = t.rightBrowY
        targetBrowWarmth = maxOf(t.browWarmth, leakage.browWarmthFloor)
        targetExtraHighlight = t.extraHighlight
        activeTransition = directive.profile
    }

    private fun renderFrame(): PixelFrame64 {
        val blink = blinkLayer

        // Combine blink progress with base lid coverage (blink adds to the top lid)
        val leftTotalTop = (leftTopLid * 8f + blink.leftContrib * 8f).roundToInt().coerceIn(0, 8)
        val rightTotalTop = (rightTopLid * 8f + blink.rightContrib * 8f).roundToInt().coerceIn(0, 8)
        val leftTotalBottom = (leftBottomLid * 8f).roundToInt().coerceIn(0, 8)
        val rightTotalBottom = (rightBottomLid * 8f).roundToInt().coerceIn(0, 8)

        // Gaze: -1..1 → -2..2 pixel offset (coerced by PetEyeRenderer.MAX_PUPIL_OFFSET)
        val leftGaze = ((gazeX - gazeAsymmetry) * 2f).roundToInt()
        val rightGaze = ((gazeX + gazeAsymmetry) * 2f).roundToInt()

        // Brow: -1..1 → -3..3 row offset
        val leftBrow = (leftBrowY * 3f).roundToInt().coerceIn(-3, 3)
        val rightBrow = (rightBrowY * 3f).roundToInt().coerceIn(-3, 3)

        val browColor = if (browWarmth >= 0.5f) PetEyeRenderer.highlight else PetEyeRenderer.accent

        return PetEyeRenderer.buildFrame(
            leftPupilOffset = leftGaze,
            rightPupilOffset = rightGaze,
            leftTopLidRows = leftTotalTop,
            rightTopLidRows = rightTotalTop,
            leftBottomLidRows = leftTotalBottom,
            rightBottomLidRows = rightTotalBottom,
            leftClosedSlit = blink.leftClosed,
            rightClosedSlit = blink.rightClosed,
            eyebrowLeftYOffset = leftBrow,
            eyebrowRightYOffset = rightBrow,
            eyebrowLeftColor = browColor,
            eyebrowRightColor = browColor,
            extraHighlight = extraHighlight
        )
    }

    /** Linear step toward target without overshoot. */
    private fun step(current: Float, target: Float, speed: Float, dt: Float): Float {
        val maxStep = speed * dt
        return when {
            current < target -> (current + maxStep).coerceAtMost(target)
            current > target -> (current - maxStep).coerceAtLeast(target)
            else -> current
        }
    }

    private val Float.f2: String get() = "%.2f".format(this)

    private data class PendingReaction(val type: FaceReactionType, val priority: Int)
}
