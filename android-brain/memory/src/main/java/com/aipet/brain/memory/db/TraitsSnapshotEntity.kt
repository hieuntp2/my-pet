package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "traits_snapshot",
    indices = [
        Index(value = ["captured_at_ms"])
    ]
)
data class TraitsSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String,
    @ColumnInfo(name = "captured_at_ms")
    val capturedAtMs: Long,
    @ColumnInfo(name = "curiosity")
    val curiosity: Float,
    @ColumnInfo(name = "sociability")
    val sociability: Float,
    @ColumnInfo(name = "energy")
    val energy: Float,
    @ColumnInfo(name = "patience")
    val patience: Float,
    @ColumnInfo(name = "boldness")
    val boldness: Float
)
