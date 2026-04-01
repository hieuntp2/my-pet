package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait

/**
 * Processes user care actions into real, persisted state changes.
 *
 * Each action produces:
 * 1. Visible emotion change
 * 2. Persisted state delta (comfort, trust, social, etc.)
 * 3. Anti-spam/diminishing returns feedback
 *
 * Design rule: no single action should feel magically powerful. Effects are
 * small, contextual, and slightly modified by personality traits.
 */
class CareActionProcessor(
    private val burstTracker: BurstInteractionTracker = BurstInteractionTracker()
) {

    fun process(
        action: CareActionType,
        state: PetState,
        traits: PetTrait?,
        now: Long
    ): CareActionResult {
        val burstCount = when (action) {
            CareActionType.TAP, CareActionType.LONG_PRESS -> burstTracker.record(now)
            else -> burstTracker.currentBurstCount(now)
        }

        val diminishing = burstCount > PetEmotionalConfig.BURST_COUNT_DIMINISH_START
        val isRepairAction = action == CareActionType.SOOTHE ||
                (action == CareActionType.FEED && state.hunger >= 60) ||
                (action == CareActionType.LONG_PRESS && state.trustScore <= 30) ||
                (action == CareActionType.REST &&
                    (state.moodArousal >= 65 || state.stimulation >= 70))

        val newState = when (action) {
            CareActionType.TAP -> applyTap(state, traits, diminishing, burstCount)
            CareActionType.LONG_PRESS -> applyLongPress(state, traits, diminishing)
            CareActionType.SOOTHE -> applySoothe(state, traits)
            CareActionType.PLAY -> applyPlay(state, traits)
            CareActionType.FEED -> applyFeed(state, traits)
            CareActionType.REST -> applyRest(state)
            CareActionType.LINGER -> applyLinger(state)
        }.let {
            it.withClampedValues(lastUpdatedAt = now).copy(
                lastMeaningfulInteractionAt = if (isMeaningfulAction(action)) now else state.lastMeaningfulInteractionAt
            )
        }

        val resultingEmotion = resolveResultEmotion(action, newState, isRepairAction, diminishing)

        return CareActionResult(
            actionType = action,
            stateBefore = state,
            stateAfter = newState,
            resultingEmotion = resultingEmotion,
            didDiminishingReturnsApply = diminishing,
            isRepairAction = isRepairAction,
            burstCount = burstCount
        )
    }

    private fun applyTap(state: PetState, traits: PetTrait?, diminishing: Boolean, burstCount: Int): PetState {
        // Patience trait reduces overstimulation from bursts
        val patienceFactor = if (traits != null) (0.5f + traits.patience * 0.5f) else 0.7f
        val overstimRisk = burstCount > (PetEmotionalConfig.BURST_COUNT_DIMINISH_START * patienceFactor)

        val socialGain = if (diminishing) PetEmotionalConfig.TAP_SOCIAL_GAIN / 2 else PetEmotionalConfig.TAP_SOCIAL_GAIN
        val comfortGain = if (overstimRisk) -PetEmotionalConfig.BURST_COMFORT_COST_PER_EXCESS else PetEmotionalConfig.TAP_COMFORT_GAIN
        val valenceGain = if (diminishing) PetEmotionalConfig.TAP_VALENCE_GAIN / 2 else PetEmotionalConfig.TAP_VALENCE_GAIN
        val bondGain = if (diminishing) 0 else PetEmotionalConfig.TAP_BOND_GAIN

        val stimGain = if (burstCount <= 3) 8 else if (burstCount <= 6) 4 else 1

        return state.copy(
            social = state.social + socialGain,
            comfort = state.comfort + comfortGain,
            moodValence = state.moodValence + valenceGain,
            bond = state.bond + bondGain,
            stimulation = (state.stimulation + stimGain).coerceAtMost(PetEmotionalConfig.BURST_MAX_STIMULATION_FROM_TAP),
            careStreak = if (!diminishing && state.neglectStreak == 0) state.careStreak else state.careStreak
        )
    }

    private fun applyLongPress(state: PetState, traits: PetTrait?, diminishing: Boolean): PetState {
        // Long press is more calming — works when pet is receptive
        val isReceptive = state.stimulation <= 70 && state.energy >= 20
        if (!isReceptive) {
            // Overstimulated or exhausted pet doesn't enjoy being held
            return state.copy(
                comfort = state.comfort - 2,
                stimulation = state.stimulation + 3
            )
        }

        val attachmentBonus = if (traits != null) (traits.attachment * 4).toInt() else 2

        return state.copy(
            comfort = state.comfort + PetEmotionalConfig.HOLD_COMFORT_GAIN,
            trustScore = state.trustScore + PetEmotionalConfig.HOLD_TRUST_GAIN + attachmentBonus,
            social = state.social + PetEmotionalConfig.HOLD_SOCIAL_GAIN,
            moodArousal = (state.moodArousal - PetEmotionalConfig.HOLD_AROUSAL_DROP).coerceAtLeast(0),
            bond = state.bond + if (!diminishing) 1 else 0
        )
    }

    private fun applySoothe(state: PetState, traits: PetTrait?): PetState {
        val attachmentBonus = if (traits != null) (traits.attachment * 5).toInt() else 2
        val sootheEffectiveness = if (state.trustScore < 30) 0.6f else 1.0f

        val comfortGain = (PetEmotionalConfig.SOOTHE_COMFORT_GAIN * sootheEffectiveness).toInt()
        val trustGain = (PetEmotionalConfig.SOOTHE_TRUST_GAIN * sootheEffectiveness).toInt() + attachmentBonus
        val arousalDrop = PetEmotionalConfig.SOOTHE_AROUSAL_DROP
        val valenceGain = (PetEmotionalConfig.SOOTHE_VALENCE_GAIN * sootheEffectiveness).toInt()
        val neglectDrop = if (state.neglectStreak > 0) PetEmotionalConfig.SOOTHE_NEGLECT_STREAK_DROP else 0
        val careGain = if (state.careStreak < 10) 1 else 0

        return state.copy(
            comfort = state.comfort + comfortGain,
            trustScore = state.trustScore + trustGain,
            moodArousal = (state.moodArousal - arousalDrop).coerceAtLeast(0),
            moodValence = state.moodValence + valenceGain,
            neglectStreak = (state.neglectStreak - neglectDrop).coerceAtLeast(0),
            careStreak = state.careStreak + careGain
        )
    }

    private fun applyPlay(state: PetState, traits: PetTrait?): PetState {
        // Play has lower effect when very sleepy
        if (state.sleepiness >= 75 || state.energy <= 20) {
            return state.copy(
                stimulation = state.stimulation + 5,
                social = state.social + 3
            )
        }

        val playfulBonus = if (traits != null) (traits.playful * 8).toInt() else 3
        val socialBonus = if (traits != null) (traits.sociability * 4).toInt() else 2
        val careGain = if (state.careStreak < 10) 1 else 0

        return state.copy(
            stimulation = state.stimulation + PetEmotionalConfig.PLAY_STIMULATION_GAIN + playfulBonus,
            social = state.social + PetEmotionalConfig.PLAY_SOCIAL_GAIN + socialBonus,
            moodValence = state.moodValence + PetEmotionalConfig.PLAY_VALENCE_GAIN,
            energy = state.energy - PetEmotionalConfig.PLAY_ENERGY_COST,
            bond = state.bond + PetEmotionalConfig.PLAY_BOND_GAIN,
            careStreak = state.careStreak + careGain
        )
    }

    private fun applyFeed(state: PetState, traits: PetTrait?): PetState {
        // Feed is much more powerful when the pet is actually hungry
        val hungerRelevant = state.hunger >= 50
        val comfortGain = if (hungerRelevant) PetEmotionalConfig.FEED_COMFORT_GAIN else PetEmotionalConfig.FEED_COMFORT_GAIN / 2
        val trustGain = if (hungerRelevant) PetEmotionalConfig.FEED_TRUST_GAIN else PetEmotionalConfig.FEED_TRUST_GAIN / 2
        val valenceGain = if (hungerRelevant) PetEmotionalConfig.FEED_VALENCE_GAIN else PetEmotionalConfig.FEED_VALENCE_GAIN / 2
        // Social/attachment personality makes feeding a bonding moment
        val attachmentBonus = if (traits != null) (traits.attachment * 3).toInt() else 0
        val careGain = if (state.careStreak < 10) 1 else 0

        return state.copy(
            hunger = (state.hunger - PetEmotionalConfig.FEED_HUNGER_DROP).coerceAtLeast(0),
            comfort = state.comfort + comfortGain + attachmentBonus,
            trustScore = state.trustScore + trustGain,
            moodValence = state.moodValence + valenceGain,
            careStreak = state.careStreak + careGain
        )
    }

    private fun applyRest(state: PetState): PetState {
        val careGain = if (state.careStreak < 10) 1 else 0
        return state.copy(
            comfort = state.comfort + PetEmotionalConfig.REST_COMFORT_GAIN,
            trustScore = state.trustScore + PetEmotionalConfig.REST_TRUST_GAIN,
            moodArousal = (state.moodArousal - PetEmotionalConfig.REST_AROUSAL_DROP).coerceAtLeast(0),
            stimulation = (state.stimulation - PetEmotionalConfig.REST_STIMULATION_DROP).coerceAtLeast(0),
            careStreak = state.careStreak + careGain
        )
    }

    private fun applyLinger(state: PetState): PetState {
        return state.copy(
            comfort = state.comfort + PetEmotionalConfig.LINGER_COMFORT_GAIN,
            trustScore = state.trustScore + PetEmotionalConfig.LINGER_TRUST_GAIN
        )
    }

    private fun isMeaningfulAction(action: CareActionType): Boolean {
        return action == CareActionType.SOOTHE ||
                action == CareActionType.FEED ||
                action == CareActionType.PLAY ||
                action == CareActionType.REST ||
                action == CareActionType.LONG_PRESS
    }

    private fun resolveResultEmotion(
        action: CareActionType,
        newState: PetState,
        isRepairAction: Boolean,
        diminishing: Boolean
    ): PetEmotion {
        if (isRepairAction) return PetEmotion.RELIEVED

        return when (action) {
            CareActionType.SOOTHE -> PetEmotion.RELIEVED
            CareActionType.FEED -> if (newState.hunger <= 30) PetEmotion.HAPPY else PetEmotion.CURIOUS
            CareActionType.PLAY -> if (newState.energy >= 40) PetEmotion.EXCITED else PetEmotion.HAPPY
            CareActionType.REST -> if (newState.sleepiness >= 65) PetEmotion.SLEEPY else PetEmotion.IDLE
            CareActionType.LONG_PRESS -> if (newState.comfort >= 70) PetEmotion.IDLE else PetEmotion.SHY
            CareActionType.TAP -> if (diminishing) PetEmotion.WITHDRAWN else PetEmotion.HAPPY
            CareActionType.LINGER -> PetEmotion.IDLE
        }
    }
}
