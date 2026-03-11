package com.aipet.brain.perception.vision.face.embedding

import android.graphics.Bitmap

interface FaceEmbeddingEngine : AutoCloseable {
    val embeddingDimension: Int

    fun generateEmbedding(faceBitmap: Bitmap): Result<FloatArray>
}
