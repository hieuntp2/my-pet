package com.aipet.brain.brain.personality

data class PetTrait(
    val petId: String,
    val playful: Float,
    val lazy: Float,
    val curious: Float,
    val social: Float,
    val updatedAt: Long
) {
    init {
        require(petId.isNotBlank()) { "petId cannot be blank." }
        require(playful in TRAIT_MIN..TRAIT_MAX) { "playful must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(lazy in TRAIT_MIN..TRAIT_MAX) { "lazy must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(curious in TRAIT_MIN..TRAIT_MAX) { "curious must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(social in TRAIT_MIN..TRAIT_MAX) { "social must be between $TRAIT_MIN and $TRAIT_MAX." }
        require(updatedAt > 0L) { "updatedAt must be greater than zero." }
    }

    companion object {
        const val TRAIT_MIN: Float = 0f
        const val TRAIT_MAX: Float = 1f
    }
}
