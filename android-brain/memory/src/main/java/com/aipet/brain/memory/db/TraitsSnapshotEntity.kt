package com.aipet.brain.memory.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traits_snapshots")
data class TraitsSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val curiosity: Float,
    val sociability: Float,
    val energy: Float,
    val patience: Float,
    val boldness: Float,
    val createdAt: Long
)
