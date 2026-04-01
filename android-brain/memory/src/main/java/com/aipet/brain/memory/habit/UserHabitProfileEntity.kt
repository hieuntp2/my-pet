package com.aipet.brain.memory.habit

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_habit_profile")
data class UserHabitProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "preferred_time_slots_json")
    val preferredTimeSlotsJson: String,
    @ColumnInfo(name = "avg_session_length_ms")
    val avgSessionLengthMs: Long,
    @ColumnInfo(name = "avg_sessions_per_day")
    val avgSessionsPerDay: Float,
    @ColumnInfo(name = "primary_interaction_style")
    val primaryInteractionStyle: String,
    @ColumnInfo(name = "recent_consistency_score")
    val recentConsistencyScore: Float,
    @ColumnInfo(name = "strongest_daypart")
    val strongestDaypart: String,
    @ColumnInfo(name = "last_updated_at_ms")
    val lastUpdatedAtMs: Long
)
