package com.aipet.brain.brain.evolution.domain

interface EpisodeRepository {
    suspend fun save(episode: MemoryEpisode)
    suspend fun loadRecent(limit: Int = 20): List<MemoryEpisode>
    suspend fun loadSince(sinceMs: Long): List<MemoryEpisode>
    suspend fun loadByImportance(minImportance: Float = 0.5f, limit: Int = 10): List<MemoryEpisode>
    suspend fun count(): Int
}
