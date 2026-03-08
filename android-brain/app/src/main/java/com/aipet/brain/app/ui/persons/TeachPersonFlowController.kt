package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.teachsamples.TeachSampleRecord
import com.aipet.brain.memory.teachsamples.TeachSampleStore
import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal data class TeachPersonCapturedSample(
    val observationId: String,
    val observedAtMs: Long,
    val source: String,
    val note: String?,
    val imageUri: String,
    val faceCropUri: String?,
    val qualityMetadata: SampleQualityMetadata
) {
    val scoredQuality: SampleQualityScore
        get() = scoreSampleQuality(
            faceCropUri = faceCropUri,
            qualityMetadata = qualityMetadata
        )

    val softWarnings: List<SampleQualityWarning>
        get() = deriveSampleWarnings(scoredQuality = scoredQuality)

    val hasSoftWarning: Boolean
        get() = softWarnings.isNotEmpty()
}

internal data class TeachPersonCapturedObservation(
    val observationId: String,
    val observedAtMs: Long,
    val source: String,
    val note: String?,
    val imageUri: String,
    val faceCropUri: String? = null,
    val qualityMetadata: SampleQualityMetadata? = null
)

internal data class TeachPersonUiState(
    val displayName: String = "",
    val nickname: String = "",
    val capturedSamples: List<TeachPersonCapturedSample> = emptyList(),
    val completionConfirmedAtMs: Long? = null,
    val isCapturing: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null
) {
    val qualityGateResult: TeachQualityGateResult
        get() = evaluateTeachQualityGate(capturedSamples)

    val bestSampleSelection: BestSampleSelection
        get() = selectBestSamples(capturedSamples)

    val teachSessionSummary: TeachSessionSummary
        get() = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = qualityGateResult,
            bestSampleSelection = bestSampleSelection
        )

    val pruningSuggestions: TeachSamplePruningSuggestions
        get() = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = bestSampleSelection,
            qualityGateResult = qualityGateResult
        )

    val completionState: TeachSessionCompletionState
        get() = evaluateTeachSessionCompletionState(
            qualityGateResult = qualityGateResult,
            sessionSummary = teachSessionSummary,
            completionConfirmedAtMs = completionConfirmedAtMs
        )
}

internal sealed interface TeachPersonSaveResult {
    data class Success(val personId: String) : TeachPersonSaveResult
    data class ValidationError(val message: String) : TeachPersonSaveResult
    data class Failure(val message: String) : TeachPersonSaveResult
}

internal sealed interface TeachSampleCleanupResult {
    data class Success(
        val removedObservationId: String,
        val updatedCompletionConfirmedAtMs: Long?,
        val completionConfirmationReset: Boolean
    ) : TeachSampleCleanupResult

    data class ValidationError(val message: String) : TeachSampleCleanupResult
    data class Failure(val message: String) : TeachSampleCleanupResult
}

internal class TeachPersonFlowController(
    private val personFlowController: PersonFlowController,
    private val teachSampleStore: TeachSampleStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val sampleIdProvider: () -> String = { UUID.randomUUID().toString() }
) {
    fun observeCapturedSamplesForSession(
        sessionId: String,
        limit: Int
    ): Flow<List<TeachPersonCapturedSample>> {
        return teachSampleStore.observeBySession(
            sessionId = sessionId,
            limit = limit.coerceAtLeast(0)
        ).map { records ->
            records.map { record -> record.toUiSample() }
        }
    }

    suspend fun recordCapturedSampleForSession(
        sessionId: String,
        sample: TeachPersonCapturedObservation
    ): Boolean {
        val normalizedSessionId = sessionId.trim()
        val normalizedObservationId = sample.observationId.trim()
        val normalizedImageUri = sample.imageUri.trim()
        if (normalizedSessionId.isBlank() || normalizedObservationId.isBlank() || normalizedImageUri.isBlank()) {
            return false
        }
        val qualityMetadata = sample.qualityMetadata ?: buildDefaultQualityMetadata(sample)
        return teachSampleStore.insert(
            TeachSampleRecord(
                sampleId = sampleIdProvider(),
                sessionId = normalizedSessionId,
                observationId = normalizedObservationId,
                observedAtMs = sample.observedAtMs,
                source = sample.source,
                note = sample.note,
                imageUri = normalizedImageUri,
                faceCropUri = sample.faceCropUri?.trim()?.ifBlank { null },
                qualityMetadata = qualityMetadata,
                createdAtMs = nowProvider()
            )
        )
    }

    suspend fun saveTaughtPerson(
        displayName: String,
        nickname: String,
        capturedSamples: List<TeachPersonCapturedSample>
    ): TeachPersonSaveResult {
        val qualityGateResult = evaluateTeachQualityGate(capturedSamples)
        if (!qualityGateResult.canSaveTeachPerson) {
            return TeachPersonSaveResult.ValidationError(
                message = qualityGateResult.saveBlockedReason ?: "Teach quality gate is blocking save."
            )
        }

        return when (val personSaveResult = personFlowController.saveTaughtPerson(
            PersonEditorInput(
                displayName = displayName,
                nickname = nickname
            )
        )) {
            is PersonSaveResult.Success -> TeachPersonSaveResult.Success(personSaveResult.personId)
            is PersonSaveResult.ValidationError -> TeachPersonSaveResult.ValidationError(personSaveResult.message)
            is PersonSaveResult.Failure -> TeachPersonSaveResult.Failure(personSaveResult.message)
        }
    }

    suspend fun removeTeachSample(
        sessionId: String,
        observationId: String,
        completionConfirmedAtMs: Long?
    ): TeachSampleCleanupResult {
        val normalizedSessionId = sessionId.trim()
        val normalizedObservationId = observationId.trim()
        if (normalizedSessionId.isBlank() || normalizedObservationId.isBlank()) {
            return TeachSampleCleanupResult.ValidationError(
                message = "Unable to remove sample: missing session or observation identifier."
            )
        }

        return runCatching {
            val removed = teachSampleStore.deleteBySessionAndObservation(
                sessionId = normalizedSessionId,
                observationId = normalizedObservationId
            )
            if (!removed) {
                TeachSampleCleanupResult.Failure(
                    message = "Unable to remove sample: sample does not exist in this teach session."
                )
            } else {
                val completionReset = completionConfirmedAtMs != null
                TeachSampleCleanupResult.Success(
                    removedObservationId = normalizedObservationId,
                    updatedCompletionConfirmedAtMs = null,
                    completionConfirmationReset = completionReset
                )
            }
        }.getOrElse { error ->
            TeachSampleCleanupResult.Failure(
                message = error.message ?: "Unable to remove sample."
            )
        }
    }

    fun getTeachSessionCompletionState(
        capturedSamples: List<TeachPersonCapturedSample>,
        completionConfirmedAtMs: Long?
    ): TeachSessionCompletionState {
        val (qualityGateResult, sessionSummary) = evaluateTeachSession(capturedSamples)
        return evaluateTeachSessionCompletionState(
            qualityGateResult = qualityGateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = completionConfirmedAtMs
        )
    }

    fun confirmTeachSessionCompletion(
        capturedSamples: List<TeachPersonCapturedSample>,
        completionConfirmedAtMs: Long?
    ): TeachSessionCompletionConfirmationResult {
        val (qualityGateResult, sessionSummary) = evaluateTeachSession(capturedSamples)
        return confirmTeachSessionCompletion(
            qualityGateResult = qualityGateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = completionConfirmedAtMs,
            nowMs = nowProvider()
        )
    }

    private fun buildDefaultQualityMetadata(
        sample: TeachPersonCapturedObservation
    ): SampleQualityMetadata {
        val isDebugSource = sample.source.equals("debug", ignoreCase = true)
        val flags = if (isDebugSource) {
            setOf(
                SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                SampleQualityFlag.NOT_CAMERA_FRAME
            )
        } else {
            emptySet()
        }
        return SampleQualityMetadata(
            qualityStatus = if (isDebugSource) {
                SampleQualityStatus.LIMITED_SOURCE
            } else {
                SampleQualityStatus.UNASSESSED
            },
            qualityFlags = flags,
            note = if (isDebugSource) {
                "Debug-generated image reference. True camera-frame quality analysis is not available yet."
            } else {
                "Quality metadata recorded without advanced analysis."
            },
            evaluatedAtMs = nowProvider()
        )
    }

    private fun evaluateTeachSession(
        capturedSamples: List<TeachPersonCapturedSample>
    ): Pair<TeachQualityGateResult, TeachSessionSummary> {
        val qualityGateResult = evaluateTeachQualityGate(capturedSamples)
        val bestSampleSelection = selectBestSamples(capturedSamples)
        val sessionSummary = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = qualityGateResult,
            bestSampleSelection = bestSampleSelection
        )
        return qualityGateResult to sessionSummary
    }

    companion object {
        fun sessionTokenFor(sessionId: String): String {
            return "teach_person_session=$sessionId"
        }
    }
}

private fun TeachSampleRecord.toUiSample(): TeachPersonCapturedSample {
    return TeachPersonCapturedSample(
        observationId = observationId,
        observedAtMs = observedAtMs,
        source = source,
        note = note,
        imageUri = imageUri,
        faceCropUri = faceCropUri,
        qualityMetadata = qualityMetadata
    )
}
