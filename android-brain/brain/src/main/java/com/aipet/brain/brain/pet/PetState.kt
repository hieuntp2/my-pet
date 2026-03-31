package com.aipet.brain.brain.pet

data class PetState(
    // --- Core needs (Layer A) ---
    val mood: PetMood,
    val energy: Int,
    val hunger: Int,
    val sleepiness: Int,
    val social: Int,           // socialNeed: how much social contact the pet craves
    val comfort: Int,          // emotional safety / settled-ness (slower decay)
    val stimulation: Int,      // engagement / boredom axis (faster decay)
    // --- Mood climate (Layer B) ---
    val moodValence: Int,      // -100..100: emotional weather from recent history
    val moodArousal: Int,      // 0..100: activation level of the mood climate
    // --- Relationship (Layer D) ---
    val bond: Int,             // long-term emotional closeness (hard to build, slow to lose)
    val trustScore: Int,       // how safe the pet feels right now (fragile)
    val attachmentScore: Int,  // how strongly the pet seeks reunion
    val neglectStreak: Int,    // count of neglect episodes (bounded, not a punishment counter)
    val careStreak: Int,       // count of healthy care sessions (recent pattern)
    // --- Timestamps ---
    val lastUpdatedAt: Long,
    val lastOpenAt: Long,                    // last time app was opened (0 = never recorded)
    val lastMeaningfulInteractionAt: Long    // last feed/soothe/play/long-hold (0 = never)
) {
    init {
        require(energy in VALUE_MIN..VALUE_MAX) { "energy $energy out of range." }
        require(hunger in VALUE_MIN..VALUE_MAX) { "hunger $hunger out of range." }
        require(sleepiness in VALUE_MIN..VALUE_MAX) { "sleepiness $sleepiness out of range." }
        require(social in VALUE_MIN..VALUE_MAX) { "social $social out of range." }
        require(comfort in VALUE_MIN..VALUE_MAX) { "comfort $comfort out of range." }
        require(stimulation in VALUE_MIN..VALUE_MAX) { "stimulation $stimulation out of range." }
        require(moodValence in VALENCE_MIN..VALENCE_MAX) { "moodValence $moodValence out of range." }
        require(moodArousal in VALUE_MIN..VALUE_MAX) { "moodArousal $moodArousal out of range." }
        require(bond in VALUE_MIN..VALUE_MAX) { "bond $bond out of range." }
        require(trustScore in VALUE_MIN..VALUE_MAX) { "trustScore $trustScore out of range." }
        require(attachmentScore in VALUE_MIN..VALUE_MAX) { "attachmentScore $attachmentScore out of range." }
        require(neglectStreak >= 0) { "neglectStreak $neglectStreak must be >= 0." }
        require(careStreak >= 0) { "careStreak $careStreak must be >= 0." }
        require(lastUpdatedAt > 0L) { "lastUpdatedAt must be greater than zero." }
    }

    fun withClampedValues(lastUpdatedAt: Long = this.lastUpdatedAt): PetState {
        return copy(
            energy = energy.coerceIn(VALUE_MIN, VALUE_MAX),
            hunger = hunger.coerceIn(VALUE_MIN, VALUE_MAX),
            sleepiness = sleepiness.coerceIn(VALUE_MIN, VALUE_MAX),
            social = social.coerceIn(VALUE_MIN, VALUE_MAX),
            comfort = comfort.coerceIn(VALUE_MIN, VALUE_MAX),
            stimulation = stimulation.coerceIn(VALUE_MIN, VALUE_MAX),
            moodValence = moodValence.coerceIn(VALENCE_MIN, VALENCE_MAX),
            moodArousal = moodArousal.coerceIn(VALUE_MIN, VALUE_MAX),
            bond = bond.coerceIn(VALUE_MIN, VALUE_MAX),
            trustScore = trustScore.coerceIn(VALUE_MIN, VALUE_MAX),
            attachmentScore = attachmentScore.coerceIn(VALUE_MIN, VALUE_MAX),
            neglectStreak = neglectStreak.coerceAtLeast(0),
            careStreak = careStreak.coerceAtLeast(0),
            lastUpdatedAt = lastUpdatedAt
        )
    }

    companion object {
        const val VALUE_MIN: Int = 0
        const val VALUE_MAX: Int = 100
        const val VALENCE_MIN: Int = -100
        const val VALENCE_MAX: Int = 100
        const val NEGLECT_STREAK_MAX: Int = 10
    }
}
