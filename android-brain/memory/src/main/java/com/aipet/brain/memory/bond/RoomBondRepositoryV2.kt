package com.aipet.brain.memory.bond

import com.aipet.brain.brain.evolution.domain.BondRepositoryV2
import com.aipet.brain.brain.evolution.domain.BondStateV2

class RoomBondRepositoryV2(private val dao: BondStateV2Dao) : BondRepositoryV2 {

    override suspend fun load(): BondStateV2 {
        val entity = dao.load()
        if (entity != null) return entity.toDomain()
        // Initialize with defaults on first load
        val default = BondStateV2.DEFAULT
        dao.upsert(default.toEntity())
        return default
    }

    override suspend fun save(state: BondStateV2) {
        dao.upsert(state.toEntity())
    }
}

private fun BondStateV2.toEntity(): BondStateV2Entity = BondStateV2Entity(
    id = id,
    affection = affection,
    trust = trust,
    dependency = dependency,
    stability = stability,
    lastUpdatedAtMs = lastUpdatedAtMs
)

private fun BondStateV2Entity.toDomain(): BondStateV2 = BondStateV2(
    id = id,
    affection = affection,
    trust = trust,
    dependency = dependency,
    stability = stability,
    lastUpdatedAtMs = lastUpdatedAtMs
)
