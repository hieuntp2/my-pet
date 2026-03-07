package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "face_profile_observation_links",
    primaryKeys = ["profile_id", "observation_id"],
    foreignKeys = [
        ForeignKey(
            entity = FaceProfileEntity::class,
            parentColumns = ["profile_id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["observation_id"])
    ]
)
data class FaceProfileObservationLinkEntity(
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "observation_id")
    val observationId: String,
    @ColumnInfo(name = "linked_at_ms")
    val linkedAtMs: Long
)
