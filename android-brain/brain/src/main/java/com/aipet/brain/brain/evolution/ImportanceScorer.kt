package com.aipet.brain.brain.evolution

/**
 * Scores how important an episode is for memory retrieval and behavior influence.
 *
 * Importance is boosted by:
 * - long absences (reunion weight)
 * - recovery after neglect
 * - high care intensity
 * - large emotional state changes
 * - high interaction count
 *
 * Routine low-signal sessions get low scores to prevent memory inflation.
 */
class ImportanceScorer {

    fun score(candidate: EpisodeCandidate, durationMs: Long): Float {
        var score = BASE_SCORE

        // Long absence makes the session more memorable
        val reunionBoost = when (candidate.reunionType) {
            "LONG_ABSENCE" -> 0.35f
            "RECOVERY_RETURN" -> 0.3f
            "MISSED_EXPECTED" -> 0.15f
            "ROUTINE_RETURN" -> 0.05f
            else -> 0f
        }
        score += reunionBoost

        // Neglect signal is significant
        if (candidate.hadNeglectSignal) score += 0.2f

        // Strong positive care is significant
        score += when {
            candidate.careScoreDelta > 15 -> 0.2f
            candidate.careScoreDelta > 5 -> 0.1f
            candidate.careScoreDelta < -15 -> 0.15f
            else -> 0f
        }

        // Bond delta
        score += when {
            candidate.bondDelta > 5 -> 0.1f
            candidate.bondDelta < -5 -> 0.1f
            else -> 0f
        }

        // High interaction density
        score += when {
            candidate.interactionCount >= 10 -> 0.15f
            candidate.interactionCount >= 5 -> 0.08f
            candidate.interactionCount >= 2 -> 0.03f
            else -> 0f
        }

        // Long sessions
        score += when {
            durationMs > 20 * 60_000L -> 0.1f
            durationMs > 10 * 60_000L -> 0.05f
            else -> 0f
        }

        return score.coerceIn(0f, 1f)
    }

    private companion object {
        const val BASE_SCORE = 0.1f
    }
}
