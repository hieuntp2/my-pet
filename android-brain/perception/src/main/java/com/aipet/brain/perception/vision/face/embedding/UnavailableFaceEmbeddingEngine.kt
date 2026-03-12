package com.aipet.brain.perception.vision.face.embedding

import android.graphics.Bitmap

class UnavailableFaceEmbeddingEngine(
    reason: String
) : FaceEmbeddingEngine {

    private val failureReason = reason.trim().ifBlank { DEFAULT_REASON }

    override val embeddingDimension: Int = 0

    override fun generateEmbedding(faceBitmap: Bitmap): Result<FloatArray> {
        return Result.failure(
            IllegalStateException("Face embedding engine unavailable: $failureReason")
        )
    }

    override fun close() = Unit

    private companion object {
        private const val DEFAULT_REASON = "initialization_failed"
    }
}
