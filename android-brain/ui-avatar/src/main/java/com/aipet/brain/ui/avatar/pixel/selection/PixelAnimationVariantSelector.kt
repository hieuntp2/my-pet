package com.aipet.brain.ui.avatar.pixel.selection

import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelPetAnimationStateSet
import kotlin.random.Random

class PixelAnimationVariantSelector(
    private val strategy: PixelVariantSelectionStrategy = PixelVariantSelectionStrategy.WeightedRandom(),
    private val random: Random = Random.Default
) {
    fun selectNext(
        animationSet: PixelPetAnimationStateSet,
        context: PixelVariantSelectionContext
    ): PixelAnimationVariant {
        val candidates = resolveCandidates(
            animationSet = animationSet,
            context = context
        )
        val selectedVariant = when (strategy) {
            is PixelVariantSelectionStrategy.WeightedRandom -> weightedRandomSelection(candidates)
            PixelVariantSelectionStrategy.SimpleRoundRobin -> roundRobinSelection(
                animationSet = animationSet,
                context = context,
                candidates = candidates
            )
        }

        context.recordSelection(selectedVariant.id)
        return selectedVariant
    }

    private fun resolveCandidates(
        animationSet: PixelPetAnimationStateSet,
        context: PixelVariantSelectionContext
    ): List<PixelAnimationVariant> {
        val variants = animationSet.variants
        val recentHistorySize = when (strategy) {
            is PixelVariantSelectionStrategy.WeightedRandom -> strategy.nonRepeatingHistorySize
            PixelVariantSelectionStrategy.SimpleRoundRobin -> 0
        }
        if (recentHistorySize == 0 || variants.size <= SINGLE_VARIANT_COUNT) {
            return variants
        }

        val recentHistoryIds = context.recentVariantIds(limit = recentHistorySize).toSet()
        val filteredByHistory = variants.filterNot { it.id in recentHistoryIds }
        if (filteredByHistory.isNotEmpty()) {
            return filteredByHistory
        }

        val lastVariantId = context.lastVariantId
        val filteredByLastVariant = variants.filterNot { it.id == lastVariantId }
        return filteredByLastVariant.ifEmpty { variants }
    }

    private fun weightedRandomSelection(
        candidates: List<PixelAnimationVariant>
    ): PixelAnimationVariant {
        val totalWeight = candidates.sumOf { it.weight }
        var remainingWeight = random.nextInt(totalWeight)

        for (variant in candidates) {
            remainingWeight -= variant.weight
            if (remainingWeight < 0) {
                return variant
            }
        }

        return candidates.last()
    }

    private fun roundRobinSelection(
        animationSet: PixelPetAnimationStateSet,
        context: PixelVariantSelectionContext,
        candidates: List<PixelAnimationVariant>
    ): PixelAnimationVariant {
        val lastVariantId = context.lastVariantId ?: return candidates.first()
        val lastVariantIndex = animationSet.variants.indexOfFirst { it.id == lastVariantId }
        if (lastVariantIndex < 0) {
            return candidates.first()
        }

        for (step in 1..animationSet.variants.size) {
            val nextIndex = (lastVariantIndex + step) % animationSet.variants.size
            val nextVariant = animationSet.variants[nextIndex]
            if (nextVariant in candidates) {
                return nextVariant
            }
        }

        return candidates.first()
    }

    private companion object {
        private const val SINGLE_VARIANT_COUNT = 1
    }
}
