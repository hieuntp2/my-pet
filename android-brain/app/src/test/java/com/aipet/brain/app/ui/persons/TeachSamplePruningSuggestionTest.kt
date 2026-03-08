package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachSamplePruningSuggestionTest {
    @Test
    fun derivePruningSuggestions_marksWeakNonPreferredSampleAsPruningCandidate() {
        val strongPreferredSample = createSample(
            observationId = "observation-strong",
            observedAtMs = 30_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/strong-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val weakSample = createSample(
            observationId = "observation-weak",
            observedAtMs = 30_001L,
            source = "DEBUG",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
            qualityFlags = setOf(
                SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                SampleQualityFlag.NOT_CAMERA_FRAME
            )
        )
        val capturedSamples = listOf(weakSample, strongPreferredSample)

        val selection = selectBestSamples(capturedSamples = capturedSamples)
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = selection,
            qualityGateResult = gateResult
        )

        assertEquals(
            SampleRetentionHint.KEEP,
            pruningSuggestions.suggestionsByObservationId["observation-strong"]?.retentionHint
        )
        assertEquals(
            SampleRetentionHint.REMOVE_SUGGESTED,
            pruningSuggestions.suggestionsByObservationId["observation-weak"]?.retentionHint
        )
        assertTrue(pruningSuggestions.pruningCandidateIds.contains("observation-weak"))
        assertTrue(pruningSuggestions.removeSuggestedIds.contains("observation-weak"))
    }

    @Test
    fun derivePruningSuggestions_doesNotSuggestPreferredSampleForPruning() {
        val sampleOlder = createSample(
            observationId = "observation-old",
            observedAtMs = 31_001L,
            source = "CAMERA",
            faceCropUri = "content://sample/old-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val sampleNewer = createSample(
            observationId = "observation-new",
            observedAtMs = 31_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/new-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(sampleOlder, sampleNewer)

        val selection = selectBestSamples(capturedSamples = capturedSamples)
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = selection,
            qualityGateResult = gateResult
        )
        val preferredSampleId = selection.bestSampleId

        assertEquals("observation-new", preferredSampleId)
        assertEquals(
            SampleRetentionHint.KEEP,
            pruningSuggestions.suggestionsByObservationId[preferredSampleId]?.retentionHint
        )
        assertFalse(pruningSuggestions.pruningCandidateIds.contains(preferredSampleId))
    }

    @Test
    fun derivePruningSuggestions_withEquivalentWeakSamples_usesDeterministicOrder() {
        val strongPreferredSample = createSample(
            observationId = "observation-strong",
            observedAtMs = 32_003L,
            source = "CAMERA",
            faceCropUri = "content://sample/strong-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val weakSampleB = createSample(
            observationId = "observation-b",
            observedAtMs = 32_002L,
            source = "CAMERA",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val weakSampleA = createSample(
            observationId = "observation-a",
            observedAtMs = 32_002L,
            source = "CAMERA",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(weakSampleB, weakSampleA, strongPreferredSample)

        val selection = selectBestSamples(capturedSamples = capturedSamples)
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = selection,
            qualityGateResult = gateResult
        )

        assertEquals(
            listOf("observation-a", "observation-b"),
            pruningSuggestions.pruningCandidateIds
        )
    }

    @Test
    fun derivePruningSuggestions_doesNotChangeHardGateSaveReadiness() {
        val failingSample = createSample(
            observationId = "observation-failing",
            observedAtMs = 33_001L,
            source = "CAMERA",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val passingSample = createSample(
            observationId = "observation-passing",
            observedAtMs = 33_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/passing-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(failingSample, passingSample)

        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val selection = selectBestSamples(capturedSamples = capturedSamples)
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = capturedSamples,
            bestSampleSelection = selection,
            qualityGateResult = gateResult
        )

        assertTrue(gateResult.canSaveTeachPerson)
        assertTrue(pruningSuggestions.pruningCandidateIds.contains("observation-failing"))
        assertFalse(pruningSuggestions.pruningCandidateIds.contains("observation-passing"))
    }
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
