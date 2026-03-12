package com.aipet.brain.brain.traits

typealias TraitsRepository = TraitsSnapshotRepository

class TraitsManager(
    private val repository: TraitsRepository
) {
    private var currentTraits: TraitsSnapshot? = null

    suspend fun loadInitialTraits() {
        currentTraits = repository.latest()
    }

    fun getCurrentTraits(): TraitsSnapshot? {
        return currentTraits
    }

    suspend fun saveSnapshot(snapshot: TraitsSnapshot) {
        repository.save(snapshot)
        currentTraits = snapshot
    }
}
