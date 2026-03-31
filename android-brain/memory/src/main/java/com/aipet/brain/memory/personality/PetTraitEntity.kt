package com.aipet.brain.memory.personality

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aipet.brain.brain.personality.PetTrait

@Entity(
    tableName = "pet_traits",
    indices = [Index(value = ["updated_at"], name = "index_pet_traits_updated_at")]
)
data class PetTraitEntity(
    @PrimaryKey
    @ColumnInfo(name = "pet_id")
    val petId: String,
    @ColumnInfo(name = "playful")
    val playful: Float,
    @ColumnInfo(name = "lazy")
    val lazy: Float,
    @ColumnInfo(name = "curious")
    val curious: Float,
    @ColumnInfo(name = "social")
    val social: Float,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    // v2 trait fields added in migration 21→22
    @ColumnInfo(name = "patience")
    val patience: Float = 0.5f,
    @ColumnInfo(name = "attachment")
    val attachment: Float = 0.3f,
    @ColumnInfo(name = "energy_profile")
    val energyProfile: Float = 0.5f
) {
    fun toDomain(): PetTrait {
        return PetTrait(
            petId = petId,
            playful = playful,
            lazy = lazy,
            curious = curious,
            social = social,
            updatedAt = updatedAt,
            patience = patience,
            attachment = attachment,
            energyProfile = energyProfile
        )
    }

    companion object {
        fun fromDomain(trait: PetTrait): PetTraitEntity {
            return PetTraitEntity(
                petId = trait.petId,
                playful = trait.playful,
                lazy = trait.lazy,
                curious = trait.curious,
                social = trait.social,
                updatedAt = trait.updatedAt,
                patience = trait.patience,
                attachment = trait.attachment,
                energyProfile = trait.energyProfile
            )
        }
    }
}
