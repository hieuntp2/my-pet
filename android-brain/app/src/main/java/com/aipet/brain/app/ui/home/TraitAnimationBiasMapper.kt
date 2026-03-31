package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.personality.PetTrait

/**
 * Maps a [PetTrait] profile to animation category bias weights.
 *
 * Only traits above the threshold produce a meaningful bias so low trait values
 * don't distort animation selection.  The bias multiplier is already capped by
 * [com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector.MAX_BIAS_MULTIPLIER].
 */
object TraitAnimationBiasMapper {

    private const val TRAIT_THRESHOLD = 0.6f

    fun mapToCategories(trait: PetTrait?): Map<String, Float> {
        if (trait == null) return emptyMap()
        return buildMap {
            if (trait.playful > TRAIT_THRESHOLD) put("playful", 1f + trait.playful)
            if (trait.lazy > TRAIT_THRESHOLD) put("calm", 1f + trait.lazy)
            if (trait.curious > TRAIT_THRESHOLD) put("curious", 1f + trait.curious)
            if (trait.social > TRAIT_THRESHOLD) put("warm", 1f + trait.social)
        }
    }
}
