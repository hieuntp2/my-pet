package com.aipet.brain.brain.pet

enum class PetCondition {
    HUNGRY,
    SLEEPY,
    LONELY,
    PLAYFUL
}

class PetConditionResolver {
    fun resolve(state: PetState): Set<PetCondition> {
        val conditions = linkedSetOf<PetCondition>()
        if (state.hunger > 70) {
            conditions += PetCondition.HUNGRY
        }
        if (state.sleepiness > 70) {
            conditions += PetCondition.SLEEPY
        }
        if (state.social < 30) {
            conditions += PetCondition.LONELY
        }
        if (state.energy > 70) {
            conditions += PetCondition.PLAYFUL
        }
        return conditions
    }
}
