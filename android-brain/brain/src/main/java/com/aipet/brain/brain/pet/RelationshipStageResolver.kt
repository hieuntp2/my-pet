package com.aipet.brain.brain.pet

/**
 * Resolves the current relationship stage from bond, trust, and attachment scores.
 *
 * Stage must be stable — it should not flicker.
 * A stage up-transition requires all conditions to be met.
 * A stage down-transition is resistant but possible via sustained neglect.
 */
class RelationshipStageResolver {

    fun resolve(state: PetState): RelationshipStage {
        return when {
            meetsBoended(state) -> RelationshipStage.BONDED
            meetsAttached(state) -> RelationshipStage.ATTACHED
            meetsFamiliar(state) -> RelationshipStage.FAMILIAR
            else -> RelationshipStage.STRANGER
        }
    }

    private fun meetsFamiliar(state: PetState): Boolean {
        return state.bond >= PetEmotionalConfig.STAGE_FAMILIAR_BOND_MIN &&
                state.careStreak >= PetEmotionalConfig.STAGE_FAMILIAR_CARE_STREAK_MIN
    }

    private fun meetsAttached(state: PetState): Boolean {
        return state.bond >= PetEmotionalConfig.STAGE_ATTACHED_BOND_MIN &&
                state.trustScore >= PetEmotionalConfig.STAGE_ATTACHED_TRUST_MIN
    }

    private fun meetsBoended(state: PetState): Boolean {
        return state.bond >= PetEmotionalConfig.STAGE_BONDED_BOND_MIN &&
                state.trustScore >= PetEmotionalConfig.STAGE_BONDED_TRUST_MIN &&
                state.attachmentScore >= PetEmotionalConfig.STAGE_BONDED_ATTACHMENT_MIN
    }
}
