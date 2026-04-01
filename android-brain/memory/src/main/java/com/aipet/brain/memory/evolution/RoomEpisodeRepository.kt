package com.aipet.brain.memory.evolution

import com.aipet.brain.brain.evolution.domain.EpisodeRepository
import com.aipet.brain.brain.evolution.domain.MemoryEpisode

class RoomEpisodeRepository(private val dao: MemoryEpisodeDao) : EpisodeRepository {

    override suspend fun save(episode: MemoryEpisode) {
        dao.insert(episode.toEntity())
    }

    override suspend fun loadRecent(limit: Int): List<MemoryEpisode> =
        dao.loadRecent(limit).map { it.toDomain() }

    override suspend fun loadSince(sinceMs: Long): List<MemoryEpisode> =
        dao.loadSince(sinceMs).map { it.toDomain() }

    override suspend fun loadByImportance(minImportance: Float, limit: Int): List<MemoryEpisode> =
        dao.loadByImportance(minImportance, limit).map { it.toDomain() }

    override suspend fun count(): Int = dao.count()
}

private fun MemoryEpisode.toEntity(): MemoryEpisodeEntity = MemoryEpisodeEntity(
    id = id,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    durationMs = durationMs,
    eventCount = eventCount,
    interactionCount = interactionCount,
    interactionTypesJson = interactionTypesJson,
    dominantPetEmotion = dominantPetEmotion,
    dominantPetMood = dominantPetMood,
    careScoreDelta = careScoreDelta,
    bondDelta = bondDelta,
    neglectSignal = neglectSignal,
    reunionType = reunionType,
    userBehaviorTag = userBehaviorTag,
    importanceScore = importanceScore,
    summaryText = summaryText,
    createdAtMs = createdAtMs
)

private fun MemoryEpisodeEntity.toDomain(): MemoryEpisode = MemoryEpisode(
    id = id,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    durationMs = durationMs,
    eventCount = eventCount,
    interactionCount = interactionCount,
    interactionTypesJson = interactionTypesJson,
    dominantPetEmotion = dominantPetEmotion,
    dominantPetMood = dominantPetMood,
    careScoreDelta = careScoreDelta,
    bondDelta = bondDelta,
    neglectSignal = neglectSignal,
    reunionType = reunionType,
    userBehaviorTag = userBehaviorTag,
    importanceScore = importanceScore,
    summaryText = summaryText,
    createdAtMs = createdAtMs
)
