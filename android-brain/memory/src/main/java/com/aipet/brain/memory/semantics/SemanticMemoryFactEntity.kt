package com.aipet.brain.memory.semantics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "semantic_memory_facts",
    indices = [Index(value = ["fact_key"], unique = true)]
)
data class SemanticMemoryFactEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "fact_key")
    val key: String,
    @ColumnInfo(name = "value_json")
    val valueJson: String,
    @ColumnInfo(name = "confidence")
    val confidence: Float,
    @ColumnInfo(name = "source_episode_count")
    val sourceEpisodeCount: Int,
    @ColumnInfo(name = "first_learned_at_ms")
    val firstLearnedAtMs: Long,
    @ColumnInfo(name = "last_confirmed_at_ms")
    val lastConfirmedAtMs: Long,
    @ColumnInfo(name = "last_updated_at_ms", index = true)
    val lastUpdatedAtMs: Long
)
