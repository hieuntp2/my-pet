package com.aipet.brain.brain.personality

data class PetTrait(
    val petId: String,
    // Original traits
    val playful: Float,
    val lazy: Float,
    val curious: Float,
    val social: Float,
    val updatedAt: Long,
    // v2 personality traits — added with sensible defaults so existing code still compiles
    val patience: Float = 0.5f,
    val attachment: Float = 0.3f,
    val energyProfile: Float = 0.5f  // 0 = very lethargic, 1 = very energetic baseline
) {
    init {
        require(petId.isNotBlank()) { "petId cannot be blank." }
        require(playful in TRAIT_MIN..TRAIT_MAX) { "playful must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(lazy in TRAIT_MIN..TRAIT_MAX) { "lazy must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(curious in TRAIT_MIN..TRAIT_MAX) { "curious must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(social in TRAIT_MIN..TRAIT_MAX) { "social must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(patience in TRAIT_MIN..TRAIT_MAX) { "patience must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(attachment in TRAIT_MIN..TRAIT_MAX) { "attachment must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(energyProfile in TRAIT_MIN..TRAIT_MAX) { "energyProfile must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(updatedAt > 0L) { "updatedAt must be greater than zero." }
    }

    /** Alias for social — used by scoring engine for sociability bias. */
    val sociability: Float get() = social

    companion object {
        const val TRAIT_MIN: Float = 0f
        const val TRAIT_MAX: Float = 1f
    }
}
