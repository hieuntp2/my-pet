package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.AfterEffectType
import com.aipet.brain.brain.b2.domain.BehaviorAfterEffect
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.domain.EmotionMomentum
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.fusion.PerceptionFusionSnapshot
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetCondition
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages the [EmotionMomentum] lifecycle:
 * - Reaction affect updates from events
 * - Mood drift over time
 * - Carry-over from previous sessions
 * - Time-of-day modifiers
 */
class EmotionMomentumEngine(
    initial: EmotionMomentum = EmotionMomentum.DEFAULT
) {
    private val mutex = Mutex()

    @Volatile
    private var momentum: EmotionMomentum = initial

    fun current(): EmotionMomentum = momentum

    // ─── Reaction affect updates ─────────────────────────────────────────────

    /** Apply after-effects from a completed plan. */
    suspend fun applyPlanAfterEffects(plan: BehaviorPlan, nowMs: Long) {
        mutex.withLock {
            var m = momentum
            for (effect in plan.afterEffects) {
                m = applyEffect(m, effect)
            }
            momentum = m.copy(updatedAtMs = nowMs).clamped()
        }
    }

    /** Apply a touch interaction — updates joy, comfort, or irritation. */
    suspend fun applyTouchInteraction(
        interactionType: PetInteractionType,
        isSpammy: Boolean,
        nowMs: Long
    ) {
        mutex.withLock {
            momentum = when {
                isSpammy -> momentum.copy(
                    irritation = (momentum.irritation + 0.15f).coerceAtMost(1f),
                    joy = (momentum.joy - 0.05f).coerceAtLeast(0f)
                )
                interactionType == PetInteractionType.LONG_PRESS -> momentum.copy(
                    comfort = (momentum.comfort + 0.2f).coerceAtMost(1f),
                    joy = (momentum.joy + 0.1f).coerceAtMost(1f),
                    neediness = (momentum.neediness - 0.1f).coerceAtLeast(0f)
                )
                else -> momentum.copy(
                    joy = (momentum.joy + 0.1f).coerceAtMost(1f),
                    comfort = (momentum.comfort + 0.05f).coerceAtMost(1f)
                )
            }.copy(updatedAtMs = nowMs).clamped()
        }
    }

    /** Apply the effect of hearing a loud sound. */
    suspend fun applyLoudSound(nowMs: Long) {
        mutex.withLock {
            momentum = momentum.copy(
                startledLevel = (momentum.startledLevel + 0.7f).coerceAtMost(1f),
                caution = (momentum.caution + 0.2f).coerceAtMost(1f),
                updatedAtMs = nowMs
            ).clamped()
        }
    }

    /** Apply the effect of user voice interaction. */
    suspend fun applyVoiceInteraction(confidence: Float, nowMs: Long) {
        mutex.withLock {
            momentum = momentum.copy(
                curiosity = (momentum.curiosity + confidence * 0.2f).coerceAtMost(1f),
                joy = (momentum.joy + confidence * 0.05f).coerceAtMost(1f),
                updatedAtMs = nowMs
            ).clamped()
        }
    }

    /** Apply the effect of a game session completing. */
    suspend fun applyGameCompleted(won: Boolean, nowMs: Long) {
        mutex.withLock {
            momentum = momentum.copy(
                joy = (momentum.joy + if (won) 0.3f else 0.1f).coerceAtMost(1f),
                drowsiness = (momentum.drowsiness + 0.15f).coerceAtMost(1f),
                updatedAtMs = nowMs
            ).clamped()
        }
    }

    // ─── Session mood initialization ─────────────────────────────────────────

    /**
     * Seed session mood from PetConditions and persisted momentum at session start.
     * Applies time-of-day biases.
     */
    suspend fun initializeForSession(
        conditions: Set<PetCondition>,
        isNightTime: Boolean,
        isMorning: Boolean,
        isReturningAfterAbsence: Boolean,
        previousMomentum: EmotionMomentum?,
        nowMs: Long
    ) {
        mutex.withLock {
            var m = previousMomentum?.decayedForNewSession() ?: EmotionMomentum.DEFAULT

            // Conditions → mood seeds
            if (conditions.contains(PetCondition.SLEEPY)) {
                m = m.copy(moodDrowsy = (m.moodDrowsy + 0.4f).coerceAtMost(1f))
            }
            if (conditions.contains(PetCondition.LONELY)) {
                m = m.copy(moodNeedy = (m.moodNeedy + 0.3f).coerceAtMost(1f))
            }
            if (conditions.contains(PetCondition.PLAYFUL)) {
                m = m.copy(moodPlayful = (m.moodPlayful + 0.3f).coerceAtMost(1f))
            }
            if (isReturningAfterAbsence) {
                m = m.copy(moodNeedy = (m.moodNeedy + 0.2f).coerceAtMost(1f))
            }

            // Time-of-day
            if (isNightTime) {
                m = m.copy(moodDrowsy = (m.moodDrowsy + 0.3f).coerceAtMost(1f))
            } else if (isMorning) {
                m = m.copy(curiosity = (m.curiosity + 0.2f).coerceAtMost(1f))
            }

            momentum = m.copy(updatedAtMs = nowMs).clamped()
        }
    }

    // ─── Periodic drift ──────────────────────────────────────────────────────

    /**
     * Apply slow mood drift toward neutral. Call periodically (every 10–30s).
     * All mood fields decay toward 0 at a slow rate.
     */
    suspend fun applySlowDecay(nowMs: Long) {
        mutex.withLock {
            val decayRate = SLOW_DECAY_RATE
            momentum = momentum.copy(
                joy = drift(momentum.joy, 0f, decayRate),
                comfort = drift(momentum.comfort, 0f, decayRate),
                curiosity = drift(momentum.curiosity, 0f, decayRate),
                drowsiness = drift(momentum.drowsiness, 0f, decayRate * 0.5f),
                neediness = drift(momentum.neediness, 0f, decayRate),
                irritation = drift(momentum.irritation, 0f, decayRate * 1.5f),
                caution = drift(momentum.caution, 0f, decayRate),
                startledLevel = drift(momentum.startledLevel, 0f, FAST_DECAY_RATE),
                moodPlayful = drift(momentum.moodPlayful, 0f, decayRate * 0.3f),
                moodWithdrawn = drift(momentum.moodWithdrawn, 0f, decayRate * 0.5f),
                moodWarm = drift(momentum.moodWarm, 0f, decayRate * 0.2f),
                moodDrowsy = drift(momentum.moodDrowsy, 0f, decayRate * 0.3f),
                moodNeedy = drift(momentum.moodNeedy, 0f, decayRate * 0.4f),
                updatedAtMs = nowMs
            ).clamped()
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun applyEffect(m: EmotionMomentum, effect: BehaviorAfterEffect): EmotionMomentum =
        when (effect.type) {
            AfterEffectType.APPLY_JOY_DELTA ->
                m.copy(joy = (m.joy + effect.value).coerceIn(0f, 1f))
            AfterEffectType.APPLY_IRRITATION_DELTA ->
                m.copy(irritation = (m.irritation + effect.value).coerceIn(0f, 1f))
            AfterEffectType.APPLY_DROWSINESS_DELTA ->
                m.copy(drowsiness = (m.drowsiness + effect.value).coerceIn(0f, 1f))
            AfterEffectType.APPLY_NEEDINESS_DELTA ->
                m.copy(neediness = (m.neediness + effect.value).coerceIn(0f, 1f))
            AfterEffectType.APPLY_WARMTH_DELTA ->
                m.copy(moodWarm = (m.moodWarm + effect.value).coerceIn(0f, 1f))
            else -> m
        }

    /** Move [current] toward [target] by [rate]. */
    private fun drift(current: Float, target: Float, rate: Float): Float {
        if (current == target) return current
        return if (current > target) (current - rate).coerceAtLeast(target)
        else (current + rate).coerceAtMost(target)
    }

    companion object {
        private const val SLOW_DECAY_RATE = 0.008f
        private const val FAST_DECAY_RATE = 0.05f
    }
}

/** Prepare momentum for a new session by decaying reaction affect and retaining mood. */
private fun EmotionMomentum.decayedForNewSession(): EmotionMomentum = copy(
    joy = joy * 0.4f,
    comfort = comfort * 0.3f,
    curiosity = curiosity * 0.3f,
    startledLevel = 0f,
    caution = caution * 0.3f,
    irritation = irritation * 0.3f,
    moodPlayful = moodPlayful * 0.7f,
    moodWithdrawn = moodWithdrawn * 0.5f,
    moodWarm = moodWarm * 0.8f,
    moodDrowsy = moodDrowsy * 0.6f,
    moodNeedy = moodNeedy * 0.6f
)
