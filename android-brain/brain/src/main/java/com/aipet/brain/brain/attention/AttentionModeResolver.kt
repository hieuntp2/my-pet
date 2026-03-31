package com.aipet.brain.brain.attention

import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.pet.PetCondition

/**
 * Resolves the appropriate [AttentionMode] from context.
 * Mode is separate from the focus target — it describes *how* the pet is attending.
 */
class AttentionModeResolver {

    fun resolve(ctx: WorkingContext, currentMode: AttentionMode): AttentionMode {
        val p = ctx.perception
        val conditions = ctx.conditions
        val momentum = ctx.emotionMomentum

        return when {
            // High startle: ALERT
            p.audioContext.loudEventActive -> AttentionMode.ALERT

            // Direct user engagement: SOCIAL_LOCK
            (p.touchContext.recentLongPress || p.touchContext.recentTap) &&
                !p.touchContext.spamLikelihood.isSpammy() &&
                p.presence.userPresent -> AttentionMode.SOCIAL_LOCK

            // Voice active: LISTENING (unless socially locked already)
            p.voiceContext.voiceActivity -> if (currentMode == AttentionMode.SOCIAL_LOCK)
                AttentionMode.SOCIAL_LOCK
            else
                AttentionMode.LISTENING

            // Play is active
            ctx.currentBehavior?.intention == com.aipet.brain.brain.b2.domain.PetIntention.PLAY ->
                AttentionMode.PLAY_FOCUS

            // Overstimulated or irritated: WITHDRAWN
            momentum.irritation > 0.6f || p.touchContext.spamLikelihood.isSpammy() ->
                AttentionMode.WITHDRAWN

            // Sleepy: DOZING
            conditions.contains(PetCondition.SLEEPY) || momentum.drowsiness > 0.6f ->
                AttentionMode.DOZING

            // User present but not interacting: PASSIVE_COMPANION
            p.presence.userPresent && p.presence.stablePresenceMs > 2_000L ->
                AttentionMode.PASSIVE_COMPANION

            // Novel event: CURIOUS_INSPECTION
            p.attentionContext.noveltySignal > 0.4f || p.presence.entryEventRecently ->
                AttentionMode.CURIOUS_INSPECTION

            // Default: IDLE_SCANNING
            else -> AttentionMode.IDLE_SCANNING
        }
    }

    private fun Float.isSpammy(): Boolean = this > 0.6f
}
