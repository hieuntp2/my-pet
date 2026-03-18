package com.aipet.brain.brain.personality

data class PetPersonalitySummary(
    val label: String,
    val dominantTrait: String,
    val description: String
)

class PetPersonalitySummaryResolver {
    fun resolve(traits: PetTrait?): PetPersonalitySummary? {
        val current = traits ?: return null
        val centeredTraits = listOf(
            TraitScore("playful", current.playful - 0.5f),
            TraitScore("lazy", current.lazy - 0.5f),
            TraitScore("curious", current.curious - 0.5f),
            TraitScore("social", current.social - 0.5f)
        )
        val strongest = centeredTraits.maxByOrNull { it.score }
        if (strongest == null || strongest.score < DOMINANT_THRESHOLD) {
            return PetPersonalitySummary(
                label = "Balanced",
                dominantTrait = "balanced",
                description = "Showing a steady personality"
            )
        }
        return when (strongest.name) {
            "playful" -> PetPersonalitySummary(
                label = "Playful",
                dominantTrait = strongest.name,
                description = "Leans toward bright, energetic reactions"
            )

            "lazy" -> PetPersonalitySummary(
                label = "Calm",
                dominantTrait = strongest.name,
                description = "Prefers gentler, calmer reactions"
            )

            "curious" -> PetPersonalitySummary(
                label = "Curious",
                dominantTrait = strongest.name,
                description = "Leans toward curious, observant reactions"
            )

            else -> PetPersonalitySummary(
                label = "Affectionate",
                dominantTrait = strongest.name,
                description = "Warms up strongly to attention and care"
            )
        }
    }

    private data class TraitScore(
        val name: String,
        val score: Float
    )

    private companion object {
        const val DOMINANT_THRESHOLD = 0.10f
    }
}
