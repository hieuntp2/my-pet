package com.aipet.brain.memory.persons

interface PersonStore {
    suspend fun insert(person: PersonRecord)

    suspend fun update(person: PersonRecord): Boolean

    suspend fun getById(personId: String): PersonRecord?

    suspend fun listAll(): List<PersonRecord>

    suspend fun getOwner(): PersonRecord?

    suspend fun assignOwner(personId: String): Boolean

    suspend fun clearOwner(): Boolean

    suspend fun recordPersonSeen(
        personId: String,
        seenAtMs: Long = System.currentTimeMillis()
    ): PersonRecord?
}
