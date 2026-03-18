package com.aipet.brain.brain.behavior

data class PetBehaviorCandidate<T>(
    val behavior: T,
    val label: String,
    val baseWeight: Float,
    val adjustments: List<BehaviorWeight> = emptyList()
) {
    init {
        require(label.isNotBlank()) { "label cannot be blank." }
        require(baseWeight >= 0f) { "baseWeight must be non-negative." }
    }

    val totalWeight: Float
        get() = (baseWeight + adjustments.sumOf { it.delta.toDouble() }.toFloat())
            .coerceAtLeast(MIN_WEIGHT)

    companion object {
        private const val MIN_WEIGHT = 0.01f
    }
}
