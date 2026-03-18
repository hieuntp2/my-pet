package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.personality.PetPersonalitySummaryResolver
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetState

data class HomeUiModel(
    val petName: String,
    val identityLine: String,
    val statusLine: String,
    val moodLabel: String,
    val personalityLabel: String?,
    val todaySummary: HomeTodaySummary?,
    val indicators: List<HomeStateIndicator>
)

data class HomeStateIndicator(
    val label: String,
    val value: String,
    val progress: Float
)

object HomeUiModelBuilder {
    private val personalitySummaryResolver = PetPersonalitySummaryResolver()

    fun build(
        petName: String,
        petState: PetState?,
        emotion: PetEmotion,
        conditions: Set<PetCondition>,
        traits: PetTrait? = null,
        todaySummary: HomeTodaySummary? = null
    ): HomeUiModel {
        val safeName = petName.ifBlank { "Pet" }
        val state = petState
        val moodLabel = emotion.toMoodLabel()
        val personalityLabel = personalitySummaryResolver.resolve(traits)?.label
        if (state == null) {
            return HomeUiModel(
                petName = safeName,
                identityLine = "Your digital pet companion",
                statusLine = "$safeName is waking up for today.",
                moodLabel = moodLabel,
                personalityLabel = personalityLabel,
                todaySummary = todaySummary,
                indicators = listOf(
                    HomeStateIndicator(label = "Energy", value = "Starting up", progress = 0.5f),
                    HomeStateIndicator(label = "Hunger", value = "Unknown", progress = 0.3f),
                    HomeStateIndicator(label = "Company", value = "Unknown", progress = 0.5f)
                )
            )
        }
        return HomeUiModel(
            petName = safeName,
            identityLine = "Your digital pet companion",
            statusLine = statusFor(
                petName = safeName,
                emotion = emotion,
                conditions = conditions
            ),
            moodLabel = moodLabel,
            personalityLabel = personalityLabel,
            todaySummary = todaySummary,
            indicators = listOf(
                HomeStateIndicator(
                    label = "Energy",
                    value = state.energy.toEnergyLabel(),
                    progress = state.energy.toProgress()
                ),
                HomeStateIndicator(
                    label = "Hunger",
                    value = state.hunger.toHungerLabel(),
                    progress = state.hunger.toProgress()
                ),
                HomeStateIndicator(
                    label = "Company",
                    value = state.social.toCompanyLabel(),
                    progress = (100 - state.social).toProgress()
                )
            )
        )
    }

    private fun statusFor(
        petName: String,
        emotion: PetEmotion,
        conditions: Set<PetCondition>
    ): String {
        return when {
            conditions.contains(PetCondition.HUNGRY) -> "$petName is getting hungry."
            conditions.contains(PetCondition.SLEEPY) -> "$petName seems sleepy."
            conditions.contains(PetCondition.LONELY) -> "$petName wants your company."
            conditions.contains(PetCondition.PLAYFUL) || emotion == PetEmotion.EXCITED -> "$petName looks playful today."
            emotion == PetEmotion.CURIOUS -> "$petName seems curious right now."
            emotion == PetEmotion.HAPPY -> "$petName looks happy to see you."
            emotion == PetEmotion.SAD -> "$petName needs a little comfort."
            else -> "$petName feels calm right now."
        }
    }

    private fun PetEmotion.toMoodLabel(): String {
        return when (this) {
            PetEmotion.IDLE -> "Calm"
            PetEmotion.HAPPY -> "Happy"
            PetEmotion.CURIOUS -> "Curious"
            PetEmotion.SLEEPY -> "Sleepy"
            PetEmotion.SAD -> "Needs comfort"
            PetEmotion.EXCITED -> "Playful"
            PetEmotion.HUNGRY -> "Hungry"
        }
    }

    private fun Int.toEnergyLabel(): String {
        return when {
            this >= 70 -> "High"
            this >= 35 -> "Steady"
            else -> "Low"
        }
    }

    private fun Int.toHungerLabel(): String {
        return when {
            this >= 70 -> "Hungry"
            this >= 40 -> "Peckish"
            else -> "Satisfied"
        }
    }

    private fun Int.toCompanyLabel(): String {
        return when {
            this >= 65 -> "Content"
            this >= 35 -> "Wants time together"
            else -> "Needs attention"
        }
    }

    private fun Int.toProgress(): Float {
        return (this.coerceIn(0, 100) / 100f)
    }
}
