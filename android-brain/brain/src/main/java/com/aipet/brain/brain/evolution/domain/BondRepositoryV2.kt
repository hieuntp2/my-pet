package com.aipet.brain.brain.evolution.domain

interface BondRepositoryV2 {
    suspend fun load(): BondStateV2
    suspend fun save(state: BondStateV2)
}
