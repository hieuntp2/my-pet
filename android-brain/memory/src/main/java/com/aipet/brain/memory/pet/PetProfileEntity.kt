package com.aipet.brain.memory.pet

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aipet.brain.brain.pet.PetProfile

@Entity(tableName = "pet_profile")
data class PetProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "slot_id")
    val slotId: Int = SINGLETON_SLOT_ID,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): PetProfile {
        return PetProfile(
            id = profileId,
            name = name,
            createdAt = createdAt
        )
    }

    companion object {
        const val SINGLETON_SLOT_ID: Int = 1

        fun fromDomain(profile: PetProfile): PetProfileEntity {
            return PetProfileEntity(
                slotId = SINGLETON_SLOT_ID,
                profileId = profile.id,
                name = profile.name,
                createdAt = profile.createdAt
            )
        }
    }
}
