package com.aipet.brain.memory.habit

import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import com.aipet.brain.brain.evolution.domain.UserHabitRepository

class RoomUserHabitRepository(private val dao: UserHabitProfileDao) : UserHabitRepository {

    override suspend fun load(): UserHabitProfile {
        val entity = dao.load()
        if (entity != null) return entity.toDomain()
        val default = UserHabitProfile.DEFAULT
        dao.upsert(default.toEntity())
        return default
    }

    override suspend fun save(profile: UserHabitProfile) {
        dao.upsert(profile.toEntity())
    }
}

private fun UserHabitProfile.toEntity(): UserHabitProfileEntity = UserHabitProfileEntity(
    id = id,
    preferredTimeSlotsJson = preferredTimeSlotsJson,
    avgSessionLengthMs = avgSessionLengthMs,
    avgSessionsPerDay = avgSessionsPerDay,
    primaryInteractionStyle = primaryInteractionStyle,
    recentConsistencyScore = recentConsistencyScore,
    strongestDaypart = strongestDaypart,
    lastUpdatedAtMs = lastUpdatedAtMs
)

private fun UserHabitProfileEntity.toDomain(): UserHabitProfile = UserHabitProfile(
    id = id,
    preferredTimeSlotsJson = preferredTimeSlotsJson,
    avgSessionLengthMs = avgSessionLengthMs,
    avgSessionsPerDay = avgSessionsPerDay,
    primaryInteractionStyle = primaryInteractionStyle,
    recentConsistencyScore = recentConsistencyScore,
    strongestDaypart = strongestDaypart,
    lastUpdatedAtMs = lastUpdatedAtMs
)
