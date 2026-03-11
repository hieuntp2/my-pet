package com.aipet.brain.app.ui.persons

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.UserTaughtPersonEventPayload
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.perception.vision.face.embedding.FaceEmbeddingEngine
import java.util.UUID

internal sealed interface TeachPersonPersistenceResult {
    data class Success(val personId: String) : TeachPersonPersistenceResult
    data class ValidationError(val message: String) : TeachPersonPersistenceResult
    data class Failure(val message: String) : TeachPersonPersistenceResult
}

internal class TeachPersonSaveController(
    private val database: RoomDatabase,
    private val contentResolver: ContentResolver,
    private val personStore: PersonStore,
    private val faceProfileStore: FaceProfileStore,
    private val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val eventBus: EventBus,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val personIdProvider: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun saveTaughtPerson(
        displayName: String,
        nickname: String,
        capturedSamples: List<TeachPersonCapturedSample>
    ): TeachPersonPersistenceResult {
        val normalizedDisplayName = displayName.trim()
        if (normalizedDisplayName.isBlank()) {
            return TeachPersonPersistenceResult.ValidationError("Display name cannot be blank.")
        }
        if (capturedSamples.size < MINIMUM_REQUIRED_SAMPLES) {
            return TeachPersonPersistenceResult.ValidationError(
                "Capture at least $MINIMUM_REQUIRED_SAMPLES samples before saving."
            )
        }

        val nowMs = nowProvider()
        val personRecord = PersonRecord(
            personId = personIdProvider(),
            displayName = normalizedDisplayName,
            nickname = nickname.trim().ifBlank { null },
            isOwner = false,
            createdAtMs = nowMs,
            updatedAtMs = nowMs,
            lastSeenAtMs = null,
            seenCount = 0
        )

        return runCatching {
            database.withTransaction {
                personStore.insert(personRecord)
                val profile = faceProfileStore.createProfileCandidate(
                    label = "teach_person_${personRecord.personId}",
                    note = "created_from_teach_person_flow"
                )
                check(faceProfileStore.linkProfileToPerson(profile.profileId, personRecord.personId)) {
                    "Failed to link profile to person."
                }

                capturedSamples.forEach { sample ->
                    val sourceUri = sample.faceCropUri ?: sample.imageUri
                    val bitmap = loadBitmap(sourceUri)
                        ?: error("Unable to load sample image: $sourceUri")
                    val embedding = faceEmbeddingEngine.generateEmbedding(bitmap).getOrElse { error ->
                        throw IllegalStateException(
                            "Embedding inference failed for sample ${sample.observationId}: ${error.message}",
                            error
                        )
                    }
                    check(
                        faceProfileStore.addEmbeddingToProfile(
                            profileId = profile.profileId,
                            values = embedding.toList(),
                            metadata = "teach_session_sample=${sample.observationId}"
                        ) != null
                    ) {
                        "Unable to persist embedding for sample ${sample.observationId}."
                    }
                }
            }

            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.USER_TAUGHT_PERSON,
                    timestampMs = nowProvider(),
                    payloadJson = UserTaughtPersonEventPayload(
                        personId = personRecord.personId,
                        displayName = personRecord.displayName,
                        sampleCount = capturedSamples.size
                    ).toJson()
                )
            )
            TeachPersonPersistenceResult.Success(personRecord.personId)
        }.getOrElse { error ->
            TeachPersonPersistenceResult.Failure(error.message ?: "Failed to save taught person.")
        }
    }

    private fun loadBitmap(imageUri: String): Bitmap? {
        val parsed = Uri.parse(imageUri)
        return runCatching {
            contentResolver.openInputStream(parsed)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: when (parsed.scheme) {
                null, "file" -> BitmapFactory.decodeFile(parsed.path)
                else -> null
            }
        }.getOrNull()
    }

    companion object {
        const val MINIMUM_REQUIRED_SAMPLES: Int = 3
    }
}
