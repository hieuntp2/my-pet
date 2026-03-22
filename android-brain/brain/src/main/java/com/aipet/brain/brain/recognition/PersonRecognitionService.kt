package com.aipet.brain.brain.recognition

import com.aipet.brain.brain.recognition.model.KnownPersonEmbeddings
import com.aipet.brain.brain.recognition.model.RecognitionClassification
import com.aipet.brain.brain.recognition.model.RecognitionResult
import com.aipet.brain.brain.recognition.model.RecognitionThresholdConfig
import com.aipet.brain.core.common.math.VectorMath

class PersonRecognitionService(
    private val knownPersonEmbeddingsSource: KnownPersonEmbeddingsSource,
    private val thresholdConfig: RecognitionThresholdConfig = RecognitionThresholdConfig(),
    private val decisionLogger: (String) -> Unit = {},
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun recognize(currentEmbedding: FloatArray): RecognitionResult {
        return recognizeInternal(
            currentEmbedding = currentEmbedding,
            threshold = thresholdConfig.normalizedAcceptanceThreshold
        )
    }

    suspend fun recognizeWithThreshold(
        currentEmbedding: FloatArray,
        acceptanceThreshold: Float
    ): RecognitionResult {
        val threshold = if (acceptanceThreshold.isFinite()) {
            acceptanceThreshold.coerceIn(-1f, 1f)
        } else {
            thresholdConfig.normalizedAcceptanceThreshold
        }
        return recognizeInternal(
            currentEmbedding = currentEmbedding,
            threshold = threshold
        )
    }

    private suspend fun recognizeInternal(
        currentEmbedding: FloatArray,
        threshold: Float
    ): RecognitionResult {
        val decisionTimestamp = nowProvider()
        if (!currentEmbedding.isValidRawEmbedding()) {
            return createUnknownResult(
                threshold = threshold,
                evaluatedCandidates = 0,
                timestamp = decisionTimestamp
            ).also { result ->
                logDecision(result)
            }
        }

        val normalizedCurrentEmbedding = VectorMath.l2Normalize(currentEmbedding)
        if (normalizedCurrentEmbedding.isZeroVector()) {
            return createUnknownResult(
                threshold = threshold,
                evaluatedCandidates = 0,
                timestamp = decisionTimestamp
            ).also { result ->
                logDecision(result)
            }
        }

        val knownPersons = knownPersonEmbeddingsSource.loadKnownPersonEmbeddings()
        if (knownPersons.isEmpty()) {
            return createUnknownResult(
                threshold = threshold,
                evaluatedCandidates = 0,
                timestamp = decisionTimestamp
            ).also { result ->
                logDecision(result)
            }
        }

        var evaluatedCandidates = 0
        var bestCandidate: ScoredCandidate? = null

        for (knownPerson in knownPersons) {
            val candidate = scorePersonCandidate(
                knownPerson = knownPerson,
                normalizedCurrentEmbedding = normalizedCurrentEmbedding
            )

            if (candidate == null) {
                continue
            }

            evaluatedCandidates += 1
            if (bestCandidate == null || candidate.similarityScore > bestCandidate.similarityScore) {
                bestCandidate = candidate
            }
        }

        val result = if (bestCandidate == null) {
            createUnknownResult(
                threshold = threshold,
                evaluatedCandidates = evaluatedCandidates,
                timestamp = decisionTimestamp
            )
        } else {
            val bestScore = bestCandidate.similarityScore.safeScore()
            val accepted = bestScore >= threshold
            val classification = if (accepted) {
                RecognitionClassification.RECOGNIZED
            } else {
                RecognitionClassification.UNKNOWN
            }
            RecognitionResult(
                classification = classification,
                bestPersonId = if (classification == RecognitionClassification.RECOGNIZED) {
                    bestCandidate.personId
                } else {
                    null
                },
                bestScore = bestScore,
                threshold = threshold,
                accepted = accepted,
                evaluatedCandidates = evaluatedCandidates,
                timestamp = decisionTimestamp
            )
        }
        logDecision(result)
        return result
    }

    private fun scorePersonCandidate(
        knownPerson: KnownPersonEmbeddings,
        normalizedCurrentEmbedding: FloatArray
    ): ScoredCandidate? {
        val normalizedPersonId = knownPerson.personId.trim()
        if (normalizedPersonId.isBlank()) {
            return null
        }

        val centroidAggregation = EmbeddingCentroidAggregator.aggregate(
            embeddings = knownPerson.embeddings.map { stored -> stored.values },
            expectedDimension = normalizedCurrentEmbedding.size
        ) ?: return null

        val similarity = VectorMath.cosineSimilarity(
            a = normalizedCurrentEmbedding,
            b = centroidAggregation.centroid
        ).safeScore()

        decisionLogger(
            "Centroid comparison: personId=$normalizedPersonId, " +
                "sampleCount=${centroidAggregation.sampleCount}, similarity=$similarity"
        )

        return ScoredCandidate(
            personId = normalizedPersonId,
            similarityScore = similarity
        )
    }

    private fun createUnknownResult(
        threshold: Float,
        evaluatedCandidates: Int,
        timestamp: Long
    ): RecognitionResult {
        return RecognitionResult(
            classification = RecognitionClassification.UNKNOWN,
            bestPersonId = null,
            bestScore = 0f,
            threshold = threshold,
            accepted = false,
            evaluatedCandidates = evaluatedCandidates,
            timestamp = timestamp
        )
    }

    private fun logDecision(result: RecognitionResult) {
        decisionLogger(
            "Recognition decision: classification=${result.classification.name}, " +
                "bestScore=${result.bestScore}, threshold=${result.threshold}, " +
                "evaluatedCandidates=${result.evaluatedCandidates}, accepted=${result.accepted}, " +
                "bestPersonId=${result.bestPersonId ?: "none"}"
        )
    }
}

private data class ScoredCandidate(
    val personId: String,
    val similarityScore: Float
)

private fun FloatArray.isValidRawEmbedding(): Boolean {
    if (isEmpty()) {
        return false
    }
    return all { value ->
        value.isFinite()
    }
}

private fun FloatArray.isZeroVector(): Boolean {
    return all { value ->
        value == 0f
    }
}

private fun Float.safeScore(): Float {
    return if (isFinite()) {
        coerceIn(-1f, 1f)
    } else {
        0f
    }
}
