package com.aipet.brain.memory.habit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserHabitProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserHabitProfileEntity)

    @Query("SELECT * FROM user_habit_profile WHERE id = 'singleton' LIMIT 1")
    suspend fun load(): UserHabitProfileEntity?
}
