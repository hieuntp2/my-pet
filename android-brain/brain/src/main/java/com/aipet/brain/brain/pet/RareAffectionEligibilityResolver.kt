package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait

/**
 * Determines whether a rare affection moment is eligible.
 *
 * Rare affection must be:
 * - infrequent (cooldown enforced)
 * - contextually appropriate (right bond/trust/mood alignment)
 * - personality-compatible
 * - subtle (not gamified)
 *
 * When eligible, the caller selects one of a small set of rare reactions
 * (extra-warm reunion, soft post-soothe spark, special playful moment).
 */
class RareAffectionEligibilityResolver {

    data class EligibilityResult(
        val eligible: Boolean,
        val reason: String
    )

    fun resolve(
        state: PetState,
        traits: PetTrait?,
        lastRareAffectionAtMs: Long,
        now: Long
    ): EligibilityResult {
        val cooldownElapsed = now - lastRareAffectionAtMs
        if (lastRareAffectionAtMs > 0 && cooldownElapsed < PetEmotionalConfig.RARE_COOLDOWN_MS) {
            return EligibilityResult(false, "cooldown_active")
        }
        if (state.bond < PetEmotionalConfig.RARE_MIN_BOND) {
            return EligibilityResult(false, "bond_too_low")
        }
        if (state.trustScore < PetEmotionalConfig.RARE_MIN_TRUST) {
            return EligibilityResult(false, "trust_too_low")
        }
        if (state.careStreak < PetEmotionalConfig.RARE_MIN_CARE_STREAK) {
            return EligibilityResult(false, "care_streak_too_low")
        }
        if (state.neglectStreak > PetEmotionalConfig.RARE_MAX_NEGLECT_STREAK) {
            return EligibilityResult(false, "neglect_streak_too_high")
        }
        if (state.moodValence < PetEmotionalConfig.RARE_MIN_VALENCE) {
            return EligibilityResult(false, "mood_valence_too_low")
        }

        // Attachment trait bonus: high-attachment pets unlock rare moments slightly more readily
        val attachmentBonus = traits?.attachment ?: 0.3f
        if (attachmentBonus < 0.2f && state.attachmentScore < 30) {
            return EligibilityResult(false, "low_attachment_too_low")
        }

        return EligibilityResult(true, "conditions_met")
    }
}
