package com.aipet.brain.brain.evolution.domain

interface UserHabitRepository {
    suspend fun load(): UserHabitProfile
    suspend fun save(profile: UserHabitProfile)
}
