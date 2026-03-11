package com.aipet.brain.brain.recognition

import com.aipet.brain.core.common.math.VectorMath

data class EmbeddingCentroidAggregation(
    val centroid: FloatArray,
    val sampleCount: Int
)

object EmbeddingCentroidAggregator {
    fun aggregate(
        embeddings: List<FloatArray>,
        expectedDimension: Int
    ): EmbeddingCentroidAggregation? {
        if (expectedDimension <= 0 || embeddings.isEmpty()) {
            return null
        }

        val sum = DoubleArray(expectedDimension)
        var sampleCount = 0

        embeddings.forEach { embedding ->
            if (embedding.size != expectedDimension) {
                return@forEach
            }
            if (!embedding.all { value -> value.isFinite() }) {
                return@forEach
            }

            val normalizedEmbedding = VectorMath.l2Normalize(embedding)
            if (normalizedEmbedding.isZeroVector()) {
                return@forEach
            }

            for (index in normalizedEmbedding.indices) {
                sum[index] += normalizedEmbedding[index].toDouble()
            }
            sampleCount += 1
        }

        if (sampleCount == 0) {
            return null
        }

        val averagedCentroid = FloatArray(expectedDimension) { index ->
            (sum[index] / sampleCount).toFloat()
        }
        val normalizedCentroid = VectorMath.l2Normalize(averagedCentroid)
        if (normalizedCentroid.isZeroVector()) {
            return null
        }

        return EmbeddingCentroidAggregation(
            centroid = normalizedCentroid,
            sampleCount = sampleCount
        )
    }
}

private fun FloatArray.isZeroVector(): Boolean {
    return all { value ->
        value == 0f
    }
}
