package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import kotlin.random.Random

/**
 * Evolution-aware behavior scorer.
 *
 * Combines all long-term signals (memory, bond, habit, personality, lifecycle)
 * into an explainable score for each behavior category.
 *
 * This extends the existing IntentionScorer by adding the new context inputs.
 * It does NOT replace the behavior engine — it enriches the context fed into it.
 */
class EvolutionBehaviorScorer(
    private val randomSeed: () -> Float = { Random.nextFloat() * 0.1f }
) {

    enum class BehaviorCategory {
        GREETING, IDLE, REACTION, INVITATION, COMFORT_SEEKING, RECOVERY, AFFECTIONATE_RARE
    }

    data class ScoredBehavior(
        val category: BehaviorCategory,
        val components: BehaviorScoreComponents
    )

    data class ScoringContext(
        val petState: PetState,
        val conditions: Set<PetCondition>,
        val traits: PetTrait?,
        val bond: BondStateV2,
        val recentEpisodes: List<MemoryEpisode>,
        val habitProfile: UserHabitProfile,
        val dayPhase: DayPhase,
        val reunionType: ReunionType,
        val expectationState: ExpectedReturnWindowResolver.ExpectationState,
        val invitationIgnoredCount: Int,
        val lastInvitationMs: Long,
        val nowMs: Long
    )

    fun score(context: ScoringContext, category: BehaviorCategory): BehaviorScoreComponents {
        val stateWeight = computeStateWeight(context, category)
        val relationshipWeight = computeRelationshipWeight(context, category)
        val memoryWeight = computeMemoryWeight(context, category)
        val habitWeight = computeHabitWeight(context, category)
        val personalityWeight = computePersonalityWeight(context, category)
        val lifecycleWeight = computeLifecycleWeight(context, category)
        val suppressionPenalty = computeSuppressionPenalty(context, category)
        val cooldownPenalty = computeCooldownPenalty(context, category)
        val noise = randomSeed() // bounded ±0.1

        return BehaviorScoreComponents(
            stateWeight = stateWeight,
            relationshipWeight = relationshipWeight,
            memoryWeight = memoryWeight,
            habitWeight = habitWeight,
            personalityWeight = personalityWeight,
            lifecycleWeight = lifecycleWeight,
            variationNoise = noise,
            suppressionPenalty = suppressionPenalty,
            cooldownPenalty = cooldownPenalty
        )
    }

    private fun computeStateWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val state = ctx.petState
        return when (cat) {
            BehaviorCategory.GREETING -> {
                val warmthBase = state.bond / 100f * 0.4f
                val trustBoost = state.trustScore / 100f * 0.3f
                warmthBase + trustBoost
            }
            BehaviorCategory.INVITATION -> {
                val energyFactor = state.energy / 100f * 0.5f
                val socialFactor = (100 - state.social) / 100f * 0.3f
                energyFactor + socialFactor
            }
            BehaviorCategory.COMFORT_SEEKING -> {
                val comfortNeed = (100 - state.comfort) / 100f * 0.5f
                val neglectFactor = minOf(ctx.petState.neglectStreak, 5) / 5f * 0.3f
                comfortNeed + neglectFactor
            }
            BehaviorCategory.RECOVERY -> {
                if (ctx.reunionType == ReunionType.RECOVERY_RETURN) 0.6f
                else if (ctx.petState.neglectStreak >= 2) 0.4f
                else 0f
            }
            BehaviorCategory.AFFECTIONATE_RARE -> {
                if (state.bond >= 70 && state.trustScore >= 60) 0.5f else 0.1f
            }
            else -> 0.3f
        }
    }

    private fun computeRelationshipWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val bond = ctx.bond
        return when (cat) {
            BehaviorCategory.GREETING -> bond.affection * 0.4f + bond.trust * 0.3f
            BehaviorCategory.INVITATION -> bond.dependency * 0.4f + bond.affection * 0.2f
            BehaviorCategory.COMFORT_SEEKING -> (1f - bond.stability) * 0.3f
            BehaviorCategory.RECOVERY -> (1f - bond.trust) * 0.4f
            BehaviorCategory.AFFECTIONATE_RARE -> bond.affection * 0.3f * bond.trust
            else -> bond.affection * 0.2f
        }
    }

    private fun computeMemoryWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val importantMemories = ctx.recentEpisodes.filter { it.importanceScore >= 0.5f }
        val recentNeglect = ctx.recentEpisodes.any { it.neglectSignal }
        val recentGoodCare = ctx.recentEpisodes.any { it.careScoreDelta > 8 }

        return when (cat) {
            BehaviorCategory.GREETING -> {
                if (importantMemories.isNotEmpty() && recentGoodCare) 0.3f
                else if (recentNeglect) -0.1f
                else 0.1f
            }
            BehaviorCategory.RECOVERY -> if (recentNeglect) 0.4f else 0f
            BehaviorCategory.AFFECTIONATE_RARE -> if (recentGoodCare && importantMemories.size >= 2) 0.3f else 0f
            else -> if (recentGoodCare) 0.1f else 0f
        }
    }

    private fun computeHabitWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val expectation = ctx.expectationState
        return when (cat) {
            BehaviorCategory.GREETING -> when (expectation) {
                ExpectedReturnWindowResolver.ExpectationState.ON_TIME -> 0.25f
                ExpectedReturnWindowResolver.ExpectationState.SLIGHTLY_LATE -> 0.15f
                ExpectedReturnWindowResolver.ExpectationState.MISSED_WINDOW -> 0.05f
                else -> 0.1f
            }
            BehaviorCategory.INVITATION -> {
                val consistency = ctx.habitProfile.recentConsistencyScore
                consistency * 0.2f
            }
            else -> 0f
        }
    }

    private fun computePersonalityWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val traits = ctx.traits ?: return 0f
        return when (cat) {
            BehaviorCategory.INVITATION -> traits.social * 0.3f + traits.attachment * 0.2f
            BehaviorCategory.AFFECTIONATE_RARE -> traits.social * 0.2f
            BehaviorCategory.GREETING -> traits.social * 0.15f
            BehaviorCategory.IDLE -> traits.lazy * 0.2f
            BehaviorCategory.COMFORT_SEEKING -> traits.attachment * 0.2f
            else -> 0f
        }
    }

    private fun computeLifecycleWeight(ctx: ScoringContext, cat: BehaviorCategory): Float {
        val mods = LifecycleBaselineModifiers.for_(ctx.dayPhase)
        return when (cat) {
            BehaviorCategory.INVITATION -> mods.initiativeBias * 0.2f
            BehaviorCategory.GREETING -> (mods.greetingWarmthMod - 1f) * 0.3f
            BehaviorCategory.IDLE -> if (ctx.dayPhase == DayPhase.NIGHT) 0.3f else 0f
            else -> 0f
        }
    }

    private fun computeSuppressionPenalty(ctx: ScoringContext, cat: BehaviorCategory): Float {
        if (cat != BehaviorCategory.INVITATION) return 0f
        return InvitationSuppressionRules.penaltyForIgnoredCount(ctx.invitationIgnoredCount)
    }

    private fun computeCooldownPenalty(ctx: ScoringContext, cat: BehaviorCategory): Float {
        if (cat != BehaviorCategory.INVITATION && cat != BehaviorCategory.AFFECTIONATE_RARE) return 0f
        val msSinceLast = ctx.nowMs - ctx.lastInvitationMs
        val cooldownMs = if (cat == BehaviorCategory.AFFECTIONATE_RARE) RARE_COOLDOWN_MS else INVITE_COOLDOWN_MS
        return if (msSinceLast < cooldownMs) {
            (1f - msSinceLast.toFloat() / cooldownMs).coerceIn(0f, 0.8f)
        } else 0f
    }

    private companion object {
        val INVITE_COOLDOWN_MS = 10 * 60_000L
        val RARE_COOLDOWN_MS = 60 * 60_000L
    }
}
