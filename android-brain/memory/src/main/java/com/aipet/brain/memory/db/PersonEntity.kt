package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persons",
    indices = [
        Index(value = ["is_owner"]),
        Index(value = ["updated_at_ms"])
    ]
)
data class PersonEntity(
    @PrimaryKey
    @ColumnInfo(name = "person_id")
    val personId: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "nickname")
    val nickname: String?,
    @ColumnInfo(name = "is_owner")
    val isOwner: Boolean,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,
    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long,
    @ColumnInfo(name = "last_seen_at_ms")
    val lastSeenAtMs: Long?,
    @ColumnInfo(name = "seen_count")
    val seenCount: Int
)
