package com.aipet.brain.brain.pet

enum class PetCondition {
    HUNGRY,
    SLEEPY,
    LONELY,
    PLAYFUL,
    CALM,
    // v2 conditions
    NEEDY,          // craving social comfort; high social need + moderate bond
    BORED,          // low stimulation, moderate energy
    DISTANT,        // coolness from neglect or low trust
    OVERSTIMULATED  // too many rapid interactions in a short window
}

class PetConditionResolver {
    fun resolve(state: PetState): Set<PetCondition> {
        val conditions = linkedSetOf<PetCondition>()

        if (state.hunger >= HUNGER_THRESHOLD) {
            conditions += PetCondition.HUNGRY
        }
        if (state.sleepiness >= SLEEPINESS_THRESHOLD || state.energy <= LOW_ENERGY_THRESHOLD) {
            conditions += PetCondition.SLEEPY
        }
        if (state.social <= LONELINESS_THRESHOLD) {
            conditions += PetCondition.LONELY
        }
        if (
            state.energy >= PLAYFUL_ENERGY_THRESHOLD &&
            state.sleepiness <= PLAYFUL_SLEEPINESS_MAX
        ) {
            conditions += PetCondition.PLAYFUL
        }

        // v2: NEEDY — high social need, decent bond but not fulfilled
        if (state.social <= NEEDY_SOCIAL_THRESHOLD && state.bond >= NEEDY_BOND_MIN) {
            conditions += PetCondition.NEEDY
        }

        // v2: BORED — low stimulation with enough energy to want engagement
        if (state.stimulation <= BORED_STIMULATION_THRESHOLD && state.energy >= BORED_ENERGY_MIN) {
            conditions += PetCondition.BORED
        }

        // v2: DISTANT — trust is low OR neglect streak is significant
        if (state.trustScore <= DISTANT_TRUST_THRESHOLD || state.neglectStreak >= DISTANT_NEGLECT_STREAK) {
            conditions += PetCondition.DISTANT
        }

        // v2: OVERSTIMULATED — stimulation is very high (set externally by CareActionProcessor after burst)
        if (state.stimulation >= OVERSTIMULATED_THRESHOLD) {
            conditions += PetCondition.OVERSTIMULATED
        }

        if (
            conditions.isEmpty() &&
            state.energy in CALM_ENERGY_RANGE &&
            state.hunger <= CALM_HUNGER_MAX &&
            state.sleepiness <= CALM_SLEEPINESS_MAX &&
            state.social >= CALM_SOCIAL_MIN
        ) {
            conditions += PetCondition.CALM
        }
        return conditions
    }

    private companion object {
        const val HUNGER_THRESHOLD = 70
        const val SLEEPINESS_THRESHOLD = 70
        const val LOW_ENERGY_THRESHOLD = 25
        const val LONELINESS_THRESHOLD = 30
        const val PLAYFUL_ENERGY_THRESHOLD = 70
        const val PLAYFUL_SLEEPINESS_MAX = 55
        val CALM_ENERGY_RANGE = 30..69
        const val CALM_HUNGER_MAX = 60
        const val CALM_SLEEPINESS_MAX = 60
        const val CALM_SOCIAL_MIN = 30
        // v2 thresholds
        const val NEEDY_SOCIAL_THRESHOLD = 35
        const val NEEDY_BOND_MIN = 20
        const val BORED_STIMULATION_THRESHOLD = 25
        const val BORED_ENERGY_MIN = 40
        const val DISTANT_TRUST_THRESHOLD = 20
        const val DISTANT_NEGLECT_STREAK = 3
        const val OVERSTIMULATED_THRESHOLD = 85
    }
}
