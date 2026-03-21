package com.aipet.brain.ui.avatar.pixel.model

data class PixelAnimationFrameEntry(
    val frame: PixelFrame64,
    val durationMillis: Int
) {
    init {
        require(durationMillis > 0) { "durationMillis must be greater than 0." }
    }
}

enum class PixelAnimationPlaybackMode {
    LOOP,
    ONE_SHOT
}

data class PixelAnimationClip(
    val id: String,
    val playbackMode: PixelAnimationPlaybackMode,
    val frames: List<PixelAnimationFrameEntry>
) {
    init {
        require(id.isNotBlank()) { "Pixel animation clip id must not be blank." }
        require(frames.isNotEmpty()) { "Pixel animation clip must include at least one frame." }
    }

    val totalDurationMillis: Int = frames.sumOf(PixelAnimationFrameEntry::durationMillis)
}

enum class PixelAnimationVariantTier {
    PRIMARY,
    COMMON,
    RARE
}

data class PixelAnimationVariant(
    val id: String,
    val clip: PixelAnimationClip,
    val tier: PixelAnimationVariantTier = PixelAnimationVariantTier.PRIMARY,
    val weight: Int = DEFAULT_WEIGHT,
    val categories: Set<String> = emptySet()
) {
    init {
        require(id.isNotBlank()) { "Pixel animation variant id must not be blank." }
        require(weight > 0) { "Pixel animation variant weight must be greater than 0." }
        require(categories.all { it.isNotBlank() }) { "Pixel animation variant categories must not contain blanks." }
    }

    companion object {
        private const val DEFAULT_WEIGHT = 1
    }
}

data class PixelPetAnimationStateSet(
    val state: PixelPetVisualState,
    val variants: List<PixelAnimationVariant>
) {
    init {
        require(variants.isNotEmpty()) { "Pixel pet animation state set must include at least one variant." }
    }

    val primaryVariants: List<PixelAnimationVariant> = variants.filter {
        it.tier == PixelAnimationVariantTier.PRIMARY
    }

    val rareVariants: List<PixelAnimationVariant> = variants.filter {
        it.tier == PixelAnimationVariantTier.RARE
    }
}

data class PixelPetAnimationCatalog(
    val states: Map<PixelPetVisualState, PixelPetAnimationStateSet>
) {
    init {
        require(states.isNotEmpty()) { "Pixel pet animation catalog must include at least one state set." }
        require(states.all { (state, stateSet) -> state == stateSet.state }) {
            "Pixel pet animation catalog keys must match each state set's declared state."
        }
    }

    operator fun get(state: PixelPetVisualState): PixelPetAnimationStateSet? = states[state]
}
