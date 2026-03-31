package com.aipet.brain.brain.pet

/**
 * Determines whether a care action constitutes a repair event and how much
 * recovery to applied to the relationship state.
 *
 * Recovery is most effective when:
 * - the pet is currently hesitant/distant (low trust, neglect streak > 0)
 * - the action is intentional (soothe, feed when hungry, long-press)
 * - the care streak hasn't already been boosted this session
 *
 * Recovery is emotionally visible: it should show as RELIEVED or similar.
 */
class RepairEffectResolver {

    data class RepairResult(
        val isRepair: Boolean,
        val trustGainBonus: Int,
        val careStreakGain: Int,
        val neglectStreakDrop: Int,
        val shouldShowReliefEmotion: Boolean
    )

    fun resolve(action: CareActionType, stateBefore: PetState): RepairResult {
        val isDistressed = stateBefore.trustScore <= 30 || stateBefore.neglectStreak >= 2
        val isDistant = stateBefore.neglectStreak >= 3

        return when {
            // Soothe is the primary repair action — always counts
            action == CareActionType.SOOTHE -> RepairResult(
                isRepair = true,
                trustGainBonus = if (isDistressed) 5 else 2,
                careStreakGain = if (stateBefore.careStreak < 10) 1 else 0,
                neglectStreakDrop = if (stateBefore.neglectStreak > 0) 1 else 0,
                shouldShowReliefEmotion = true
            )

            // Feeding a genuinely hungry pet is a care signal
            action == CareActionType.FEED && stateBefore.hunger >= 60 -> RepairResult(
                isRepair = true,
                trustGainBonus = if (isDistressed) 3 else 1,
                careStreakGain = if (stateBefore.careStreak < 10) 1 else 0,
                neglectStreakDrop = 0,
                shouldShowReliefEmotion = isDistressed
            )

            // Long press when the pet is emotionally fragile acts as comfort
            action == CareActionType.LONG_PRESS && stateBefore.trustScore <= 35 -> RepairResult(
                isRepair = true,
                trustGainBonus = if (isDistant) 2 else 1,
                careStreakGain = 0,
                neglectStreakDrop = 0,
                shouldShowReliefEmotion = isDistant
            )

            // Linger during a distant state is a soft but real repair signal
            action == CareActionType.LINGER && isDistant -> RepairResult(
                isRepair = true,
                trustGainBonus = 1,
                careStreakGain = 0,
                neglectStreakDrop = 0,
                shouldShowReliefEmotion = false
            )

            else -> RepairResult(
                isRepair = false,
                trustGainBonus = 0,
                careStreakGain = 0,
                neglectStreakDrop = 0,
                shouldShowReliefEmotion = false
            )
        }
    }
}
