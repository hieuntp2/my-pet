package com.aipet.brain.ui.avatar.pixel.selection

sealed interface PixelVariantSelectionStrategy {
    data class WeightedRandom(
        val nonRepeatingHistorySize: Int = DEFAULT_NON_REPEATING_HISTORY_SIZE
    ) : PixelVariantSelectionStrategy {
        init {
            require(nonRepeatingHistorySize in 0..PixelVariantSelectionContext.MAX_HISTORY_SIZE) {
                "nonRepeatingHistorySize must be between 0 and ${PixelVariantSelectionContext.MAX_HISTORY_SIZE}."
            }
        }

        companion object {
            private const val DEFAULT_NON_REPEATING_HISTORY_SIZE = 1
        }
    }

    data object SimpleRoundRobin : PixelVariantSelectionStrategy
}
