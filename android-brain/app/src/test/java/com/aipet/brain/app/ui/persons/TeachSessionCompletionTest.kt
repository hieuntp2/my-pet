package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachSessionCompletionTest {
    @Test
    fun evaluateTeachSessionCompletionState_whenHardGateFails_isBlocked() {
        val lowSample = createSample(
            observationId = "observation-low",
            observedAtMs = 40_001L,
            source = "CAMERA",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val (gateResult, sessionSummary) = buildSessionState(capturedSamples = listOf(lowSample))

        val completionState = evaluateTeachSessionCompletionState(
            qualityGateResult = gateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = null
        )

        assertFalse(completionState.isReadyToComplete)
        assertFalse(completionState.isCompleted)
        assertEquals(TeachSessionCompletionStatus.BLOCKED, completionState.status)
        assertEquals(
            "Capture at least one sample with a face crop and MEDIUM/HIGH quality level before saving.",
            completionState.completionBlockedReason
        )

        val completionResult = confirmTeachSessionCompletion(
            qualityGateResult = gateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = null,
            nowMs = 90_000L
        )
        assertTrue(completionResult is TeachSessionCompletionConfirmationResult.Blocked)
    }

    @Test
    fun evaluateTeachSessionCompletionState_whenHardGatePasses_isReadyToComplete() {
        val qualifiedSample = createSample(
            observationId = "observation-qualified",
            observedAtMs = 41_001L,
            source = "CAMERA",
            faceCropUri = "content://sample/qualified-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val (gateResult, sessionSummary) = buildSessionState(capturedSamples = listOf(qualifiedSample))

        val completionState = evaluateTeachSessionCompletionState(
            qualityGateResult = gateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = null
        )

        assertTrue(completionState.isReadyToComplete)
        assertFalse(completionState.isCompleted)
        assertEquals(TeachSessionCompletionStatus.READY_TO_COMPLETE, completionState.status)
        assertEquals(null, completionState.completionBlockedReason)
    }

    @Test
    fun confirmTeachSessionCompletion_whenReady_transitionsToCompleted() {
        val qualifiedSample = createSample(
            observationId = "observation-complete",
            observedAtMs = 42_001L,
            source = "CAMERA",
            faceCropUri = "content://sample/complete-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val (gateResult, sessionSummary) = buildSessionState(capturedSamples = listOf(qualifiedSample))

        val completionResult = confirmTeachSessionCompletion(
            qualityGateResult = gateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = null,
            nowMs = 99_000L
        )

        assertTrue(completionResult is TeachSessionCompletionConfirmationResult.Confirmed)
        val completedState = (completionResult as TeachSessionCompletionConfirmationResult.Confirmed).completionState
        assertEquals(TeachSessionCompletionStatus.COMPLETED, completedState.status)
        assertTrue(completedState.isCompleted)
        assertTrue(completedState.isReadyToComplete)
        assertEquals(99_000L, completedState.completedAtMs)
    }

    @Test
    fun confirmTeachSessionCompletion_pruningSuggestionsRemainAdvisoryAndDoNotBlockCompletion() {
        val weakSample = createSample(
            observationId = "observation-weak",
            observedAtMs = 43_001L,
            source = "DEBUG",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
            qualityFlags = setOf(
                SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                SampleQualityFlag.NOT_CAMERA_FRAME
            )
        )
        val qualifiedSample = createSample(
            observationId = "observation-strong",
            observedAtMs = 43_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/strong-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(weakSample, qualifiedSample)
        val (gateResult, sessionSummary) = buildSessionState(capturedSamples = capturedSamples)
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = selectBestSamples(capturedSamples),
            qualityGateResult = gateResult
        )

        assertTrue(gateResult.canSaveTeachPerson)
        assertTrue(pruningSuggestions.hasPruningCandidates)
        val completionResult = confirmTeachSessionCompletion(
            qualityGateResult = gateResult,
            sessionSummary = sessionSummary,
            completionConfirmedAtMs = null,
            nowMs = 100_000L
        )

        assertTrue(completionResult is TeachSessionCompletionConfirmationResult.Confirmed)
    }
}

private fun buildSessionState(
    capturedSamples: List<TeachPersonCapturedSample>
): Pair<TeachQualityGateResult, TeachSessionSummary> {
    val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
    val selection = selectBestSamples(capturedSamples = capturedSamples)
    val summary = deriveTeachSessionSummary(
        capturedSamples = capturedSamples,
        qualityGateResult = gateResult,
        bestSampleSelection = selection
    )
    return gateResult to summary
}

private fun createSample(
    observationId: String,
    observedAtMs: Long,
    source: String,
    faceCropUri: String?,
    qualityStatus: SampleQualityStatus,
    qualityFlags: Set<SampleQualityFlag>
): TeachPersonCapturedSample {
    return TeachPersonCapturedSample(
        observationId = observationId,
        observedAtMs = observedAtMs,
        source = source,
        note = null,
        imageUri = "content://sample/$observationId.jpg",
        faceCropUri = faceCropUri,
        qualityMetadata = SampleQualityMetadata(
            qualityStatus = qualityStatus,
            qualityFlags = qualityFlags,
            note = null,
            evaluatedAtMs = observedAtMs
        )
    )
}
