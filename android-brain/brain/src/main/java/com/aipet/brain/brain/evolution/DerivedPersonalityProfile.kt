package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.personality.PetTrait

/**
 * Derives a human-readable personality profile label from current trait values.
 * Labels are for debug, analytics, and behavior rule selection.
 * They do NOT replace traits — they summarize them.
 */
object DerivedPersonalityProfile {

    fun from(traits: PetTrait): String = when {
        traits.social >= 0.7f && traits.playful >= 0.6f && traits.curious >= 0.5f -> "secure_playful"
        traits.attachment >= 0.7f && traits.social >= 0.6f && traits.lazy <= 0.4f -> "needy_attached"
        traits.curious >= 0.7f && traits.social <= 0.5f && traits.lazy <= 0.4f -> "curious_independent"
        traits.patience >= 0.6f && traits.social >= 0.5f && traits.playful <= 0.5f -> "calm_gentle"
        traits.social <= 0.4f && traits.attachment >= 0.5f && traits.lazy >= 0.5f -> "sensitive_unstable"
        traits.playful >= 0.7f -> "energetic_curious"
        traits.lazy >= 0.7f -> "sleepy_calm"
        traits.social >= 0.6f -> "social_warm"
        else -> "balanced"
    }
}
