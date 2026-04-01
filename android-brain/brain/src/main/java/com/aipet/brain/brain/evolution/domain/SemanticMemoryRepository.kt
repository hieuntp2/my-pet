package com.aipet.brain.brain.evolution.domain

interface SemanticMemoryRepository {
    suspend fun upsert(fact: SemanticMemoryFact)
    suspend fun getByKey(key: String): SemanticMemoryFact?
    suspend fun getAll(): List<SemanticMemoryFact>
    suspend fun getWithMinConfidence(minConfidence: Float): List<SemanticMemoryFact>
}
