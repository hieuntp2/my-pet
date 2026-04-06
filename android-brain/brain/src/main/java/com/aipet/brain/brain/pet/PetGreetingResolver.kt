package com.aipet.brain.brain.pet

import com.aipet.brain.brain.behavior.PetBehaviorContext
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.behavior.PetBehaviorWeightResolver
import com.aipet.brain.brain.personality.PetTrait
import kotlin.math.absoluteValue

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
     * Evolution context fields (evolutionReunionType, evolutionBondAffection, evolutionBondTrust)
     * bias the result when available.
     */
    fun resolveStyle(context: PetGreetingContext): PetGreetingStyle {
        val state = context.state

        // Evolution: RECOVERY_RETURN always starts cautious then relieved
        if (context.evolutionReunionType == "RECOVERY_RETURN") {
            return if (state.careStreak >= 1) PetGreetingStyle.RELIEVED else PetGreetingStyle.HESITANT
        }

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

        // Evolution: long absence with good trust → warm but slightly tentative
        if (context.evolutionReunionType == "LONG_ABSENCE" && context.evolutionBondTrust >= 0.4f) {
            return PetGreetingStyle.WARM
        }

        // Evolution: high affection biases base bond score upward for style selection
        val evoWarmth = context.evolutionBondAffection
        val evoBoostedBond = state.bond + (evoWarmth * 20).toInt()

        return when (context.absenceBucket) {
            AbsenceBucket.SHORT_RETURN -> {
                when {
                    context.isPlayful && state.energy >= 60 -> PetGreetingStyle.PLAYFUL
                    evoBoostedBond >= 40 -> PetGreetingStyle.GENTLE
                    else -> PetGreetingStyle.GENTLE
                }
            }

            AbsenceBucket.MEDIUM_RETURN -> {
                when {
                    evoBoostedBond >= 60 && state.trustScore >= 40 -> PetGreetingStyle.WARM
                    context.isNeedy -> PetGreetingStyle.NEEDY
                    context.isPlayful && state.energy >= 50 -> PetGreetingStyle.PLAYFUL
                    evoBoostedBond >= 30 -> PetGreetingStyle.GENTLE
                    else -> PetGreetingStyle.GENTLE
                }
            }

            AbsenceBucket.LONG_RETURN -> {
                when {
                    evoBoostedBond >= 50 && state.trustScore >= 35 -> PetGreetingStyle.WARM
                    evoBoostedBond >= 30 && state.trustScore >= 25 -> PetGreetingStyle.GENTLE
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
        val messageCandidates = when {
            conditions.contains(PetCondition.HUNGRY) -> {
                if (decision.selectedBehavior == PetEmotion.CURIOUS) {
                    listOf(
                        "sniff... is there food?",
                        "is it snack time already?",
                        "i smell food... maybe?"
                    )
                } else {
                    listOf(
                        "*nuzzle* wants food",
                        "i am really hungry",
                        "food would help right now"
                    )
                }
            }
            conditions.contains(PetCondition.SLEEPY) -> {
                if (decision.selectedBehavior == PetEmotion.IDLE) {
                    listOf(
                        "still waking up",
                        "slow start right now",
                        "mmm... still waking up"
                    )
                } else {
                    listOf(
                        "*yawn* waking up",
                        "waking up... give me a second",
                        "*yawn* eyes still heavy"
                    )
                }
            }
            style == PetGreetingStyle.DISTANT -> listOf(
                "...oh, you're back",
                "...you came back",
                "you're back."
            )
            style == PetGreetingStyle.HESITANT -> listOf(
                "oh... hi",
                "h-hi...",
                "um... hi"
            )
            style == PetGreetingStyle.RELIEVED -> listOf(
                "you came back",
                "i am glad you came back",
                "you are back... thank you"
            )
            style == PetGreetingStyle.NEEDY -> listOf(
                "missed you so much",
                "i really missed you",
                "stay close with me, please"
            )
            conditions.contains(PetCondition.LONELY) -> {
                if (decision.selectedBehavior == PetEmotion.HAPPY) {
                    listOf(
                        "missed you a lot",
                        "i've been waiting for you",
                        "happy you're here with me"
                    )
                } else {
                    listOf(
                        "missed you",
                        "felt a bit alone",
                        "i wanted you here"
                    )
                }
            }
            // Keep calm baseline stable for tests and for a recognizable home identity.
            conditions.contains(PetCondition.CALM) -> listOf("glad you're here")
            else -> when (decision.selectedBehavior) {
                PetEmotion.EXCITED,
                PetEmotion.HAPPY -> listOf(
                    "so happy to see you!",
                    "yay, you're here!",
                    "this made me really happy!"
                )
                PetEmotion.CURIOUS -> listOf(
                    "oh, you're here!",
                    "hey, what are we doing now?",
                    "hmm, you're back."
                )
                else -> listOf(
                    "hello there",
                    "hi there",
                    "hey, i'm here"
                )
            }
        }
        val message = pickGreetingMessage(
            context = context,
            decision = decision,
            candidates = messageCandidates
        )

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

    private fun pickGreetingMessage(
        context: PetGreetingContext,
        decision: PetBehaviorDecision<PetEmotion>,
        candidates: List<String>
    ): String {
        if (candidates.isEmpty()) {
            return "hello there"
        }
        if (candidates.size == 1) {
            return candidates.first()
        }
        val seed = buildString(capacity = 96) {
            append(context.absenceBucket.name)
            append("|")
            append(decision.selectedBehavior.name)
            append("|")
            append(context.state.lastUpdatedAt / 1_000L)
            append("|")
            append(context.state.lastOpenAt / 1_000L)
            append("|")
            append(context.state.lastMeaningfulInteractionAt / 1_000L)
            append("|")
            append(context.state.bond)
            append("|")
            append(context.state.social)
            append("|")
            append(context.conditions.sortedBy { it.name }.joinToString(separator = ",") { it.name })
        }
        val index = seed.hashCode().absoluteValue % candidates.size
        return candidates[index]
    }
}

