package com.aipet.brain.brain.attention

import com.aipet.brain.brain.attention.AttentionTargetEvaluator.FocusCandidate
import com.aipet.brain.brain.b2.domain.RelationshipState
import com.aipet.brain.brain.b2.domain.WorkingContext

/**
 * Computes effective salience for each candidate by adding bonuses and subtracting costs.
 *
 * Effective salience formula:
 *   raw_salience
 * + relationship_bonus
 * + novelty_bonus
 * + current_need_bonus
 * - switch_cost
 * - fatigue_penalty
 * - anti_repeat_penalty
 */
class SalienceScorer {

    data class ScoredCandidate(
        val candidate: FocusCandidate,
        val switchCostPenalty: Float,
        val effectiveSalience: Float
    )

    fun score(
        candidates: List<FocusCandidate>,
        currentTarget: FocusTarget,
        ctx: WorkingContext
    ): List<ScoredCandidate> {
        return candidates.map { candidate ->
            val rel = ctx.relationship
            val relationshipBonus = relationshipBonus(candidate.target.type, rel)
            val noveltyBonus = noveltyBonus(candidate.target.type, ctx)
            val needBonus = needBonus(candidate.target.type, ctx)
            val switchCost = switchCost(candidate.target, currentTarget, ctx)
            val fatiguePenalty = fatiguePenalty(candidate.target.type, ctx)

            val effective = (
                candidate.rawSalience + relationshipBonus + noveltyBonus + needBonus
                    - switchCost - fatiguePenalty
            ).coerceAtLeast(0f)

            ScoredCandidate(
                candidate = candidate,
                switchCostPenalty = switchCost,
                effectiveSalience = effective
            )
        }
    }

    // ─── Scoring components ───────────────────────────────────────────────────

    private fun relationshipBonus(type: FocusTargetType, rel: RelationshipState): Float = when (type) {
        FocusTargetType.USER_FACE -> (rel.familiarity * 0.2f) + (rel.recentWarmth * 0.1f)
        FocusTargetType.USER_VOICE -> rel.trust * 0.1f
        FocusTargetType.TOUCH_SOURCE -> rel.familiarity * 0.1f
        else -> 0f
    }

    private fun noveltyBonus(type: FocusTargetType, ctx: WorkingContext): Float = when (type) {
        FocusTargetType.SOUND_SOURCE ->
            ctx.perception.attentionContext.noveltySignal * 0.3f
        FocusTargetType.USER_FACE ->
            if (ctx.perception.presence.entryEventRecently) 0.2f else 0f
        else -> 0f
    }

    private fun needBonus(type: FocusTargetType, ctx: WorkingContext): Float {
        val momentum = ctx.emotionMomentum
        return when (type) {
            FocusTargetType.INTERNAL_NEED -> momentum.neediness * 0.2f + momentum.drowsiness * 0.1f
            FocusTargetType.USER_FACE -> if (momentum.moodNeedy > 0.4f) 0.15f else 0f
            else -> 0f
        }
    }

    private fun switchCost(
        candidate: FocusTarget,
        currentTarget: FocusTarget,
        ctx: WorkingContext
    ): Float {
        if (currentTarget.type == FocusTargetType.NONE) return 0f
        if (candidate.type == currentTarget.type) return 0f  // same type — no switch cost

        // Base switch cost from attention mode
        val baseCost = when (ctx.attention.mode) {
            AttentionMode.SOCIAL_LOCK -> 0.4f
            AttentionMode.PLAY_FOCUS -> 0.35f
            AttentionMode.DOZING -> 0.3f
            AttentionMode.WITHDRAWN -> 0.25f
            AttentionMode.IDLE_SCANNING -> 0.02f
            else -> 0.1f
        }

        // Stickiness multiplier
        val stickiness = ctx.attention.stickiness
        return baseCost * (1f + stickiness)
    }

    private fun fatiguePenalty(type: FocusTargetType, ctx: WorkingContext): Float {
        val fatigue = ctx.attention.fatigue
        if (fatigue < 0.3f) return 0f
        return when (type) {
            FocusTargetType.SOUND_SOURCE -> fatigue * 0.3f
            FocusTargetType.INTERNAL_NEED -> fatigue * 0.1f
            else -> fatigue * 0.1f
        }
    }
}
