package com.aipet.brain.brain.recognition

import com.aipet.brain.brain.recognition.model.KnownFaceEmbedding
import com.aipet.brain.brain.recognition.model.KnownPersonEmbeddings
import com.aipet.brain.brain.recognition.model.RecognitionClassification
import com.aipet.brain.brain.recognition.model.RecognitionThresholdConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PersonRecognitionServiceTest {

    @Test
    fun recognize_emptyCurrentEmbedding_returnsUnknownWithZeroCandidates() = runTest {
        val service = PersonRecognitionService(
            knownPersonEmbeddingsSource = FakeKnownPersonEmbeddingsSource(emptyList()),
            nowProvider = { 1000L }
        )

        val result = service.recognize(floatArrayOf())

        assertNull(result.bestPersonId)
        assertEquals(0f, result.bestScore, 0f)
        assertEquals(false, result.accepted)
        assertEquals(RecognitionClassification.UNKNOWN, result.classification)
        assertEquals(0, result.evaluatedCandidates)
        assertEquals(1000L, result.timestamp)
    }

    @Test
    fun recognize_noKnownPersons_returnsUnknownWithZeroCandidates() = runTest {
        val service = PersonRecognitionService(
            knownPersonEmbeddingsSource = FakeKnownPersonEmbeddingsSource(emptyList()),
            nowProvider = { 2000L }
        )

        val result = service.recognize(floatArrayOf(0.5f, 0.5f))

        assertEquals(RecognitionClassification.UNKNOWN, result.classification)
        assertEquals(false, result.accepted)
        assertEquals(0, result.evaluatedCandidates)
        assertEquals(2000L, result.timestamp)
    }

    @Test
    fun recognize_scoresCentroidsAndReturnsRecognizedResult() = runTest {
        val knownPersons = listOf(
            KnownPersonEmbeddings(
                personId = "person-a",
                displayName = "Alice",
                embeddings = listOf(
                    KnownFaceEmbedding("a-1", floatArrayOf(1f, 0f, 0f)),
                    KnownFaceEmbedding("a-2", floatArrayOf(0.95f, 0.05f, 0f))
                )
            ),
            KnownPersonEmbeddings(
                personId = "person-b",
                displayName = "Bob",
                embeddings = listOf(
                    KnownFaceEmbedding("b-1", floatArrayOf(0f, 1f, 0f)),
                    KnownFaceEmbedding("b-2", floatArrayOf(0f, 0.9f, 0.1f))
                )
            )
        )
        val service = PersonRecognitionService(
            knownPersonEmbeddingsSource = FakeKnownPersonEmbeddingsSource(knownPersons),
            nowProvider = { 3000L }
        )

        val result = service.recognize(floatArrayOf(0.98f, 0.02f, 0f))

        assertEquals(RecognitionClassification.RECOGNIZED, result.classification)
        assertEquals(true, result.accepted)
        assertEquals(2, result.evaluatedCandidates)
        assertEquals("person-a", result.bestPersonId)
        assertTrue(result.bestScore > 0.9f)
        assertEquals(3000L, result.timestamp)
    }

    @Test
    fun recognize_personsWithNoUsableEmbeddings_returnsUnknownWithoutCrash() = runTest {
        val knownPersons = listOf(
            KnownPersonEmbeddings(
                personId = "person-no-embeddings",
                displayName = "NoEmbeddings",
                embeddings = emptyList()
            ),
            KnownPersonEmbeddings(
                personId = "person-mismatch",
                displayName = "Mismatch",
                embeddings = listOf(
                    KnownFaceEmbedding("m-1", floatArrayOf(0.1f, 0.2f))
                )
            ),
            KnownPersonEmbeddings(
                personId = "person-invalid",
                displayName = "Invalid",
                embeddings = listOf(
                    KnownFaceEmbedding("i-1", floatArrayOf(Float.NaN, 0.2f, 0.3f))
                )
            )
        )
        val service = PersonRecognitionService(
            knownPersonEmbeddingsSource = FakeKnownPersonEmbeddingsSource(knownPersons),
            nowProvider = { 4000L }
        )

        val result = service.recognize(floatArrayOf(0.2f, 0.3f, 0.4f))

        assertEquals(RecognitionClassification.UNKNOWN, result.classification)
        assertEquals(false, result.accepted)
        assertEquals(0, result.evaluatedCandidates)
        assertNull(result.bestPersonId)
        assertEquals(4000L, result.timestamp)
    }

    @Test
    fun recognize_belowThreshold_classifiesAsUnknown() = runTest {
        val knownPersons = listOf(
            KnownPersonEmbeddings(
                personId = "person-a",
                displayName = "Alice",
                embeddings = listOf(
                    KnownFaceEmbedding("a-1", floatArrayOf(1f, 0f, 0f))
                )
            )
        )
        val service = PersonRecognitionService(
            knownPersonEmbeddingsSource = FakeKnownPersonEmbeddingsSource(knownPersons),
            thresholdConfig = RecognitionThresholdConfig(acceptanceThreshold = 0.95f),
            nowProvider = { 5000L }
        )

        val result = service.recognize(floatArrayOf(0.75f, 0.66f, 0f))

        assertEquals(RecognitionClassification.UNKNOWN, result.classification)
        assertEquals(false, result.accepted)
        assertNull(result.bestPersonId)
        assertEquals(1, result.evaluatedCandidates)
        assertTrue(result.bestScore < result.threshold)
        assertEquals(5000L, result.timestamp)
    }
}

private class FakeKnownPersonEmbeddingsSource(
    private val knownPersons: List<KnownPersonEmbeddings>
) : KnownPersonEmbeddingsSource {
    override suspend fun loadKnownPersonEmbeddings(): List<KnownPersonEmbeddings> {
        return knownPersons
    }
}
