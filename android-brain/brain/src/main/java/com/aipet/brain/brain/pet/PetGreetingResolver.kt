package com.aipet.brain.brain.pet

import com.aipet.brain.brain.behavior.PetBehaviorContext
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.behavior.PetBehaviorWeightResolver
import com.aipet.brain.brain.personality.PetTrait

data class PetGreetingReaction(
    val message: String,
    val emotion: PetEmotion,
    val reason: String,
    val greetingStyle: PetGreetingStyle = PetGreetingStyle.GENTLE
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
        return resolveWithContext(
            context = PetGreetingContext(
                absenceBucket = AbsenceBucket.MEDIUM_RETURN,
                state = state,
                conditions = conditions,
                traits = traits
            ),
            emotion = emotion
        )
    }

    /**
     * Full v2 resolution using absence bucket and complete pet context.
     * This is the preferred resolution path for the app-open lifecycle.
     */
    fun resolveWithContext(
        context: PetGreetingContext,
        emotion: PetEmotion
    ): PetGreetingResolution {
        val decision = behaviorWeightResolver.resolveGreetingEmotion(
            context = PetBehaviorContext(
                state = context.state,
                conditions = context.conditions,
                traits = context.traits
            ),
            fallbackEmotion = emotion
        )

        val greetingStyle = resolveStyle(context)
        val reaction = buildReaction(context, decision, greetingStyle)
        return PetGreetingResolution(
            reaction = reaction,
            decision = decision
        )
    }

    /**
     * Resolves greeting style from context + selected emotion.
     * Style is independent from emotion — same emotion can be warm or hesitant.
     */
    fun resolveStyle(context: PetGreetingContext): PetGreetingStyle {
        val state = context.state

        // Distant state always produces hesitant or distant style
        if (context.isDistant && state.trustScore <= 25) {
            return PetGreetingStyle.DISTANT
        }
        if (context.isDistant || state.neglectStreak >= 3) {
            return PetGreetingStyle.HESITANT
        }

        // Sleepy overrides most warmth
        if (context.isSleepy) {
            return PetGreetingStyle.SLEEPY
        }

        // After extensive neglect that was recently repaired (care streak > 0, neglect > 0)
        if (state.neglectStreak >= 1 && state.careStreak >= 1) {
            return PetGreetingStyle.RELIEVED
        }

        return when (context.absenceBucket) {
            AbsenceBucket.SHORT_RETURN -> {
                when {
                    context.isPlayful && state.energy >= 60 -> PetGreetingStyle.PLAYFUL
                    state.bond >= 40 -> PetGreetingStyle.GENTLE
                    else -> PetGreetingStyle.GENTLE
                }
            }

            AbsenceBucket.MEDIUM_RETURN -> {
                when {
                    state.bond >= 60 && state.trustScore >= 40 -> PetGreetingStyle.WARM
                    context.isNeedy -> PetGreetingStyle.NEEDY
                    context.isPlayful && state.energy >= 50 -> PetGreetingStyle.PLAYFUL
                    state.bond >= 30 -> PetGreetingStyle.GENTLE
                    else -> PetGreetingStyle.GENTLE
                }
            }

            AbsenceBucket.LONG_RETURN -> {
                when {
                    state.bond >= 50 && state.trustScore >= 35 -> PetGreetingStyle.WARM
                    state.bond >= 30 && state.trustScore >= 25 -> PetGreetingStyle.GENTLE
                    context.isNeedy -> PetGreetingStyle.NEEDY
                    else -> PetGreetingStyle.HESITANT
                }
            }

            AbsenceBucket.NEGLECT_RETURN -> {
                if (state.trustScore <= 30) PetGreetingStyle.DISTANT else PetGreetingStyle.HESITANT
            }
        }
    }

    private fun buildReaction(
        context: PetGreetingContext,
        decision: PetBehaviorDecision<PetEmotion>,
        style: PetGreetingStyle
    ): PetGreetingReaction {
        val conditions = context.conditions
        val message = when {
            conditions.contains(PetCondition.HUNGRY) -> {
                if (decision.selectedBehavior == PetEmotion.CURIOUS) "sniff... is there food?" else "*nuzzle* wants food"
            }
            conditions.contains(PetCondition.SLEEPY) -> {
                if (decision.selectedBehavior == PetEmotion.IDLE) "still waking up" else "*yawn* waking up"
            }
            style == PetGreetingStyle.DISTANT -> "...oh, you're back"
            style == PetGreetingStyle.HESITANT -> "oh... hi"
            style == PetGreetingStyle.RELIEVED -> "you came back"
            style == PetGreetingStyle.NEEDY -> "missed you so much"
            conditions.contains(PetCondition.LONELY) -> {
                if (decision.selectedBehavior == PetEmotion.HAPPY) "missed you a lot" else "missed you"
            }
            conditions.contains(PetCondition.CALM) -> "glad you're here"
            else -> when (decision.selectedBehavior) {
                PetEmotion.EXCITED -> "so happy to see you!"
                PetEmotion.HAPPY -> "so happy to see you!"
                PetEmotion.CURIOUS -> "oh, you're here!"
                else -> "hello there"
            }
        }

        val emotion = when {
            conditions.contains(PetCondition.HUNGRY) -> PetEmotion.HUNGRY
            conditions.contains(PetCondition.SLEEPY) -> PetEmotion.SLEEPY
            style == PetGreetingStyle.DISTANT -> PetEmotion.DISTANT
            style == PetGreetingStyle.HESITANT -> PetEmotion.SHY
            style == PetGreetingStyle.RELIEVED -> PetEmotion.RELIEVED
            style == PetGreetingStyle.NEEDY -> PetEmotion.NEEDY
            conditions.contains(PetCondition.LONELY) -> {
                if (decision.selectedBehavior == PetEmotion.HAPPY) PetEmotion.HAPPY else PetEmotion.SAD
            }
            else -> decision.selectedBehavior
        }

        return PetGreetingReaction(
            message = message,
            emotion = emotion,
            reason = decision.selectedLabel,
            greetingStyle = style
        )
    }
}

