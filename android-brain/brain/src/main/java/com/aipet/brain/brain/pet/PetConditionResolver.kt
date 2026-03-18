package com.aipet.brain.brain.pet

enum class PetCondition {
    HUNGRY,
    SLEEPY,
    LONELY,
    PLAYFUL,
    CALM
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
            state.sleepiness <= PLAYFUL_SLEEPINESS_MAX &&
            state.hunger <= PLAYFUL_HUNGER_MAX
        ) {
            conditions += PetCondition.PLAYFUL
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
        const val PLAYFUL_HUNGER_MAX = 55
        val CALM_ENERGY_RANGE = 30..69
        const val CALM_HUNGER_MAX = 60
        const val CALM_SLEEPINESS_MAX = 60
        const val CALM_SOCIAL_MIN = 30
    }
}
