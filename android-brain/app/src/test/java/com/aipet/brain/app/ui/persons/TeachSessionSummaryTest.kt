package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachSessionSummaryTest {
    @Test
    fun deriveTeachSessionSummary_withNoSamples_reflectsBlockedGateAndCounts() {
        val capturedSamples = emptyList<TeachPersonCapturedSample>()
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val selection = selectBestSamples(capturedSamples = capturedSamples)

        val summary = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = gateResult,
            bestSampleSelection = selection
        )

        assertEquals(0, summary.totalSampleCount)
        assertEquals(0, summary.qualifiedSampleCount)
        assertEquals(0, summary.warningSampleCount)
        assertEquals(0, summary.totalWarningCount)
        assertFalse(summary.canSave)
        assertEquals("Capture at least 1 sample before saving.", summary.blockedReason)
        assertNull(summary.preferredSampleId)
        assertFalse(summary.hasWarnings)
    }

    @Test
    fun deriveTeachSessionSummary_withMixedSamples_reflectsPreferredSampleAndWarnings() {
        val failingSample = createSample(
            observationId = "observation-failing",
            observedAtMs = 20_001L,
            source = "DEBUG",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
            qualityFlags = setOf(
                SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                SampleQualityFlag.NOT_CAMERA_FRAME
            )
        )
        val passingSample = createSample(
            observationId = "observation-passing",
            observedAtMs = 20_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/passing-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val anotherFailingSample = createSample(
            observationId = "observation-failing-2",
            observedAtMs = 20_003L,
            source = "CAMERA",
            faceCropUri = null,
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(failingSample, anotherFailingSample, passingSample)
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val selection = selectBestSamples(capturedSamples = capturedSamples)

        val summary = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = gateResult,
            bestSampleSelection = selection
        )

        assertEquals(3, summary.totalSampleCount)
        assertEquals(3, summary.qualifiedSampleCount)
        assertEquals(1, summary.requiredQualifiedSampleCount)
        assertTrue(summary.canSave)
        assertNull(summary.blockedReason)
        assertEquals(2, summary.warningSampleCount)
        assertEquals(failingSample.softWarnings.size + anotherFailingSample.softWarnings.size, summary.totalWarningCount)
        assertTrue(summary.hasWarnings)
        assertEquals("observation-passing", summary.preferredSampleId)
    }

    @Test
    fun deriveTeachSessionSummary_whenGateFails_exposesCurrentGateBlockedReason() {
        // Gate fails only when there are zero samples (minimum count not met).
        val capturedSamples = emptyList<TeachPersonCapturedSample>()
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val selection = selectBestSamples(capturedSamples = capturedSamples)

        val summary = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = gateResult,
            bestSampleSelection = selection
        )

        assertFalse(summary.canSave)
        assertEquals(
            "Capture at least 1 sample before saving.",
            summary.blockedReason
        )
        assertEquals(gateResult.saveBlockedReason, summary.blockedReason)
    }

    @Test
    fun deriveTeachSessionSummary_preferredSampleMatchesSelectionResult() {
        val sampleOlder = createSample(
            observationId = "observation-old",
            observedAtMs = 22_001L,
            source = "CAMERA",
            faceCropUri = "content://sample/old-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val sampleNewer = createSample(
            observationId = "observation-new",
            observedAtMs = 22_002L,
            source = "CAMERA",
            faceCropUri = "content://sample/new-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val sampleOlder2 = createSample(
            observationId = "observation-older",
            observedAtMs = 22_000L,
            source = "CAMERA",
            faceCropUri = "content://sample/older-crop.jpg",
            qualityStatus = SampleQualityStatus.UNASSESSED,
            qualityFlags = emptySet()
        )
        val capturedSamples = listOf(sampleOlder2, sampleOlder, sampleNewer)
        val gateResult = evaluateTeachQualityGate(capturedSamples = capturedSamples)
        val selection = selectBestSamples(capturedSamples = capturedSamples)

        val summary = deriveTeachSessionSummary(
            capturedSamples = capturedSamples,
            qualityGateResult = gateResult,
            bestSampleSelection = selection
        )

        assertEquals(selection.bestSampleId, summary.preferredSampleId)
        assertEquals("observation-new", summary.preferredSampleId)
        assertTrue(summary.canSave)
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
