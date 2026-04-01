package com.aipet.brain.memory.evolution

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_episodes")
data class MemoryEpisodeEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "start_time_ms")
    val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms")
    val endTimeMs: Long,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "event_count")
    val eventCount: Int,
    @ColumnInfo(name = "interaction_count")
    val interactionCount: Int,
    @ColumnInfo(name = "interaction_types_json")
    val interactionTypesJson: String,
    @ColumnInfo(name = "dominant_pet_emotion")
    val dominantPetEmotion: String,
    @ColumnInfo(name = "dominant_pet_mood")
    val dominantPetMood: String,
    @ColumnInfo(name = "care_score_delta")
    val careScoreDelta: Int,
    @ColumnInfo(name = "bond_delta")
    val bondDelta: Int,
    @ColumnInfo(name = "neglect_signal")
    val neglectSignal: Boolean,
    @ColumnInfo(name = "reunion_type")
    val reunionType: String,
    @ColumnInfo(name = "user_behavior_tag")
    val userBehaviorTag: String,
    @ColumnInfo(name = "importance_score")
    val importanceScore: Float,
    @ColumnInfo(name = "summary_text")
    val summaryText: String,
    @ColumnInfo(name = "created_at_ms", index = true)
    val createdAtMs: Long
)
