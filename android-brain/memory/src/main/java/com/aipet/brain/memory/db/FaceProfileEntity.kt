package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_profiles",
    indices = [
        Index(value = ["updated_at_ms"]),
        Index(value = ["linked_person_id"])
    ]
)
data class FaceProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "label")
    val label: String?,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "linked_person_id")
    val linkedPersonId: String?,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long
)
