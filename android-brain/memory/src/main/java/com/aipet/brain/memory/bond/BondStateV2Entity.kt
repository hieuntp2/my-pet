package com.aipet.brain.memory.bond

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bond_state_v2")
data class BondStateV2Entity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "affection")
    val affection: Float,
    @ColumnInfo(name = "trust")
    val trust: Float,
    @ColumnInfo(name = "dependency")
    val dependency: Float,
    @ColumnInfo(name = "stability")
    val stability: Float,
    @ColumnInfo(name = "last_updated_at_ms")
    val lastUpdatedAtMs: Long
)
