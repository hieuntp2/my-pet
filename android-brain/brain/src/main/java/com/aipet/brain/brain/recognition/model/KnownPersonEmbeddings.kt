package com.aipet.brain.brain.recognition.model

data class KnownPersonEmbeddings(
    val personId: String,
    val displayName: String,
    val embeddings: List<KnownFaceEmbedding>
)

data class KnownFaceEmbedding(
    val embeddingId: String,
    val values: FloatArray
)
