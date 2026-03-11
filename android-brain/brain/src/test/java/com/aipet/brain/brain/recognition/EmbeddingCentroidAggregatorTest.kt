package com.aipet.brain.brain.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingCentroidAggregatorTest {

    @Test
    fun aggregate_validEmbeddings_returnsNormalizedCentroid() {
        val result = EmbeddingCentroidAggregator.aggregate(
            embeddings = listOf(
                floatArrayOf(1f, 0f),
                floatArrayOf(0f, 1f)
            ),
            expectedDimension = 2
        )

        assertNotNull(result)
        assertEquals(2, result?.sampleCount)
        val centroid = result?.centroid ?: FloatArray(0)
        assertEquals(2, centroid.size)
        assertTrue(centroid[0] > 0.7f)
        assertTrue(centroid[1] > 0.7f)
    }

    @Test
    fun aggregate_ignoresInvalidAndMismatchedEmbeddings() {
        val result = EmbeddingCentroidAggregator.aggregate(
            embeddings = listOf(
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(Float.NaN, 0f, 0f),
                floatArrayOf(1f, 0f),
                floatArrayOf()
            ),
            expectedDimension = 3
        )

        assertNotNull(result)
        assertEquals(1, result?.sampleCount)
        val centroid = result?.centroid ?: FloatArray(0)
        assertEquals(3, centroid.size)
        assertTrue(centroid[0] > 0.99f)
        assertEquals(0f, centroid[1], 0f)
        assertEquals(0f, centroid[2], 0f)
    }

    @Test
    fun aggregate_singleSample_centroidMatchesSample() {
        val result = EmbeddingCentroidAggregator.aggregate(
            embeddings = listOf(floatArrayOf(0f, 1f, 0f)),
            expectedDimension = 3
        )

        assertNotNull(result)
        assertEquals(1, result?.sampleCount)
        val centroid = result?.centroid ?: FloatArray(0)
        assertEquals(3, centroid.size)
        assertEquals(0f, centroid[0], 0f)
        assertTrue(centroid[1] > 0.99f)
        assertEquals(0f, centroid[2], 0f)
    }

    @Test
    fun aggregate_noValidEmbeddings_returnsNull() {
        val result = EmbeddingCentroidAggregator.aggregate(
            embeddings = listOf(
                floatArrayOf(Float.NaN, 1f),
                floatArrayOf(1f),
                floatArrayOf()
            ),
            expectedDimension = 2
        )

        assertNull(result)
    }
}
