package com.aipet.brain.brain.pet

import com.aipet.brain.brain.behavior.PetBehaviorContext
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.behavior.PetBehaviorWeightResolver
import com.aipet.brain.brain.personality.PetTrait

data class PetGreetingReaction(
    val message: String,
    val emotion: PetEmotion,
    val reason: String
)

data class PetGreetingResolution(
    val reaction: PetGreetingReaction,
    val decision: PetBehaviorDecision<PetEmotion>
)

class PetGreetingResolver(
    private val behaviorWeightResolver: PetBehaviorWeightResolver = PetBehaviorWeightResolver()
) {
    fun resolve(
        state: PetState,
        emotion: PetEmotion,
        conditions: Set<PetCondition>,
        traits: PetTrait? = null
    ): PetGreetingReaction {
        return resolveDetailed(
            state = state,
            emotion = emotion,
            conditions = conditions,
            traits = traits
        ).reaction
    }

    fun resolveDetailed(
        state: PetState,
        emotion: PetEmotion,
        conditions: Set<PetCondition>,
        traits: PetTrait? = null
    ): PetGreetingResolution {
        val decision = behaviorWeightResolver.resolveGreetingEmotion(
            context = PetBehaviorContext(
                state = state,
                conditions = conditions,
                traits = traits
            ),
            fallbackEmotion = emotion
        )
        val reaction = when {
            conditions.contains(PetCondition.HUNGRY) -> {
                PetGreetingReaction(
                    message = if (decision.selectedBehavior == PetEmotion.CURIOUS) {
                        "sniff... is there food?"
                    } else {
                        "*nuzzle* wants food"
                    },
                    emotion = PetEmotion.HUNGRY,
                    reason = decision.selectedLabel
                )
            }

            conditions.contains(PetCondition.SLEEPY) -> {
                PetGreetingReaction(
                    message = if (decision.selectedBehavior == PetEmotion.IDLE) {
                        "still waking up"
                    } else {
                        "*yawn* waking up"
                    },
                    emotion = PetEmotion.SLEEPY,
                    reason = decision.selectedLabel
                )
            }

            conditions.contains(PetCondition.LONELY) -> {
                PetGreetingReaction(
                    message = if (decision.selectedBehavior == PetEmotion.HAPPY) {
                        "missed you a lot"
                    } else {
                        "missed you"
                    },
                    emotion = if (decision.selectedBehavior == PetEmotion.HAPPY) PetEmotion.HAPPY else PetEmotion.SAD,
                    reason = decision.selectedLabel
                )
            }

            conditions.contains(PetCondition.CALM) -> {
                PetGreetingReaction(
                    message = "glad you're here",
                    emotion = PetEmotion.IDLE,
                    reason = decision.selectedLabel
                )
            }

            else -> {
                when (decision.selectedBehavior) {
                    PetEmotion.EXCITED -> {
                        PetGreetingReaction(
                            message = "so happy to see you!",
                            emotion = PetEmotion.EXCITED,
                            reason = decision.selectedLabel
                        )
                    }
                    PetEmotion.HAPPY -> {
                        PetGreetingReaction(
                            message = "so happy to see you!",
                            emotion = PetEmotion.HAPPY,
                            reason = decision.selectedLabel
                        )
                    }
                    PetEmotion.CURIOUS -> {
                        PetGreetingReaction(
                            message = "oh, you're here!",
                            emotion = PetEmotion.CURIOUS,
                            reason = decision.selectedLabel
                        )
                    }
                    else -> {
                        PetGreetingReaction(
                            message = "hello there",
                            emotion = decision.selectedBehavior,
                            reason = decision.selectedLabel
                        )
                    }
                }
            }
        }
        return PetGreetingResolution(
            reaction = reaction,
            decision = decision
        )
    }
}
