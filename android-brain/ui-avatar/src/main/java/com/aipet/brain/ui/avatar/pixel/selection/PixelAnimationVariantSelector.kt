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
        context: PixelVariantSelectionContext,
        categoryBias: Map<String, Float> = emptyMap()
    ): PixelAnimationVariant {
        val candidates = resolveCandidates(
            animationSet = animationSet,
            context = context
        )
        val selectedVariant = when (strategy) {
            is PixelVariantSelectionStrategy.WeightedRandom -> weightedRandomSelection(
                candidates = candidates,
                categoryBias = categoryBias
            )
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
        candidates: List<PixelAnimationVariant>,
        categoryBias: Map<String, Float> = emptyMap()
    ): PixelAnimationVariant {
        // Apply category bias: if a variant's categories intersect with bias keys,
        // scale its effective weight up by the bias multiplier (capped at MAX_BIAS_MULTIPLIER).
        val effectiveWeights = if (categoryBias.isEmpty()) {
            candidates.map { it.weight }
        } else {
            candidates.map { variant ->
                val multiplier = variant.categories
                    .mapNotNull { cat -> categoryBias[cat] }
                    .maxOrNull()
                    ?.coerceIn(1f, MAX_BIAS_MULTIPLIER)
                    ?: 1f
                (variant.weight * multiplier).toInt().coerceAtLeast(1)
            }
        }
        val totalWeight = effectiveWeights.sum()
        var remainingWeight = random.nextInt(totalWeight)

        for ((variant, weight) in candidates.zip(effectiveWeights)) {
            remainingWeight -= weight
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

    companion object {
        /** Maximum multiplier applied by category bias to any single variant's weight. */
        const val MAX_BIAS_MULTIPLIER: Float = 2.0f
        private const val SINGLE_VARIANT_COUNT = 1
    }
}
