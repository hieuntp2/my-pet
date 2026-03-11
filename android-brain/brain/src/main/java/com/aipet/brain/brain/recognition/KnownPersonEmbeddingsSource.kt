package com.aipet.brain.brain.recognition

import com.aipet.brain.brain.recognition.model.KnownPersonEmbeddings

interface KnownPersonEmbeddingsSource {
    suspend fun loadKnownPersonEmbeddings(): List<KnownPersonEmbeddings>
}
