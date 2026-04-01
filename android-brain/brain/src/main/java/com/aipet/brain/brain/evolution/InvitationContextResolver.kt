package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.UserHabitProfile

/**
 * Resolves what type of invitation the pet should emit, or whether to suppress entirely.
 *
 * Anti-spam rules are enforced:
 * - Hard cooldown between invitations
 * - Confidence threshold (bond/trust must meet minimum)
 * - Suppression after repeated ignores with gradual recovery
 * - Day-phase gate (no invitations at NIGHT unless dependency is very high)
 */
class InvitationContextResolver {

    data class InvitationDecision(
        val shouldInvite: Boolean,
        val intentType: InvitationIntentType?,
        val suppressionReason: String?,
        val confidenceScore: Float
    )

    fun resolve(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        bond: BondStateV2,
        habitProfile: UserHabitProfile,
        dayPhase: DayPhase,
        reunionType: ReunionType,
        expectationState: ExpectedReturnWindowResolver.ExpectationState,
        ignoredCount: Int,
        msSinceLastInvitation: Long,
        nowMs: Long
    ): InvitationDecision {
        // Hard cooldown
        if (msSinceLastInvitation < HARD_COOLDOWN_MS) {
            return suppressed("cooldown")
        }

        // Night suppression unless dependency is very high
        if (dayPhase == DayPhase.NIGHT && bond.dependency < HIGH_DEPENDENCY_THRESHOLD) {
            return suppressed("night_phase")
        }

        // Hard state suppression
        if (conditions.contains(PetCondition.SLEEPY) || state.energy < MIN_ENERGY) {
            return suppressed("sleepy_or_low_energy")
        }
        if (conditions.contains(PetCondition.OVERSTIMULATED)) {
            return suppressed("overstimulated")
        }
        if (conditions.contains(PetCondition.DISTANT)) {
            return suppressed("distant")
        }

        // Trust minimum — pet won't initiate if trust is very low
        if (bond.trust < MIN_TRUST_TO_INVITE) {
            return suppressed("trust_too_low")
        }

        // Suppression from ignored invitations (confidence degrades)
        val suppressionPenalty = when {
            ignoredCount >= 4 -> return suppressed("ignored_too_many_times")
            ignoredCount >= 3 -> 0.5f
            ignoredCount >= 2 -> 0.3f
            ignoredCount == 1 -> 0.1f
            else -> 0f
        }

        val confidenceScore = computeConfidence(state, bond, traits, expectationState) - suppressionPenalty
        if (confidenceScore < MIN_CONFIDENCE_TO_INVITE) {
            return suppressed("confidence_below_threshold")
        }

        val intent = resolveIntent(state, conditions, traits, bond, dayPhase, reunionType, expectationState)
        return InvitationDecision(
            shouldInvite = true,
            intentType = intent,
            suppressionReason = null,
            confidenceScore = confidenceScore
        )
    }

    private fun computeConfidence(
        state: PetState,
        bond: BondStateV2,
        traits: PetTrait?,
        expectation: ExpectedReturnWindowResolver.ExpectationState
    ): Float {
        var confidence = 0f
        confidence += state.energy / 100f * 0.2f
        confidence += bond.affection * 0.3f
        confidence += bond.trust * 0.2f
        confidence += (traits?.social ?: 0.5f) * 0.15f
        if (expectation == ExpectedReturnWindowResolver.ExpectationState.ON_TIME) confidence += 0.15f
        return confidence.coerceIn(0f, 1f)
    }

    private fun resolveIntent(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        bond: BondStateV2,
        dayPhase: DayPhase,
        reunionType: ReunionType,
        expectationState: ExpectedReturnWindowResolver.ExpectationState
    ): InvitationIntentType {
        // Recovery arc
        if (reunionType == ReunionType.RECOVERY_RETURN && bond.trust < 0.4f) {
            return InvitationIntentType.TENTATIVE_REACH_OUT
        }
        // Night / low energy: quiet
        if (dayPhase == DayPhase.NIGHT) return InvitationIntentType.QUIET_PRESENCE
        // Expectant — waiting was fulfilled
        if (expectationState == ExpectedReturnWindowResolver.ExpectationState.ON_TIME &&
            bond.dependency > 0.5f
        ) {
            return InvitationIntentType.EXPECTANT
        }
        // Comfort seeking
        if (conditions.contains(PetCondition.NEEDY) || (traits?.attachment ?: 0f) > 0.65f) {
            return InvitationIntentType.COMFORT_SEEKING
        }
        // Default: playful
        return InvitationIntentType.PLAYFUL
    }

    private fun suppressed(reason: String) = InvitationDecision(
        shouldInvite = false,
        intentType = null,
        suppressionReason = reason,
        confidenceScore = 0f
    )

    private companion object {
        val HARD_COOLDOWN_MS = 8 * 60_000L
        const val MIN_ENERGY = 35
        const val MIN_TRUST_TO_INVITE = 0.15f
        const val MIN_CONFIDENCE_TO_INVITE = 0.25f
        const val HIGH_DEPENDENCY_THRESHOLD = 0.75f
    }
}
