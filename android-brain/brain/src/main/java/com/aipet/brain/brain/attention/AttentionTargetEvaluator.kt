package com.aipet.brain.brain.attention

import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.fusion.PerceptionFusionSnapshot
import com.aipet.brain.brain.pet.PetCondition

/**
 * Generates candidate [FocusTarget]s from the current perception fusion snapshot
 * and internal need state, with raw salience scores.
 */
class AttentionTargetEvaluator {

    data class FocusCandidate(
        val target: FocusTarget,
        val rawSalience: Float
    )

    fun evaluate(ctx: WorkingContext): List<FocusCandidate> {
        val p = ctx.perception
        val nowMs = ctx.snapshotAtMs
        val candidates = mutableListOf<FocusCandidate>()

        // USER_FACE: highest social priority when user is present
        if (p.presence.userPresent) {
            val recencyBonus = if (p.presence.entryEventRecently) 0.3f else 0f
            val familiarBonus = if (p.presence.familiarUserPresent) 0.2f else 0f
            val stableBonus = (p.presence.stablePresenceMs / 10_000f).coerceAtMost(0.2f)
            val salience = 0.6f + recencyBonus + familiarBonus + stableBonus
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.USER_FACE,
                    id = p.presence.recognizedPersonId,
                    confidence = if (p.presence.familiarUserPresent) 0.9f else 0.7f,
                    salience = salience,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = salience
            )
        }

        // USER_VOICE: high when voice is active and addressed to pet
        if (p.voiceContext.voiceActivity) {
            val addressBonus = if (p.voiceContext.commandAddressedToPet) 0.3f else 0f
            val confBonus = p.voiceContext.commandConfidence * 0.2f
            val salience = 0.55f + addressBonus + confBonus
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.USER_VOICE,
                    confidence = p.voiceContext.commandConfidence.coerceAtLeast(0.5f),
                    salience = salience,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = salience
            )
        }

        // TOUCH_SOURCE: immediate touch reaction
        if (p.touchContext.recentTap || p.touchContext.recentLongPress) {
            val affectionBonus = p.touchContext.affectionLikelihood * 0.2f
            val salience = 0.75f + affectionBonus
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.TOUCH_SOURCE,
                    confidence = 1.0f,
                    salience = salience,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = salience
            )
        }

        // SOUND_SOURCE: when audio orienting is warranted
        if (p.audioContext.shouldOrientToSound) {
            val loudBonus = if (p.audioContext.loudEventActive) 0.3f else 0f
            val extBonus = p.audioContext.externalSoundConfidence * 0.2f
            val salience = 0.4f + loudBonus + extBonus
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.SOUND_SOURCE,
                    confidence = p.audioContext.externalSoundConfidence,
                    salience = salience,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = salience
            )
        }

        // INTERNAL_NEED: when pet has strong need and nothing external is pulling
        val internalNeedPressure = computeInternalNeedPressure(ctx)
        if (internalNeedPressure > 0.3f) {
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.INTERNAL_NEED,
                    confidence = 1.0f,
                    salience = internalNeedPressure,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = internalNeedPressure
            )
        }

        // GAME_TARGET: when play is active
        if (ctx.currentBehavior?.intention == PetIntention.PLAY) {
            candidates += FocusCandidate(
                target = FocusTarget(
                    type = FocusTargetType.GAME_TARGET,
                    confidence = 1.0f,
                    salience = 0.8f,
                    acquiredAtMs = nowMs,
                    lastReinforcedAtMs = nowMs
                ),
                rawSalience = 0.8f
            )
        }

        return candidates
    }

    private fun computeInternalNeedPressure(ctx: WorkingContext): Float {
        val conditions = ctx.conditions
        val state = ctx.petState
        var pressure = 0f
        if (conditions.contains(PetCondition.HUNGRY)) pressure += state.hunger / 100f * 0.5f
        if (conditions.contains(PetCondition.LONELY)) pressure += (100 - state.social) / 100f * 0.4f
        if (conditions.contains(PetCondition.SLEEPY)) pressure += state.sleepiness / 100f * 0.3f
        pressure += ctx.emotionMomentum.neediness * 0.3f
        return pressure.coerceIn(0f, 1f)
    }
}
