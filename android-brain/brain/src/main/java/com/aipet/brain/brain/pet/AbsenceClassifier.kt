package com.aipet.brain.brain.pet

/**
 * Classifies a user's return context into an absence bucket.
 * Long absence alone does not equal neglect — relationship state matters too.
 */
class AbsenceClassifier {

    fun classify(state: PetState, now: Long): AbsenceBucket {
        val lastOpen = state.lastOpenAt
        if (lastOpen <= 0L) {
            // First time opening — treat as short return (no prior context)
            return AbsenceBucket.SHORT_RETURN
        }

        val elapsedMs = now - lastOpen
        return when {
            elapsedMs < PetEmotionalConfig.ABSENCE_SHORT_MAX_MS -> AbsenceBucket.SHORT_RETURN

            elapsedMs < PetEmotionalConfig.ABSENCE_MEDIUM_MAX_MS -> AbsenceBucket.MEDIUM_RETURN

            elapsedMs < PetEmotionalConfig.ABSENCE_LONG_MAX_MS -> {
                // 3h–24h: if neglect streak is significant, classify as neglect
                if (state.neglectStreak >= PetEmotionalConfig.NEGLECT_RETURN_STREAK_THRESHOLD) {
                    AbsenceBucket.NEGLECT_RETURN
                } else {
                    AbsenceBucket.LONG_RETURN
                }
            }

            else -> {
                // > 24h is always at least LONG_RETURN; with neglect streak — NEGLECT_RETURN
                if (state.neglectStreak >= PetEmotionalConfig.NEGLECT_RETURN_STREAK_THRESHOLD) {
                    AbsenceBucket.NEGLECT_RETURN
                } else {
                    AbsenceBucket.LONG_RETURN
                }
            }
        }
    }
}
