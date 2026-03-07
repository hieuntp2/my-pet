package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import java.util.UUID

internal class PersonFlowController(
    private val personStore: PersonStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun loadPersons(): List<PersonRecord> {
        return personStore.listAll()
    }

    suspend fun loadPerson(personId: String): PersonRecord? {
        return personStore.getById(personId)
    }

    suspend fun createPerson(input: PersonEditorInput): PersonSaveResult {
        val validatedInput = validate(input) ?: return PersonSaveResult.ValidationError(
            message = "Display name cannot be blank."
        )

        val now = nowProvider()
        val personRecord = PersonRecord(
            personId = idProvider(),
            displayName = validatedInput.displayName,
            nickname = validatedInput.nickname,
            isOwner = false,
            createdAtMs = now,
            updatedAtMs = now,
            lastSeenAtMs = null,
            seenCount = 0
        )
        personStore.insert(personRecord)
        return PersonSaveResult.Success(personRecord.personId)
    }

    suspend fun updatePerson(
        existingPerson: PersonRecord,
        input: PersonEditorInput
    ): PersonSaveResult {
        val validatedInput = validate(input) ?: return PersonSaveResult.ValidationError(
            message = "Display name cannot be blank."
        )

        val updatedPerson = existingPerson.copy(
            displayName = validatedInput.displayName,
            nickname = validatedInput.nickname,
            updatedAtMs = nowProvider()
        )
        val updated = personStore.update(updatedPerson)
        return if (updated) {
            PersonSaveResult.Success(updatedPerson.personId)
        } else {
            PersonSaveResult.Failure("Failed to update person.")
        }
    }

    private fun validate(input: PersonEditorInput): ValidatedPersonEditorInput? {
        val trimmedDisplayName = input.displayName.trim()
        if (trimmedDisplayName.isBlank()) {
            return null
        }
        val trimmedNickname = input.nickname.trim().ifBlank { null }
        return ValidatedPersonEditorInput(
            displayName = trimmedDisplayName,
            nickname = trimmedNickname
        )
    }

    suspend fun loadOwner(): PersonRecord? {
        return personStore.getOwner()
    }

    suspend fun assignOwner(personId: String): OwnerAssignmentResult {
        val assigned = personStore.assignOwner(personId)
        return if (assigned) {
            OwnerAssignmentResult.Success("Owner assigned successfully.")
        } else {
            OwnerAssignmentResult.Failure("Unable to assign owner.")
        }
    }

    suspend fun clearOwner(): OwnerAssignmentResult {
        val cleared = personStore.clearOwner()
        return if (cleared) {
            OwnerAssignmentResult.Success("Owner cleared.")
        } else {
            OwnerAssignmentResult.Failure("No owner to clear.")
        }
    }

    suspend fun recordPersonSeen(personId: String): PersonSeenRecordResult {
        val updated = personStore.recordPersonSeen(
            personId = personId,
            seenAtMs = nowProvider()
        )
        return if (updated != null) {
            PersonSeenRecordResult.Success(updated)
        } else {
            PersonSeenRecordResult.Failure("Unable to record person seen.")
        }
    }
}

internal data class PersonEditorInput(
    val displayName: String,
    val nickname: String
)

internal sealed interface PersonSaveResult {
    data class Success(val personId: String) : PersonSaveResult
    data class ValidationError(val message: String) : PersonSaveResult
    data class Failure(val message: String) : PersonSaveResult
}

private data class ValidatedPersonEditorInput(
    val displayName: String,
    val nickname: String?
)

internal sealed interface OwnerAssignmentResult {
    data class Success(val message: String) : OwnerAssignmentResult
    data class Failure(val message: String) : OwnerAssignmentResult
}

internal sealed interface PersonSeenRecordResult {
    data class Success(val updatedPerson: PersonRecord) : PersonSeenRecordResult
    data class Failure(val message: String) : PersonSeenRecordResult
}
