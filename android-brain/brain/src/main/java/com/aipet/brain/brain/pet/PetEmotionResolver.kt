package com.aipet.brain.brain.pet

class PetEmotionResolver {
    fun resolve(
        state: PetState,
        conditions: Set<PetCondition>
    ): PetEmotion {
        return when {
            conditions.contains(PetCondition.SLEEPY) || (state.sleepiness >= 75 && state.energy <= 35) -> {
                PetEmotion.SLEEPY
            }

            conditions.contains(PetCondition.HUNGRY) || state.hunger >= 75 -> {
                PetEmotion.HUNGRY
            }

            state.energy >= 80 && state.social >= 40 -> {
                PetEmotion.EXCITED
            }

            conditions.contains(PetCondition.LONELY) || state.mood == PetMood.SAD -> {
                PetEmotion.SAD
            }

            conditions.contains(PetCondition.CALM) -> {
                PetEmotion.IDLE
            }

            state.mood == PetMood.CURIOUS -> {
                PetEmotion.CURIOUS
            }

            conditions.contains(PetCondition.PLAYFUL) || state.mood == PetMood.HAPPY || state.bond >= 70 -> {
                PetEmotion.HAPPY
            }

            else -> PetEmotion.IDLE
        }
    }
}
