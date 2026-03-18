package com.aipet.brain.brain.pet

data class PetState(
    val mood: PetMood,
    val energy: Int,
    val hunger: Int,
    val sleepiness: Int,
    val social: Int,
    val bond: Int,
    val lastUpdatedAt: Long
) {
    init {
        require(energy in VALUE_MIN..VALUE_MAX) { "energy must be between $VALUE_MIN and $VALUE_MAX." }
        require(hunger in VALUE_MIN..VALUE_MAX) { "hunger must be between $VALUE_MIN and $VALUE_MAX." }
        require(sleepiness in VALUE_MIN..VALUE_MAX) { "sleepiness must be between $VALUE_MIN and $VALUE_MAX." }
        require(social in VALUE_MIN..VALUE_MAX) { "social must be between $VALUE_MIN and $VALUE_MAX." }
        require(bond in VALUE_MIN..VALUE_MAX) { "bond must be between $VALUE_MIN and $VALUE_MAX." }
        require(lastUpdatedAt > 0L) { "lastUpdatedAt must be greater than zero." }
    }

    fun withClampedValues(lastUpdatedAt: Long = this.lastUpdatedAt): PetState {
        return copy(
            energy = energy.coerceIn(VALUE_MIN, VALUE_MAX),
            hunger = hunger.coerceIn(VALUE_MIN, VALUE_MAX),
            sleepiness = sleepiness.coerceIn(VALUE_MIN, VALUE_MAX),
            social = social.coerceIn(VALUE_MIN, VALUE_MAX),
            bond = bond.coerceIn(VALUE_MIN, VALUE_MAX),
            lastUpdatedAt = lastUpdatedAt
        )
    }

    companion object {
        const val VALUE_MIN: Int = 0
        const val VALUE_MAX: Int = 100
    }
}
