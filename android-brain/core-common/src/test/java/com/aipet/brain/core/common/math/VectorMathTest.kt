package com.aipet.brain.core.common.math

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class VectorMathTest {

    @Test
    fun l2Normalize_producesUnitLengthForKnownVector() {
        val vector = floatArrayOf(3f, 4f)

        val normalized = VectorMath.l2Normalize(vector)

        assertArrayEquals(floatArrayOf(0.6f, 0.8f), normalized, FLOAT_TOLERANCE)
        val length = sqrt(normalized.sumOf { it * it.toDouble() }).toFloat()
        assertEquals(1f, length, FLOAT_TOLERANCE)
    }

    @Test
    fun cosineSimilarity_identicalVectors_isNearOne() {
        val vector = floatArrayOf(1f, 2f, 3f)

        val similarity = VectorMath.cosineSimilarity(vector, vector)

        assertTrue(similarity > 0.9999f)
    }

    @Test
    fun cosineSimilarity_orthogonalVectors_isNearZero() {
        val a = floatArrayOf(1f, 0f, 0f)
        val b = floatArrayOf(0f, 1f, 0f)

        val similarity = VectorMath.cosineSimilarity(a, b)

        assertEquals(0f, similarity, FLOAT_TOLERANCE)
    }

    @Test
    fun cosineSimilarity_mismatchedDimensions_returnsZeroSafely() {
        val a = floatArrayOf(1f, 2f, 3f)
        val b = floatArrayOf(1f, 2f)

        val similarity = VectorMath.cosineSimilarity(a, b)

        assertEquals(0f, similarity, FLOAT_TOLERANCE)
    }

    @Test
    fun l2Normalize_nearZeroMagnitude_returnsZeroVector() {
        val input = floatArrayOf(0f, 0f, 0f)

        val normalized = VectorMath.l2Normalize(input)

        assertArrayEquals(floatArrayOf(0f, 0f, 0f), normalized, FLOAT_TOLERANCE)
    }

    @Test
    fun computeCentroid_identicalEmbeddings_keepsSameDirection() {
        val embeddings = listOf(
            floatArrayOf(1f, 2f, 3f),
            floatArrayOf(1f, 2f, 3f),
            floatArrayOf(1f, 2f, 3f)
        )

        val centroid = VectorMath.computeCentroid(embeddings)
        val normalizedReference = VectorMath.l2Normalize(floatArrayOf(1f, 2f, 3f))

        assertArrayEquals(normalizedReference, centroid, FLOAT_TOLERANCE)
    }

    @Test
    fun computeCentroid_multipleEmbeddings_returnsExpectedDimension() {
        val embeddings = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )

        val centroid = VectorMath.computeCentroid(embeddings)

        assertEquals(3, centroid.size)
        val centroidLength = sqrt(centroid.sumOf { it * it.toDouble() }).toFloat()
        assertEquals(1f, centroidLength, FLOAT_TOLERANCE)
    }

    @Test
    fun computeCentroid_emptyInput_returnsEmptySafely() {
        val centroid = VectorMath.computeCentroid(emptyList())

        assertEquals(0, centroid.size)
    }

    @Test
    fun computeCentroid_mismatchedDimensions_returnsEmptySafely() {
        val embeddings = listOf(
            floatArrayOf(1f, 2f, 3f),
            floatArrayOf(1f, 2f)
        )

        val centroid = VectorMath.computeCentroid(embeddings)

        assertEquals(0, centroid.size)
    }

    companion object {
        private const val FLOAT_TOLERANCE = 1e-4f
    }
}
