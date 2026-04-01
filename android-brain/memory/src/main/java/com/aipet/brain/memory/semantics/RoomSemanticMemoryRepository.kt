package com.aipet.brain.memory.semantics

import com.aipet.brain.brain.evolution.domain.SemanticMemoryFact
import com.aipet.brain.brain.evolution.domain.SemanticMemoryRepository

class RoomSemanticMemoryRepository(private val dao: SemanticMemoryFactDao) : SemanticMemoryRepository {

    override suspend fun upsert(fact: SemanticMemoryFact) {
        dao.upsert(fact.toEntity())
    }

    override suspend fun getByKey(key: String): SemanticMemoryFact? =
        dao.loadByKey(key)?.toDomain()

    override suspend fun getAll(): List<SemanticMemoryFact> =
        dao.loadAll().map { it.toDomain() }

    override suspend fun getWithMinConfidence(minConfidence: Float): List<SemanticMemoryFact> =
        dao.loadWithMinConfidence(minConfidence).map { it.toDomain() }
}

private fun SemanticMemoryFact.toEntity(): SemanticMemoryFactEntity = SemanticMemoryFactEntity(
    id = id,
    key = key,
    valueJson = valueJson,
    confidence = confidence,
    sourceEpisodeCount = sourceEpisodeCount,
    firstLearnedAtMs = firstLearnedAtMs,
    lastConfirmedAtMs = lastConfirmedAtMs,
    lastUpdatedAtMs = lastUpdatedAtMs
)

private fun SemanticMemoryFactEntity.toDomain(): SemanticMemoryFact = SemanticMemoryFact(
    id = id,
    key = key,
    valueJson = valueJson,
    confidence = confidence,
    sourceEpisodeCount = sourceEpisodeCount,
    firstLearnedAtMs = firstLearnedAtMs,
    lastConfirmedAtMs = lastConfirmedAtMs,
    lastUpdatedAtMs = lastUpdatedAtMs
)
