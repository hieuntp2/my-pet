package com.aipet.brain.app.gameplay

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityResult
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

internal enum class PetGameplayAction(
    val cooldownMs: Long
) {
    TAP(900L),
    LONG_PRESS(1_300L),
    FEED(1_800L),
    PLAY(1_600L),
    REST(1_800L);

    companion object {
        fun fromInteractionType(interactionType: PetInteractionType): PetGameplayAction {
            return when (interactionType) {
                PetInteractionType.TAP -> TAP
                PetInteractionType.LONG_PRESS -> LONG_PRESS
            }
        }

        fun fromActivityType(activityType: PetActivityType): PetGameplayAction {
            return when (activityType) {
                PetActivityType.FEED -> FEED
                PetActivityType.PLAY -> PLAY
                PetActivityType.REST -> REST
            }
        }
    }
}

internal data class PetGameplayFeedback(
    val text: String,
    val isBlocked: Boolean
)

internal data class PetGameplayCooldownDecision(
    val isAllowed: Boolean,
    val remainingMs: Long
)

internal class PetGameplayCooldownGate {
    private val lastAppliedAtMsByAction = mutableMapOf<PetGameplayAction, Long>()

    fun evaluate(
        action: PetGameplayAction,
        attemptedAtMs: Long
    ): PetGameplayCooldownDecision {
        val previousAtMs = lastAppliedAtMsByAction[action]
            ?: return PetGameplayCooldownDecision(isAllowed = true, remainingMs = 0L)
        val elapsedMs = attemptedAtMs - previousAtMs
        val remainingMs = (action.cooldownMs - elapsedMs).coerceAtLeast(0L)
        return PetGameplayCooldownDecision(
            isAllowed = remainingMs <= 0L,
            remainingMs = remainingMs
        )
    }

    fun recordSuccess(
        action: PetGameplayAction,
        appliedAtMs: Long
    ) {
        lastAppliedAtMsByAction[action] = appliedAtMs
    }
}

internal object PetGameplayFeedbackFormatter {
    fun forInteraction(
        petName: String,
        interactionType: PetInteractionType,
        previousState: PetState,
        updatedState: PetState
    ): PetGameplayFeedback {
        val safeName = petName.ifBlank { "Your pet" }
        val text = when (interactionType) {
            PetInteractionType.TAP -> when {
                updatedState.mood == PetMood.HAPPY -> "$safeName enjoyed that little hello."
                updatedState.mood == PetMood.CURIOUS -> "$safeName perked up at your touch."
                else -> "$safeName noticed your touch."
            }

            PetInteractionType.LONG_PRESS -> when {
                updatedState.mood == PetMood.EXCITED -> "$safeName leaned into the cuddle."
                updatedState.bond > previousState.bond -> "$safeName stayed close with you."
                else -> "$safeName settled into your attention."
            }
        }
        return PetGameplayFeedback(text = text, isBlocked = false)
    }

    fun forActivity(
        petName: String,
        result: PetActivityResult
    ): PetGameplayFeedback {
        val safeName = petName.ifBlank { "Your pet" }
        val text = when (result.activityType) {
            PetActivityType.FEED -> when {
                result.previousState.hunger >= 60 || result.delta.hungerDelta <= -20 -> {
                    "$safeName seems satisfied now."
                }

                result.updatedState.mood == PetMood.HAPPY -> "$safeName enjoyed the snack."
                else -> "$safeName appreciated the meal."
            }

            PetActivityType.PLAY -> when {
                result.delta.socialDelta > 0 && result.delta.bondDelta > 0 -> {
                    "$safeName had fun playing with you."
                }

                result.updatedState.mood == PetMood.EXCITED -> "$safeName looks ready for more fun later."
                else -> "$safeName enjoyed the playtime."
            }

            PetActivityType.REST -> when {
                result.delta.sleepinessDelta < 0 -> "$safeName looks more rested."
                result.delta.energyDelta > 0 -> "$safeName seems a little more energetic."
                else -> "$safeName is settling down."
            }
        }
        return PetGameplayFeedback(text = text, isBlocked = false)
    }

    fun blocked(
        petName: String,
        action: PetGameplayAction,
        remainingMs: Long
    ): PetGameplayFeedback {
        val safeName = petName.ifBlank { "Your pet" }
        val waitMomentText = if (remainingMs >= 1_000L) {
            "about ${((remainingMs + 999L) / 1_000L)}s"
        } else {
            "a moment"
        }
        val text = when (action) {
            PetGameplayAction.TAP -> "$safeName is already reacting. Try another tap in $waitMomentText."
            PetGameplayAction.LONG_PRESS -> "$safeName needs a short pause before another cuddle."
            PetGameplayAction.FEED -> "$safeName just ate. Try feeding again in $waitMomentText."
            PetGameplayAction.PLAY -> "$safeName needs a quick breather before more play."
            PetGameplayAction.REST -> "$safeName is already settling down."
        }
        return PetGameplayFeedback(text = text, isBlocked = true)
    }
}

internal object PetGameplayAudioMapper {
    fun categoryForInteraction(interactionType: PetInteractionType): AudioCategory {
        return when (interactionType) {
            PetInteractionType.TAP -> AudioCategory.ACKNOWLEDGMENT
            PetInteractionType.LONG_PRESS -> AudioCategory.HAPPY
        }
    }

    fun categoryForActivity(result: PetActivityResult): AudioCategory {
        return when (result.activityType) {
            PetActivityType.FEED -> if (result.previousState.hunger >= 60) AudioCategory.HAPPY else AudioCategory.ACKNOWLEDGMENT
            PetActivityType.PLAY -> AudioCategory.HAPPY
            PetActivityType.REST -> if (result.updatedState.mood == PetMood.NEUTRAL) AudioCategory.SLEEPY else AudioCategory.ACKNOWLEDGMENT
        }
    }
}
