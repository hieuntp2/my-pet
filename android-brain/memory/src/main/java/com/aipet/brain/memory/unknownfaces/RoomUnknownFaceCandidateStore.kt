package com.aipet.brain.memory.unknownfaces

import com.aipet.brain.memory.db.UnknownFaceCandidateDao
import com.aipet.brain.memory.db.UnknownFaceCandidateEntity
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class RoomUnknownFaceCandidateStore(
    private val dao: UnknownFaceCandidateDao
) : UnknownFaceCandidateStore {
    override suspend fun upsert(candidate: UnknownFaceCandidateRecord) {
        val normalizedCandidateId = candidate.candidateId.trim()
        if (normalizedCandidateId.isBlank()) {
            return
        }
        val normalizedEmbedding = candidate.representativeEmbedding
            .asSequence()
            .filter { value -> value.isFinite() }
            .toList()
        if (normalizedEmbedding.isEmpty() || normalizedEmbedding.size != candidate.representativeEmbedding.size) {
            return
        }
        dao.upsert(
            UnknownFaceCandidateEntity(
                candidateId = normalizedCandidateId,
                status = candidate.status.name,
                representativeEmbeddingBlob = normalizedEmbedding.toBlob(),
                embeddingDim = normalizedEmbedding.size,
                previewImageBase64 = candidate.previewImageBase64?.trim()?.ifBlank { null },
                firstSeenAtMs = candidate.firstSeenAtMs,
                lastSeenAtMs = candidate.lastSeenAtMs,
                seenFrameCount = candidate.seenFrameCount.coerceAtLeast(0),
                seenEncounterCount = candidate.seenEncounterCount.coerceAtLeast(0),
                averageQualityScore = candidate.averageQualityScore.coerceIn(0f, 1f),
                lastPromptAtMs = candidate.lastPromptAtMs,
                suppressedUntilMs = candidate.suppressedUntilMs,
                closestKnownPersonId = candidate.closestKnownPersonId?.trim()?.ifBlank { null },
                closestKnownSimilarity = candidate.closestKnownSimilarity,
                lastDecision = candidate.lastDecision.name,
                updatedAtMs = candidate.updatedAtMs
            )
        )
    }

    override suspend fun getById(candidateId: String): UnknownFaceCandidateRecord? {
        val normalizedCandidateId = candidateId.trim()
        if (normalizedCandidateId.isBlank()) {
            return null
        }
        return dao.getById(normalizedCandidateId)?.toRecord()
    }

    override suspend fun listActive(): List<UnknownFaceCandidateRecord> {
        return dao.listActive().mapNotNull { entity ->
            entity.toRecord()
        }
    }

    override suspend fun listAll(): List<UnknownFaceCandidateRecord> {
        return dao.listAll().mapNotNull { entity ->
            entity.toRecord()
        }
    }

    override suspend fun countActive(): Int {
        return dao.countActive()
    }

    override suspend fun deleteResolvedOlderThan(olderThanMs: Long): Int {
        return dao.deleteResolvedOlderThan(olderThanMs = olderThanMs)
    }

    private fun UnknownFaceCandidateEntity.toRecord(): UnknownFaceCandidateRecord? {
        val embedding = representativeEmbeddingBlob.toFloatList(expectedDim = embeddingDim)
        if (embedding.isEmpty()) {
            return null
        }
        return UnknownFaceCandidateRecord(
            candidateId = candidateId,
            status = status.toUnknownFaceCandidateStatus(),
            representativeEmbedding = embedding,
            previewImageBase64 = previewImageBase64,
            firstSeenAtMs = firstSeenAtMs,
            lastSeenAtMs = lastSeenAtMs,
            seenFrameCount = seenFrameCount,
            seenEncounterCount = seenEncounterCount,
            averageQualityScore = averageQualityScore.coerceIn(0f, 1f),
            lastPromptAtMs = lastPromptAtMs,
            suppressedUntilMs = suppressedUntilMs,
            closestKnownPersonId = closestKnownPersonId,
            closestKnownSimilarity = closestKnownSimilarity,
            lastDecision = lastDecision.toUnknownFaceDecision(),
            updatedAtMs = updatedAtMs
        )
    }
}

private fun String.toUnknownFaceCandidateStatus(): UnknownFaceCandidateStatus {
    return UnknownFaceCandidateStatus.entries.firstOrNull { entry ->
        entry.name == this
    } ?: UnknownFaceCandidateStatus.COLLECTING
}

private fun String.toUnknownFaceDecision(): UnknownFaceDecision {
    return UnknownFaceDecision.entries.firstOrNull { entry ->
        entry.name == this
    } ?: UnknownFaceDecision.UNKNOWN
}

private fun List<Float>.toBlob(): ByteArray {
    val buffer = ByteBuffer
        .allocate(size * Float.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
    for (value in this) {
        buffer.putFloat(value)
    }
    return buffer.array()
}

private fun ByteArray.toFloatList(expectedDim: Int): List<Float> {
    if (isEmpty() || size % Float.SIZE_BYTES != 0) {
        return emptyList()
    }
    val dimFromBlob = size / Float.SIZE_BYTES
    val dim = if (expectedDim > 0) {
        min(expectedDim, dimFromBlob)
    } else {
        dimFromBlob
    }
    if (dim <= 0) {
        return emptyList()
    }
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return buildList(dim) {
        repeat(dim) {
            add(buffer.float)
        }
    }
}
