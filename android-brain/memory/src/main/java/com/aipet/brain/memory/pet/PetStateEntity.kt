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
    val lastUpdatedAt: Long
) {
    fun toDomain(): PetState {
        val safeMood = runCatching { PetMood.valueOf(mood) }.getOrDefault(PetMood.NEUTRAL)
        return PetState(
            mood = safeMood,
            energy = energy,
            hunger = hunger,
            sleepiness = sleepiness,
            social = social,
            bond = bond,
            lastUpdatedAt = lastUpdatedAt
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
                bond = state.bond,
                lastUpdatedAt = state.lastUpdatedAt
            )
        }
    }
}
