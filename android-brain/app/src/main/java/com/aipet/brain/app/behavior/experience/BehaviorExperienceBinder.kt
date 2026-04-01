package com.aipet.brain.app.behavior.experience

import com.aipet.brain.brain.b2.domain.AnimationFamily
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.domain.BubblePolicy
import com.aipet.brain.brain.b2.domain.Interruptibility
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import kotlin.math.absoluteValue

/**
 * Converts [BehaviorPlan] into one coordinated experience bundle for Home runtime execution.
 */
class BehaviorExperienceBinder(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun resolve(
        plan: BehaviorPlan,
        currentEmotion: PetEmotion,
        currentConditions: Set<PetCondition>,
        appOpenGreeting: PetGreetingReaction?
    ): PetExperienceBundle {
        val visualDirective = VisualDirective(
            intent = resolveVisualIntent(plan, currentEmotion, currentConditions),
            holdMs = resolveHoldMs(plan),
            settleMs = resolveSettleMs(plan)
        )
        val audioDirective = resolveAudioDirective(plan, appOpenGreeting)
        val talkDirective = resolveTalkDirective(plan, appOpenGreeting)

        return PetExperienceBundle(
            sourcePlanId = plan.id,
            sourceIntention = plan.intention,
            debugLabel = plan.debugLabel,
            visualDirective = visualDirective,
            audioDirective = audioDirective,
            talkDirective = talkDirective,
            priority = mapPriority(plan.interruptPriority),
            interruptPolicy = mapInterruptPolicy(plan.interruptibility),
            antiRepeatKey = "${plan.intention.name}_${plan.animationFamily.name}",
            minimumRepeatIntervalMs = resolveRepeatIntervalMs(plan),
            cooldownMs = resolveCooldownMs(plan)
        )
    }

    private fun resolveVisualIntent(
        plan: BehaviorPlan,
        currentEmotion: PetEmotion,
        currentConditions: Set<PetCondition>
    ): PixelPetAvatarIntent {
        return when (plan.animationFamily) {
            AnimationFamily.IDLE_CALM -> if (PetCondition.SLEEPY in currentConditions) {
                PixelPetAvatarIntent.LOW_ENERGY
            } else {
                PixelPetAvatarIntent.NEUTRAL
            }
            AnimationFamily.IDLE_CURIOUS -> PixelPetAvatarIntent.ATTENTIVE
            AnimationFamily.IDLE_SLEEPY -> PixelPetAvatarIntent.LOW_ENERGY
            AnimationFamily.IDLE_LONELY -> PixelPetAvatarIntent.LONELY_NEED
            AnimationFamily.SOCIAL_WARM -> PixelPetAvatarIntent.ENGAGED
            AnimationFamily.SOCIAL_PLAYFUL -> PixelPetAvatarIntent.EXCITED
            AnimationFamily.SOCIAL_EXCITED -> PixelPetAvatarIntent.EXCITED
            AnimationFamily.REACT_HAPPY -> if (currentEmotion == PetEmotion.EXCITED) {
                PixelPetAvatarIntent.EXCITED
            } else {
                PixelPetAvatarIntent.ENGAGED
            }
            AnimationFamily.REACT_STARTLED -> PixelPetAvatarIntent.SURPRISED
            AnimationFamily.REACT_IRRITATED -> PixelPetAvatarIntent.ANNOYED
            AnimationFamily.REACT_COMFORT -> PixelPetAvatarIntent.ENGAGED
            AnimationFamily.PLAY_ACTIVE -> PixelPetAvatarIntent.EXCITED
            AnimationFamily.PLAY_INVITATION -> PixelPetAvatarIntent.EXCITED
            AnimationFamily.WITHDRAW_INWARD -> PixelPetAvatarIntent.SAD
            AnimationFamily.SELF_SOOTHE -> PixelPetAvatarIntent.LOW_ENERGY
            AnimationFamily.NONE -> mapEmotionFallback(currentEmotion, currentConditions)
        }
    }

    private fun mapEmotionFallback(
        emotion: PetEmotion,
        currentConditions: Set<PetCondition>
    ): PixelPetAvatarIntent {
        return when {
            emotion == PetEmotion.EXCITED -> PixelPetAvatarIntent.EXCITED
            emotion == PetEmotion.HAPPY -> PixelPetAvatarIntent.ENGAGED
            emotion == PetEmotion.CURIOUS -> PixelPetAvatarIntent.ATTENTIVE
            emotion == PetEmotion.SAD -> PixelPetAvatarIntent.SAD
            emotion == PetEmotion.HUNGRY || PetCondition.HUNGRY in currentConditions ->
                PixelPetAvatarIntent.HUNGRY_NEED
            emotion == PetEmotion.SLEEPY || PetCondition.SLEEPY in currentConditions ->
                PixelPetAvatarIntent.LOW_ENERGY
            else -> PixelPetAvatarIntent.NEUTRAL
        }
    }

    private fun resolveAudioDirective(
        plan: BehaviorPlan,
        appOpenGreeting: PetGreetingReaction?
    ): AudioDirective? {
        val category = when {
            appOpenGreeting != null && plan.intention in APP_OPEN_SOCIAL_INTENTIONS -> {
                AudioCategory.GREETING
            }
            plan.intention == PetIntention.STARTLE_RECOVER -> AudioCategory.SURPRISED
            plan.intention == PetIntention.RESPOND_TO_USER -> AudioCategory.ACKNOWLEDGMENT
            plan.intention == PetIntention.CELEBRATE ||
                plan.intention == PetIntention.PLAY ||
                plan.intention == PetIntention.INVITE_PLAY -> AudioCategory.HAPPY
            plan.intention == PetIntention.SEEK_ATTENTION -> AudioCategory.GREETING
            plan.intention == PetIntention.REQUEST_FOOD -> AudioCategory.CURIOUS
            plan.intention == PetIntention.LISTEN ||
                plan.intention == PetIntention.INVESTIGATE -> AudioCategory.CURIOUS
            else -> null
        } ?: return null

        val cooldownKey = "behavior_${plan.intention.name.lowercase()}_${category.name.lowercase()}"
        return AudioDirective(
            category = category,
            cooldownKey = cooldownKey,
            minIntervalMs = resolveAudioIntervalMs(plan, category)
        )
    }

    private fun resolveAudioIntervalMs(
        plan: BehaviorPlan,
        category: AudioCategory
    ): Long {
        return when {
            category == AudioCategory.SURPRISED -> 2_400L
            plan.intention == PetIntention.RESPOND_TO_USER -> 1_400L
            plan.intention == PetIntention.SEEK_ATTENTION -> 4_000L
            plan.intention == PetIntention.INVITE_PLAY -> 5_000L
            else -> 2_000L
        }
    }

    private fun resolveTalkDirective(
        plan: BehaviorPlan,
        appOpenGreeting: PetGreetingReaction?
    ): TalkDirective? {
        val issuedAtMs = nowProvider()
        val greetingMessage = appOpenGreeting?.message
            ?.takeIf { plan.intention in APP_OPEN_SOCIAL_INTENTIONS }
        if (plan.bubblePolicy == BubblePolicy.NONE && greetingMessage == null) {
            return null
        }
        val message = greetingMessage ?: resolveIntentionTalkLine(plan, issuedAtMs)
        val dedupeKey = greetingMessage?.let { "app_open_greeting" }
            ?: "behavior_${plan.intention.name.lowercase()}"
        val maxDisplayMs = if (greetingMessage != null) {
            3_600L
        } else {
            when (plan.bubblePolicy) {
                BubblePolicy.FORCE_SHOW -> 3_600L
                BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN -> 3_000L
                BubblePolicy.CLEAR -> 1_000L
                BubblePolicy.NONE -> 0L
            }
        }

        return TalkDirective(
            message = message,
            dedupeKey = dedupeKey,
            maxDisplayMs = maxDisplayMs,
            minIntervalMs = if (greetingMessage != null || plan.bubblePolicy == BubblePolicy.FORCE_SHOW) {
                8_000L
            } else {
                6_000L
            },
            issuedAtMs = issuedAtMs
        )
    }

    private fun resolveIntentionTalkLine(
        plan: BehaviorPlan,
        issuedAtMs: Long
    ): String {
        val pool = when (plan.intention) {
            PetIntention.SEEK_ATTENTION -> listOf(
                "Hey, stay with me for a bit.",
                "I want your attention right now.",
                "I am right here with you."
            )
            PetIntention.SEEK_COMFORT -> listOf(
                "Can I stay close for a moment?",
                "I feel better when you are near.",
                "A little comfort would help."
            )
            PetIntention.INVITE_PLAY -> listOf(
                "Play with me?",
                "I am ready for a game.",
                "Let us do something fun."
            )
            PetIntention.REQUEST_FOOD -> listOf(
                "I am getting hungry.",
                "Food time?",
                "My stomach is asking for a snack."
            )
            PetIntention.LISTEN -> listOf(
                "I heard that.",
                "I am listening.",
                "Hold on, I am paying attention."
            )
            PetIntention.STARTLE_RECOVER -> listOf(
                "That sound startled me.",
                "Whoa, that was loud.",
                "Give me a second to settle."
            )
            PetIntention.CELEBRATE -> listOf(
                "Yes, that felt great!",
                "That was fun!",
                "I loved that."
            )
            PetIntention.RECOVER -> listOf(
                "I am calming down now.",
                "I feel steadier again.",
                "Back to a calmer pace."
            )
            else -> listOf(
                "I am here.",
                "Staying with you.",
                "Taking this moment in."
            )
        }
        val seed = "${plan.id}_${issuedAtMs / 5_000L}"
        val index = seed.hashCode().absoluteValue % pool.size
        return pool[index]
    }

    private fun mapPriority(interruptPriority: Int): ExperienceExecutionPriority {
        return when {
            interruptPriority >= 6 -> ExperienceExecutionPriority.CRITICAL
            interruptPriority >= 4 -> ExperienceExecutionPriority.HIGH
            interruptPriority >= 2 -> ExperienceExecutionPriority.NORMAL
            interruptPriority >= 1 -> ExperienceExecutionPriority.LOW
            else -> ExperienceExecutionPriority.IDLE
        }
    }

    private fun mapInterruptPolicy(interruptibility: Interruptibility): ExperienceInterruptPolicy {
        return when (interruptibility) {
            Interruptibility.LOCKED -> ExperienceInterruptPolicy.LOCKED
            Interruptibility.HIGH_PRIORITY_ONLY -> ExperienceInterruptPolicy.HIGH_PRIORITY_ONLY
            Interruptibility.NORMAL -> ExperienceInterruptPolicy.NORMAL
            Interruptibility.FREE -> ExperienceInterruptPolicy.FREE
        }
    }

    private fun resolveHoldMs(plan: BehaviorPlan): Long {
        val intentionDefault = when (plan.intention) {
            PetIntention.STARTLE_RECOVER -> 1_400L
            PetIntention.RESPOND_TO_USER -> 1_100L
            PetIntention.CELEBRATE -> 1_800L
            PetIntention.INVITE_PLAY -> 2_200L
            PetIntention.PLAY -> 2_000L
            PetIntention.LISTEN -> 1_200L
            PetIntention.SEEK_ATTENTION -> 2_200L
            PetIntention.SEEK_COMFORT -> 2_000L
            PetIntention.REQUEST_FOOD -> 2_100L
            PetIntention.INVESTIGATE -> 1_700L
            PetIntention.OBSERVE -> 1_700L
            PetIntention.STAY_NEAR -> 2_000L
            PetIntention.WITHDRAW -> 1_800L
            PetIntention.SELF_SOOTHE -> 2_100L
            PetIntention.RECOVER -> 1_900L
            PetIntention.REST -> 1_800L
            PetIntention.DOZE -> 2_100L
        }
        val expected = plan.expectedDurationMs.takeIf { it > 0L }
        return (expected ?: intentionDefault).coerceIn(800L, 4_800L)
    }

    private fun resolveSettleMs(plan: BehaviorPlan): Long {
        return when (plan.intention) {
            PetIntention.STARTLE_RECOVER -> 520L
            PetIntention.CELEBRATE -> 640L
            PetIntention.PLAY,
            PetIntention.INVITE_PLAY -> 560L
            else -> 420L
        }
    }

    private fun resolveRepeatIntervalMs(plan: BehaviorPlan): Long {
        return when (plan.intention) {
            PetIntention.STARTLE_RECOVER -> 2_000L
            PetIntention.RESPOND_TO_USER -> 900L
            PetIntention.INVITE_PLAY -> 5_500L
            PetIntention.SEEK_ATTENTION -> 4_500L
            PetIntention.REQUEST_FOOD -> 5_000L
            else -> 2_500L
        }
    }

    private fun resolveCooldownMs(plan: BehaviorPlan): Long {
        return when (plan.intention) {
            PetIntention.STARTLE_RECOVER -> 650L
            PetIntention.CELEBRATE -> 800L
            PetIntention.RESPOND_TO_USER -> 480L
            PetIntention.INVITE_PLAY -> 900L
            else -> 520L
        }
    }

    private companion object {
        private val APP_OPEN_SOCIAL_INTENTIONS = setOf(
            PetIntention.STAY_NEAR,
            PetIntention.SEEK_ATTENTION,
            PetIntention.RESPOND_TO_USER,
            PetIntention.CELEBRATE
        )
    }
}
