package com.aipet.brain.memory.pet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

@Entity(tableName = "pet_state")
data class PetStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "mood")
    val mood: String,
    @ColumnInfo(name = "energy")
    val energy: Int,
    @ColumnInfo(name = "hunger")
    val hunger: Int,
    @ColumnInfo(name = "sleepiness")
    val sleepiness: Int,
    @ColumnInfo(name = "social")
    val social: Int,
    @ColumnInfo(name = "bond")
    val bond: Int,
    @ColumnInfo(name = "last_updated_at")
    val lastUpdatedAt: Long,
    // v2 fields added in migration 20→21
    @ColumnInfo(name = "comfort")
    val comfort: Int = 70,
    @ColumnInfo(name = "stimulation")
    val stimulation: Int = 30,
    @ColumnInfo(name = "mood_valence")
    val moodValence: Int = 0,
    @ColumnInfo(name = "mood_arousal")
    val moodArousal: Int = 50,
    @ColumnInfo(name = "trust_score")
    val trustScore: Int = 0,
    @ColumnInfo(name = "attachment_score")
    val attachmentScore: Int = 0,
    @ColumnInfo(name = "neglect_streak")
    val neglectStreak: Int = 0,
    @ColumnInfo(name = "care_streak")
    val careStreak: Int = 0,
    @ColumnInfo(name = "last_open_at")
    val lastOpenAt: Long = 0L,
    @ColumnInfo(name = "last_meaningful_interaction_at")
    val lastMeaningfulInteractionAt: Long = 0L
) {
    fun toDomain(): PetState {
        val safeMood = runCatching { PetMood.valueOf(mood) }.getOrDefault(PetMood.NEUTRAL)
        return PetState(
            mood = safeMood,
            energy = energy,
            hunger = hunger,
            sleepiness = sleepiness,
            social = social,
            comfort = comfort,
            stimulation = stimulation,
            moodValence = moodValence,
            moodArousal = moodArousal,
            bond = bond,
            trustScore = trustScore,
            attachmentScore = attachmentScore,
            neglectStreak = neglectStreak,
            careStreak = careStreak,
            lastUpdatedAt = lastUpdatedAt,
            lastOpenAt = lastOpenAt,
            lastMeaningfulInteractionAt = lastMeaningfulInteractionAt
        )
    }

    companion object {
        const val SINGLETON_ID: Int = 1

        fun fromDomain(state: PetState): PetStateEntity {
            return PetStateEntity(
                id = SINGLETON_ID,
                mood = state.mood.name,
                energy = state.energy,
                hunger = state.hunger,
                sleepiness = state.sleepiness,
                social = state.social,
                comfort = state.comfort,
                stimulation = state.stimulation,
                moodValence = state.moodValence,
                moodArousal = state.moodArousal,
                bond = state.bond,
                trustScore = state.trustScore,
                attachmentScore = state.attachmentScore,
                neglectStreak = state.neglectStreak,
                careStreak = state.careStreak,
                lastUpdatedAt = state.lastUpdatedAt,
                lastOpenAt = state.lastOpenAt,
                lastMeaningfulInteractionAt = state.lastMeaningfulInteractionAt
            )
        }
    }
}
