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
                "Capture at least $MINIMUM_REQUIRED_SAMPLES sample before saving."
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

                var embeddingCount = 0
                capturedSamples.forEach { sample ->
                    // Prefer face crop for better embedding quality; fall back to full image.
                    var bitmap = sample.faceCropUri?.let { loadBitmap(it) }
                    if (bitmap == null) {
                        bitmap = loadBitmap(sample.imageUri)
                    }
                    if (bitmap == null) {
                        // Neither crop nor full image is accessible — skip this sample.
                        return@forEach
                    }
                    val embedding = faceEmbeddingEngine.generateEmbedding(bitmap).getOrElse { error ->
                        throw IllegalStateException(
                            "Embedding inference failed for sample ${sample.observationId}: ${error.message}",
                            error
                        )
                    }
                    if (
                        faceProfileStore.addEmbeddingToProfile(
                            profileId = profile.profileId,
                            values = embedding.toList(),
                            metadata = "teach_session_sample=${sample.observationId}"
                        ) != null
                    ) {
                        embeddingCount++
                    }
                }
                check(embeddingCount > 0) {
                    "No embeddings could be saved. Check that sample images are still accessible."
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
            // First pass: read dimensions only to calculate a safe inSampleSize.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            val options = BitmapFactory.Options().apply {
                inSampleSize = if (bounds.outWidth > 0 && bounds.outHeight > 0) {
                    calculateSampleSize(bounds.outWidth, bounds.outHeight, maxDimPx = 512)
                } else {
                    1
                }
            }
            contentResolver.openInputStream(parsed)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: when (parsed.scheme) {
                null, "file" -> BitmapFactory.decodeFile(parsed.path, options)
                else -> null
            }
        }.getOrNull()
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimPx: Int): Int {
        var s = 1
        while (width / s > maxDimPx || height / s > maxDimPx) s *= 2
        return s
    }

    companion object {
        const val MINIMUM_REQUIRED_SAMPLES: Int = 1
    }
}
