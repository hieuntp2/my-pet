package com.aipet.brain.app.ui.persons

import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.app.reactions.PersonSeenSource
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonDeletedPayload
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import java.util.UUID

internal class PersonFlowController(
    private val personStore: PersonStore,
    private val personSeenEventPublisher: PersonSeenEventPublisher? = null,
    private val faceProfileStore: FaceProfileStore? = null,
    private val eventBus: EventBus? = null,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun loadPersons(): List<PersonRecord> {
        return personStore.listAll()
    }

    suspend fun loadPersonCards(): List<PersonDebugCard> {
        val persons = personStore.listAll()
        val profileStore = faceProfileStore ?: return persons.map { person ->
            PersonDebugCard(
                person = person,
                profileCount = 0,
                embeddingCount = 0,
                previewAvailable = false
            )
        }
        return persons.map { person ->
            val profiles = profileStore.listProfilesForPerson(person.personId)
            val embeddingCount = profiles.sumOf { profile ->
                profileStore.listProfileEmbeddings(profile.profileId).size
            }
            PersonDebugCard(
                person = person,
                profileCount = profiles.size,
                embeddingCount = embeddingCount,
                previewAvailable = false
            )
        }
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

    suspend fun saveTaughtPerson(input: PersonEditorInput): PersonSaveResult {
        return createPerson(input)
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
            personSeenEventPublisher?.publishPersonSeen(
                person = updated,
                source = PersonSeenSource.DIRECT_PERSON_DEBUG_ACTION
            )
            PersonSeenRecordResult.Success(updated)
        } else {
            PersonSeenRecordResult.Failure("Unable to record person seen.")
        }
    }

    suspend fun deletePerson(personId: String): PersonDeleteResult {
        val normalizedId = personId.trim()
        if (normalizedId.isBlank()) {
            return PersonDeleteResult.Failure("Invalid person ID.")
        }
        val existingPerson = personStore.getById(normalizedId)
            ?: return PersonDeleteResult.Failure("Person not found or could not be deleted.")
        val profileStore = faceProfileStore
        val profileCount = if (profileStore != null) {
            profileStore.listProfilesForPerson(normalizedId).size
        } else {
            0
        }
        val embeddingCount = if (profileStore != null) {
            profileStore.listProfilesForPerson(normalizedId).sumOf { profile ->
                profileStore.listProfileEmbeddings(profile.profileId).size
            }
        } else {
            0
        }
        val deleted = personStore.deletePerson(normalizedId)
        return if (deleted) {
            eventBus?.publish(
                EventEnvelope.create(
                    type = EventType.PERSON_DELETED,
                    timestampMs = nowProvider(),
                    payloadJson = PersonDeletedPayload(
                        personId = normalizedId,
                        displayName = existingPerson.displayName,
                        deletedAtMs = nowProvider(),
                        profileCount = profileCount,
                        embeddingCount = embeddingCount
                    ).toJson()
                )
            )
            PersonDeleteResult.Success(
                personId = normalizedId,
                deletedDisplayName = existingPerson.displayName,
                profileCount = profileCount,
                embeddingCount = embeddingCount
            )
        } else {
            PersonDeleteResult.Failure("Person not found or could not be deleted.")
        }
    }
}

internal data class PersonDebugCard(
    val person: PersonRecord,
    val profileCount: Int,
    val embeddingCount: Int,
    val previewAvailable: Boolean
)

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

internal sealed interface PersonDeleteResult {
    data class Success(
        val personId: String,
        val deletedDisplayName: String,
        val profileCount: Int,
        val embeddingCount: Int
    ) : PersonDeleteResult
    data class Failure(val message: String) : PersonDeleteResult
}
