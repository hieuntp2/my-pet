package com.aipet.brain.memory.recognition

import com.aipet.brain.brain.recognition.KnownPersonEmbeddingsSource
import com.aipet.brain.brain.recognition.model.KnownFaceEmbedding
import com.aipet.brain.brain.recognition.model.KnownPersonEmbeddings
import com.aipet.brain.memory.db.FaceProfileDao
import com.aipet.brain.memory.db.FaceProfileEmbeddingEntity
import com.aipet.brain.memory.db.PersonDao
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class RoomKnownPersonEmbeddingsSource(
    private val personDao: PersonDao,
    private val faceProfileDao: FaceProfileDao
) : KnownPersonEmbeddingsSource {
    override suspend fun loadKnownPersonEmbeddings(): List<KnownPersonEmbeddings> {
        return personDao.listAll().map { person ->
            val embeddings = faceProfileDao.listProfilesForPerson(person.personId)
                .flatMap { profile ->
                    faceProfileDao.listEmbeddingsByProfile(profile.profileId)
                }
                .mapNotNull { entity ->
                    entity.toKnownFaceEmbeddingOrNull()
                }

            KnownPersonEmbeddings(
                personId = person.personId,
                displayName = person.displayName,
                embeddings = embeddings
            )
        }
    }

    private fun FaceProfileEmbeddingEntity.toKnownFaceEmbeddingOrNull(): KnownFaceEmbedding? {
        val decodedValues = vectorBlob.toFloatArray(expectedDim = vectorDim)
        if (decodedValues.isEmpty()) {
            return null
        }
        return KnownFaceEmbedding(
            embeddingId = embeddingId,
            values = decodedValues
        )
    }
}

private fun ByteArray.toFloatArray(expectedDim: Int): FloatArray {
    if (isEmpty() || size % Float.SIZE_BYTES != 0) {
        return FloatArray(0)
    }

    val dimFromBlob = size / Float.SIZE_BYTES
    val resolvedDim = if (expectedDim > 0) {
        min(expectedDim, dimFromBlob)
    } else {
        dimFromBlob
    }
    if (resolvedDim <= 0) {
        return FloatArray(0)
    }

    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(resolvedDim) {
        buffer.float
    }
}
