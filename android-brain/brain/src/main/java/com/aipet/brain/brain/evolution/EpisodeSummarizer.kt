package com.aipet.brain.brain.evolution

/**
 * Generates human-readable summary text from an episode candidate.
 * Rule-based only — no cloud AI, no LLM.
 */
class EpisodeSummarizer {

    fun summarize(candidate: EpisodeCandidate, durationMs: Long): String {
        val duration = formatDuration(durationMs)
        val reunionLabel = formatReunionType(candidate.reunionType)
        val interactionLabel = formatInteractions(candidate.interactionCount, candidate.interactionTypes)
        val moodLabel = formatMoodSignal(candidate)
        val neglectSuffix = if (candidate.hadNeglectSignal) " (neglect signal)" else ""
        val careLabel = formatCareChange(candidate.careScoreDelta)
        return buildString {
            append(reunionLabel)
            append(". ")
            append(duration)
            append(" session")
            if (interactionLabel.isNotEmpty()) {
                append(" with ")
                append(interactionLabel)
            }
            append(". ")
            append(moodLabel)
            if (careLabel.isNotEmpty()) {
                append(". ")
                append(careLabel)
            }
            append(neglectSuffix)
            append(".")
        }
    }

    private fun formatDuration(durationMs: Long): String = when {
        durationMs < 60_000L -> "Brief (under 1 min)"
        durationMs < 5 * 60_000L -> "Short (${durationMs / 60_000L} min)"
        durationMs < 20 * 60_000L -> "Moderate (${durationMs / 60_000L} min)"
        else -> "Long (${durationMs / 60_000L} min)"
    }

    private fun formatReunionType(reunionType: String): String = when (reunionType) {
        "QUICK_RETURN" -> "Quick return"
        "ROUTINE_RETURN" -> "Routine visit"
        "LONG_ABSENCE" -> "Returned after a long absence"
        "RECOVERY_RETURN" -> "First session after neglect period"
        "MISSED_EXPECTED" -> "Late — missed usual window"
        else -> "App opened"
    }

    private fun formatInteractions(count: Int, types: List<String>): String {
        if (count == 0) return ""
        val uniqueTypes = types.distinct().take(3)
        return if (uniqueTypes.isEmpty()) "$count interaction(s)"
        else "${uniqueTypes.joinToString(", ").lowercase()} ($count interaction(s))"
    }

    private fun formatMoodSignal(candidate: EpisodeCandidate): String {
        val emotion = candidate.dominantEmotion().lowercase()
        return "Dominant mood: $emotion"
    }

    private fun formatCareChange(delta: Int): String = when {
        delta > 10 -> "Good care quality"
        delta > 0 -> "Mild care"
        delta < -10 -> "Poor care quality"
        delta < 0 -> "Care slightly low"
        else -> ""
    }
}
