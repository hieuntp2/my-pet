package com.aipet.brain.memory.persons

import com.aipet.brain.memory.db.PersonDao
import com.aipet.brain.memory.db.PersonEntity

class RoomPersonStore(
    private val personDao: PersonDao,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : PersonStore {
    override suspend fun insert(person: PersonRecord) {
        personDao.insert(person.toEntity(isOwner = false))
        if (person.isOwner) {
            personDao.assignOwner(
                personId = person.personId,
                updatedAtMs = nowProvider()
            )
        }
    }

    override suspend fun update(person: PersonRecord): Boolean {
        val updated = personDao.update(person.toEntity(isOwner = false)) > 0
        if (!updated) {
            return false
        }
        if (person.isOwner) {
            return personDao.assignOwner(
                personId = person.personId,
                updatedAtMs = nowProvider()
            )
        }
        return true
    }

    override suspend fun getById(personId: String): PersonRecord? {
        return personDao.getById(personId)?.toRecord()
    }

    override suspend fun listAll(): List<PersonRecord> {
        return personDao.listAll().map { entity ->
            entity.toRecord()
        }
    }

    override suspend fun listTopByFamiliarity(limit: Int): List<PersonRecord> {
        if (limit <= 0) {
            return emptyList()
        }
        return personDao.listTopByFamiliarity(limit = limit).map { entity ->
            entity.toRecord()
        }
    }

    override suspend fun getOwner(): PersonRecord? {
        return personDao.getOwner()?.toRecord()
    }

    override suspend fun assignOwner(personId: String): Boolean {
        return personDao.assignOwner(
            personId = personId,
            updatedAtMs = nowProvider()
        )
    }

    override suspend fun clearOwner(): Boolean {
        return personDao.clearOwnerFlags(updatedAtMs = nowProvider()) > 0
    }

    override suspend fun recordPersonSeen(
        personId: String,
        seenAtMs: Long
    ): PersonRecord? {
        val normalizedPersonId = personId.trim()
        if (normalizedPersonId.isBlank()) {
            return null
        }
        val updated = personDao.incrementSeenCount(
            personId = normalizedPersonId,
            seenAtMs = seenAtMs,
            updatedAtMs = nowProvider()
        ) > 0
        if (!updated) {
            return null
        }
        return personDao.getById(normalizedPersonId)?.toRecord()
    }

    override suspend fun updatePersonSeenStats(
        personId: String,
        timestampMs: Long
    ): PersonRecord? {
        return recordPersonSeen(
            personId = personId,
            seenAtMs = timestampMs
        )
    }

    override suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): PersonRecord? {
        if (!delta.isFinite()) {
            return null
        }
        val normalizedPersonId = personId.trim()
        if (normalizedPersonId.isBlank()) {
            return null
        }
        val updated = personDao.incrementFamiliarity(
            personId = normalizedPersonId,
            delta = delta,
            updatedAtMs = updatedAtMs
        ) > 0
        if (!updated) {
            return null
        }
        return personDao.getById(normalizedPersonId)?.toRecord()
    }

    private fun PersonRecord.toEntity(isOwner: Boolean): PersonEntity {
        return PersonEntity(
            personId = personId,
            displayName = displayName,
            nickname = nickname,
            isOwner = isOwner,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
            lastSeenAtMs = lastSeenAtMs,
            seenCount = seenCount,
            familiarityScore = familiarityScore
        )
    }

    private fun PersonEntity.toRecord(): PersonRecord {
        return PersonRecord(
            personId = personId,
            displayName = displayName,
            nickname = nickname,
            isOwner = isOwner,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
            lastSeenAtMs = lastSeenAtMs,
            seenCount = seenCount,
            familiarityScore = familiarityScore
        )
    }
}
