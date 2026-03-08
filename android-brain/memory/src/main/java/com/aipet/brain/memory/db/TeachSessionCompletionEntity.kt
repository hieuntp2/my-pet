package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teach_session_completion",
    indices = [
        Index(value = ["updated_at_ms"])
    ]
)
data class TeachSessionCompletionEntity(
    @PrimaryKey
    @ColumnInfo(name = "teach_session_id")
    val teachSessionId: String,
    @ColumnInfo(name = "is_completed_confirmed")
    val isCompletedConfirmed: Boolean,
    @ColumnInfo(name = "confirmed_at_ms")
    val confirmedAtMs: Long?,
    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long
)
