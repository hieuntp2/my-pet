package com.aipet.brain.memory.persons

interface PersonStore {
    suspend fun insert(person: PersonRecord)

    suspend fun update(person: PersonRecord): Boolean

    suspend fun getById(personId: String): PersonRecord?

    suspend fun listAll(): List<PersonRecord>

    suspend fun listTopByFamiliarity(limit: Int): List<PersonRecord> {
        return listAll().take(limit.coerceAtLeast(0))
    }

    suspend fun getOwner(): PersonRecord?

    suspend fun assignOwner(personId: String): Boolean

    suspend fun clearOwner(): Boolean

    suspend fun recordPersonSeen(
        personId: String,
        seenAtMs: Long = System.currentTimeMillis()
    ): PersonRecord?

    suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long = System.currentTimeMillis()
    ): PersonRecord? {
        if (!delta.isFinite()) {
            return null
        }
        val normalizedPersonId = personId.trim()
        if (normalizedPersonId.isBlank()) {
            return null
        }
        val current = getById(normalizedPersonId) ?: return null
        val updated = current.copy(
            familiarityScore = (current.familiarityScore + delta).coerceIn(0f, 1f),
            updatedAtMs = updatedAtMs
        )
        if (!update(updated)) {
            return null
        }
        return getById(normalizedPersonId)
    }
}
