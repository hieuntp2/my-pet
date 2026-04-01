package com.aipet.brain.brain.evolution

/**
 * Explainable component breakdown for behavior scoring.
 * Every scored decision exposes these individual contributions.
 */
data class BehaviorScoreComponents(
    val stateWeight: Float,
    val relationshipWeight: Float,
    val memoryWeight: Float,
    val habitWeight: Float,
    val personalityWeight: Float,
    val lifecycleWeight: Float,
    val variationNoise: Float,
    val suppressionPenalty: Float,
    val cooldownPenalty: Float
) {
    val total: Float get() =
        stateWeight + relationshipWeight + memoryWeight + habitWeight +
        personalityWeight + lifecycleWeight + variationNoise -
        suppressionPenalty - cooldownPenalty

    fun describe(): String = buildString {
        append("state=%.2f".format(stateWeight))
        append(" rel=%.2f".format(relationshipWeight))
        append(" mem=%.2f".format(memoryWeight))
        append(" habit=%.2f".format(habitWeight))
        append(" personality=%.2f".format(personalityWeight))
        append(" lifecycle=%.2f".format(lifecycleWeight))
        append(" noise=%.2f".format(variationNoise))
        append(" sup=-%.2f".format(suppressionPenalty))
        append(" cd=-%.2f".format(cooldownPenalty))
        append(" total=%.2f".format(total))
    }
}
