package com.aipet.brain.brain.recognition

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonRecognizedPayload
import com.aipet.brain.brain.events.PersonUnknownPayload
import com.aipet.brain.brain.recognition.model.RecognitionClassification
import com.aipet.brain.brain.recognition.model.RecognitionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecognitionDecisionEventPublisher(
    private val eventBus: EventBus,
    private val recognitionMemoryStatsUpdater: RecognitionMemoryStatsUpdater = RecognitionMemoryStatsUpdater.NoOp,
    private val updateLogger: (String) -> Unit = {}
) {
    private val statsUpdateLock = Mutex()
    private var lastProcessedRecognitionKey: String? = null

    suspend fun publish(
        recognitionResult: RecognitionResult
    ) {
        val acceptedPersonId = recognitionResult.bestPersonId
            ?.trim()
            ?.ifBlank { null }
        val eventTimestamp = recognitionResult.timestamp

        if (
            recognitionResult.classification == RecognitionClassification.RECOGNIZED &&
            recognitionResult.accepted &&
            acceptedPersonId != null
        ) {
            val statsUpdateResult = updatePersonSeenStatsIfNeeded(
                personId = acceptedPersonId,
                timestampMs = eventTimestamp
            )
            updateLogger(
                "Recognition accepted: personId=$acceptedPersonId, " +
                    "score=${recognitionResult.bestScore}, timestamp=$eventTimestamp, " +
                    "statsUpdated=${statsUpdateResult != null}, " +
                    "seenCount=${statsUpdateResult?.seenCount ?: "unknown"}"
            )
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.PERSON_RECOGNIZED,
                    timestampMs = eventTimestamp,
                    payloadJson = PersonRecognizedPayload(
                        personId = acceptedPersonId,
                        similarityScore = recognitionResult.bestScore,
                        threshold = recognitionResult.threshold,
                        evaluatedCandidates = recognitionResult.evaluatedCandidates,
                        timestamp = eventTimestamp
                    ).toJson()
                )
            )
            return
        }

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN,
                timestampMs = eventTimestamp,
                payloadJson = PersonUnknownPayload(
                    bestScore = recognitionResult.bestScore,
                    threshold = recognitionResult.threshold,
                    evaluatedCandidates = recognitionResult.evaluatedCandidates,
                    timestamp = eventTimestamp
                ).toJson()
            )
        )
        updateLogger(
            "Recognition unknown: reason=${resolveUnknownReason(recognitionResult)}, " +
                "bestScore=${recognitionResult.bestScore}, threshold=${recognitionResult.threshold}, " +
                "timestamp=$eventTimestamp, publishedEvent=${EventType.PERSON_UNKNOWN.name}"
        )
    }

    private suspend fun updatePersonSeenStatsIfNeeded(
        personId: String,
        timestampMs: Long
    ): RecognitionMemoryStatsUpdate? {
        val dedupeKey = "$personId|$timestampMs"
        val updateResult = statsUpdateLock.withLock {
            if (lastProcessedRecognitionKey == dedupeKey) {
                updateLogger(
                    "Recognition-memory update skipped (duplicate). personId=$personId, timestamp=$timestampMs"
                )
                return@withLock null
            }
            val result = recognitionMemoryStatsUpdater.updatePersonSeenStats(
                personId = personId,
                timestampMs = timestampMs
            )
            lastProcessedRecognitionKey = dedupeKey
            result
        }

        val wasUpdated = updateResult != null
        updateLogger(
            "Recognition-memory update: personId=$personId, timestamp=$timestampMs, " +
                "seenCount=${updateResult?.seenCount ?: "unknown"}, updated=$wasUpdated"
        )
        return updateResult
    }

    private fun resolveUnknownReason(result: RecognitionResult): String {
        return when {
            result.evaluatedCandidates <= 0 -> "no_candidates"
            result.bestScore < result.threshold -> "below_threshold"
            result.classification != RecognitionClassification.UNKNOWN -> "non_unknown_classification"
            else -> "safe_fallback"
        }
    }
}
