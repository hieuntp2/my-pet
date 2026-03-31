package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait

/**
 * Determines whether and what kind of invitation the pet can emit.
 *
 * An invitation is a small bid for attention — NOT a notification.
 * It must feel organic and optional.
 *
 * Rules:
 * - only invites when the pet genuinely wants interaction
 * - suppressed when recent invitations were ignored
 * - suppressed when pet is sleepy, distant, or overstimulated
 * - personality influences invitation type and drive strength
 */
class InvitationEligibilityResolver {

    data class EligibilityResult(
        val eligible: Boolean,
        val invitationType: InvitationType?,
        val suppressionReason: String?
    )

    fun resolve(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        session: SessionInteractionState,
        now: Long
    ): EligibilityResult {
        // Hard suppression conditions
        val suppressionReason = evaluateSuppression(state, conditions, session, now)
        if (suppressionReason != null) {
            return EligibilityResult(eligible = false, invitationType = null, suppressionReason = suppressionReason)
        }

        val invitationType = selectInvitationType(state, conditions, traits)
        return EligibilityResult(
            eligible = invitationType != null,
            invitationType = invitationType,
            suppressionReason = null
        )
    }

    private fun evaluateSuppression(
        state: PetState,
        conditions: Set<PetCondition>,
        session: SessionInteractionState,
        now: Long
    ): String? {
        if (conditions.contains(PetCondition.SLEEPY) || state.energy < PetEmotionalConfig.INVITATION_MIN_ENERGY) {
            return "sleepy_or_low_energy"
        }
        if (conditions.contains(PetCondition.DISTANT)) {
            return "distant_state"
        }
        if (conditions.contains(PetCondition.OVERSTIMULATED)) {
            return "overstimulated"
        }
        if (session.invitationCount >= PetEmotionalConfig.INVITATION_PER_SESSION_MAX) {
            return "session_invite_cap"
        }
        if (session.ignoredInvitationCount >= PetEmotionalConfig.INVITATION_IGNORED_SUPPRESS_COUNT) {
            return "too_many_ignored"
        }
        val cooldownElapsed = now - session.lastInvitationAtMs
        if (session.lastInvitationAtMs > 0 && cooldownElapsed < PetEmotionalConfig.INVITATION_COOLDOWN_MS) {
            return "cooldown_active"
        }
        if (state.social > PetEmotionalConfig.INVITATION_MIN_SOCIAL_NEED &&
            state.stimulation > PetEmotionalConfig.INVITATION_MAX_STIMULATION
        ) {
            return "already_stimulated"
        }
        return null
    }

    private fun selectInvitationType(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?
    ): InvitationType? {
        // Need to have sufficient social need or attachment drive
        val needsDriven = state.social <= PetEmotionalConfig.INVITATION_MIN_SOCIAL_NEED
        val attachmentDriven = state.attachmentScore >= PetEmotionalConfig.INVITATION_MIN_ATTACHMENT
        if (!needsDriven && !attachmentDriven) return null

        val playfulBias = traits?.playful ?: 0.5f
        val curiosityBias = traits?.curious ?: 0.5f
        val attachmentBias = traits?.attachment ?: 0.3f

        return when {
            conditions.contains(PetCondition.NEEDY) && attachmentBias >= 0.4f -> InvitationType.NEEDY_LOOK
            conditions.contains(PetCondition.BORED) && playfulBias >= 0.5f -> InvitationType.PLAY_INVITE
            curiosityBias >= 0.5f && state.stimulation <= 40 -> InvitationType.CURIOUS_GLANCE
            needsDriven -> InvitationType.SOFT_CHECKIN
            else -> null
        }
    }
}
