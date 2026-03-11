package com.aipet.brain.core.common.math

import kotlin.math.sqrt

object VectorMath {
    fun computeCentroid(
        embeddings: List<FloatArray>,
        epsilon: Float = DEFAULT_EPSILON
    ): FloatArray {
        if (embeddings.isEmpty()) {
            return FloatArray(0)
        }
        val dimension = embeddings.first().size
        if (dimension == 0) {
            return FloatArray(0)
        }

        val sum = DoubleArray(dimension)
        for (embedding in embeddings) {
            if (embedding.size != dimension) {
                return FloatArray(0)
            }
            for (index in embedding.indices) {
                val value = embedding[index]
                if (!value.isFinite()) {
                    return FloatArray(dimension)
                }
                sum[index] += value.toDouble()
            }
        }

        val centroid = FloatArray(dimension) { index ->
            (sum[index] / embeddings.size).toFloat()
        }
        return l2Normalize(centroid, epsilon)
    }

    fun l2Normalize(vector: FloatArray, epsilon: Float = DEFAULT_EPSILON): FloatArray {
        if (vector.isEmpty()) {
            return FloatArray(0)
        }
        if (epsilon <= 0f || !epsilon.isFinite()) {
            return FloatArray(vector.size)
        }

        var squaredSum = 0.0
        for (value in vector) {
            if (!value.isFinite()) {
                return FloatArray(vector.size)
            }
            squaredSum += value * value
        }

        val magnitude = sqrt(squaredSum).toFloat()
        if (!magnitude.isFinite() || magnitude <= epsilon) {
            return FloatArray(vector.size)
        }

        return FloatArray(vector.size) { index ->
            val normalized = vector[index] / magnitude
            if (normalized.isFinite()) normalized else 0f
        }
    }

    fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
        epsilon: Float = DEFAULT_EPSILON
    ): Float {
        if (a.size != b.size || a.isEmpty()) {
            return 0f
        }

        val normalizedA = l2Normalize(a, epsilon)
        val normalizedB = l2Normalize(b, epsilon)
        if (normalizedA.all { it == 0f } || normalizedB.all { it == 0f }) {
            return 0f
        }

        var dotProduct = 0.0
        for (index in normalizedA.indices) {
            dotProduct += normalizedA[index] * normalizedB[index]
        }

        if (!dotProduct.isFinite()) {
            return 0f
        }

        return dotProduct.toFloat().coerceIn(-1f, 1f)
    }

    private const val DEFAULT_EPSILON = 1e-12f
}
