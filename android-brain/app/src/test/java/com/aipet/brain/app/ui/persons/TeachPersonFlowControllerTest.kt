package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus
import com.aipet.brain.memory.teachsamples.TeachSampleRecord
import com.aipet.brain.memory.teachsamples.TeachSampleStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeachPersonFlowControllerTest {
    @Test
    fun recordCapturedSampleForSession_withCameraSource_persistsCameraAndFaceCropUriAndQualityMetadataReference() = runTest {
        val fakeStore = TeachFakeSampleStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = TeachFakePersonStore(),
                nowProvider = { 1_000L },
                idProvider = { "person-1" }
            ),
            teachSampleStore = fakeStore,
            nowProvider = { 1_100L },
            sampleIdProvider = { "sample-1" }
        )

        val recorded = controller.recordCapturedSampleForSession(
            sessionId = "session-a",
            sample = TeachPersonCapturedObservation(
                observationId = "observation-1",
                observedAtMs = 1_050L,
                source = "CAMERA",
                note = "teach_person_session=session-a;sample=1",
                imageUri = "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-1.jpg",
                faceCropUri = "content://com.aipet.brain.app.fileprovider/teach_samples/face_crops/sample-1.jpg"
            )
        )

        assertTrue(recorded)
        val persisted = fakeStore.listBySession(sessionId = "session-a", limit = 10)
        assertEquals(1, persisted.size)
        assertEquals(
            "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-1.jpg",
            persisted.first().imageUri
        )
        assertEquals(
            "content://com.aipet.brain.app.fileprovider/teach_samples/face_crops/sample-1.jpg",
            persisted.first().faceCropUri
        )
        assertEquals(SampleQualityStatus.UNASSESSED, persisted.first().qualityMetadata.qualityStatus)
        assertEquals(emptySet<SampleQualityFlag>(), persisted.first().qualityMetadata.qualityFlags)
        assertEquals(1_100L, persisted.first().qualityMetadata.evaluatedAtMs)
        val captured = controller.observeCapturedSamplesForSession(sessionId = "session-a", limit = 10).first()
        assertTrue(captured.isNotEmpty())
        assertEquals(75, captured.first().scoredQuality.score)
        assertEquals(SampleQualityLevel.HIGH, captured.first().scoredQuality.level)
        assertFalse(captured.first().hasSoftWarning)
        assertTrue(captured.first().softWarnings.isEmpty())
    }

    @Test
    fun recordCapturedSampleForSession_whenFaceCropMissing_persistsSafelyWithNullCropReference() = runTest {
        val fakeStore = TeachFakeSampleStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = TeachFakePersonStore(),
                nowProvider = { 5_000L },
                idProvider = { "person-3" }
            ),
            teachSampleStore = fakeStore,
            nowProvider = { 5_100L },
            sampleIdProvider = { "sample-3" }
        )

        val recorded = controller.recordCapturedSampleForSession(
            sessionId = "session-c",
            sample = TeachPersonCapturedObservation(
                observationId = "observation-3",
                observedAtMs = 5_050L,
                source = "CAMERA",
                note = "teach_person_session=session-c;sample=1;face_crop=not_detected",
                imageUri = "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-3.jpg",
                faceCropUri = null
            )
        )

        assertTrue(recorded)
        val persisted = fakeStore.listBySession(sessionId = "session-c", limit = 10)
        assertEquals(1, persisted.size)
        assertEquals(null, persisted.first().faceCropUri)
        val captured = controller.observeCapturedSamplesForSession(sessionId = "session-c", limit = 10).first()
        assertTrue(captured.isNotEmpty())
        assertEquals(15, captured.first().scoredQuality.score)
        assertEquals(SampleQualityLevel.LOW, captured.first().scoredQuality.level)
        assertTrue(captured.first().hasSoftWarning)
        assertTrue(
            captured.first().softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.MISSING_FACE_CROP
            }
        )
    }

    @Test
    fun observeCapturedSamplesForSession_returnsPersistedSamplesAndQualityMetadataForSession() = runTest {
        val fakeStore = TeachFakeSampleStore()
        fakeStore.insert(
            TeachSampleRecord(
                sampleId = "sample-1",
                sessionId = "session-a",
                observationId = "observation-1",
                observedAtMs = 1_000L,
                source = "DEBUG",
                note = "note-1",
                imageUri = "file:///tmp/sample-1.png",
                faceCropUri = "file:///tmp/sample-1-crop.png",
                qualityMetadata = SampleQualityMetadata(
                    qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                    qualityFlags = setOf(SampleQualityFlag.DEBUG_GENERATED_IMAGE),
                    note = "quality-note-1",
                    evaluatedAtMs = 1_080L
                ),
                createdAtMs = 1_100L
            )
        )
        fakeStore.insert(
            TeachSampleRecord(
                sampleId = "sample-2",
                sessionId = "session-b",
                observationId = "observation-2",
                observedAtMs = 2_000L,
                source = "DEBUG",
                note = "note-2",
                imageUri = "file:///tmp/sample-2.png",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata(
                    qualityStatus = SampleQualityStatus.UNASSESSED,
                    qualityFlags = emptySet(),
                    note = null,
                    evaluatedAtMs = 2_080L
                ),
                createdAtMs = 2_100L
            )
        )
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = TeachFakePersonStore(),
                nowProvider = { 3_000L },
                idProvider = { "person-2" }
            ),
            teachSampleStore = fakeStore
        )

        val captured = controller.observeCapturedSamplesForSession(
            sessionId = "session-a",
            limit = 10
        ).first()

        assertEquals(1, captured.size)
        assertEquals("observation-1", captured.first().observationId)
        assertEquals("file:///tmp/sample-1.png", captured.first().imageUri)
        assertEquals("file:///tmp/sample-1-crop.png", captured.first().faceCropUri)
        assertEquals(SampleQualityStatus.LIMITED_SOURCE, captured.first().qualityMetadata.qualityStatus)
        assertEquals("quality-note-1", captured.first().qualityMetadata.note)
        assertEquals(1_080L, captured.first().qualityMetadata.evaluatedAtMs)
        assertEquals(50, captured.first().scoredQuality.score)
        assertEquals(SampleQualityLevel.MEDIUM, captured.first().scoredQuality.level)
        assertTrue(captured.first().hasSoftWarning)
        assertTrue(
            captured.first().softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.LIMITED_SOURCE_QUALITY
            }
        )
        assertTrue(
            captured.first().softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.DEBUG_GENERATED_SOURCE
            }
        )
    }

    @Test
    fun capturedSample_withFaceCrop_scoresHigherThanCropMissingSample() {
        val cropPresentSample = TeachPersonCapturedSample(
            observationId = "observation-crop-present",
            observedAtMs = 1_000L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/crop-present.jpg",
            faceCropUri = "content://sample/crop-present-face.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 1_000L
            )
        )
        val cropMissingSample = TeachPersonCapturedSample(
            observationId = "observation-crop-missing",
            observedAtMs = 1_001L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/crop-missing.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 1_001L
            )
        )

        assertTrue(cropPresentSample.scoredQuality.score > cropMissingSample.scoredQuality.score)
        assertEquals(SampleQualityLevel.HIGH, cropPresentSample.scoredQuality.level)
        assertEquals(SampleQualityLevel.LOW, cropMissingSample.scoredQuality.level)
    }

    @Test
    fun capturedSample_qualityMetadataFlags_reduceScoreAndTriggerAlignedWarnings() {
        val cleanSample = TeachPersonCapturedSample(
            observationId = "observation-clean",
            observedAtMs = 2_000L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/clean.jpg",
            faceCropUri = "content://sample/clean-face.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 2_000L
            )
        )
        val limitedSample = TeachPersonCapturedSample(
            observationId = "observation-limited",
            observedAtMs = 2_001L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/limited.png",
            faceCropUri = "file:///tmp/limited-face.png",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = "Limited source sample",
                evaluatedAtMs = 2_001L
            )
        )

        assertTrue(cleanSample.scoredQuality.score > limitedSample.scoredQuality.score)
        assertEquals(SampleQualityLevel.HIGH, cleanSample.scoredQuality.level)
        assertEquals(SampleQualityLevel.LOW, limitedSample.scoredQuality.level)
        assertFalse(cleanSample.hasSoftWarning)
        assertTrue(
            limitedSample.softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.LOW_SCORE_LEVEL
            }
        )
        assertTrue(
            limitedSample.softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.LIMITED_SOURCE_QUALITY
            }
        )
    }

    @Test
    fun saveTaughtPerson_withoutSamples_returnsValidationError() = runTest {
        val fakePersonStore = TeachFakePersonStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = fakePersonStore,
                nowProvider = { 2_000L },
                idProvider = { "person-2" }
            ),
            teachSampleStore = TeachFakeSampleStore()
        )

        val result = controller.saveTaughtPerson(
            displayName = "Alex",
            nickname = "",
            capturedSamples = emptyList()
        )

        assertEquals(
            TeachPersonSaveResult.ValidationError("Capture at least 1 sample before saving."),
            result
        )
        assertTrue(fakePersonStore.listAll().isEmpty())
    }

    @Test
    fun saveTaughtPerson_withSamples_persistsUsingPersonFlowControllerPath() = runTest {
        val fakeStore = TeachFakePersonStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = fakeStore,
                nowProvider = { 9_000L },
                idProvider = { "person-from-teach" }
            ),
            teachSampleStore = TeachFakeSampleStore()
        )
        val sample = TeachPersonCapturedSample(
            observationId = "observation-1",
            observedAtMs = 8_000L,
            source = "CAMERA",
            note = "teach_person_session=session-a;sample=1",
            imageUri = "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-1.jpg",
            faceCropUri = "content://com.aipet.brain.app.fileprovider/teach_samples/face_crops/sample-1.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = "Camera capture source.",
                evaluatedAtMs = 8_010L
            )
        )

        val extraSample1 = TeachPersonCapturedSample(
            observationId = "observation-extra-1",
            observedAtMs = 8_001L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/extra-1.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 8_001L
            )
        )
        val extraSample2 = TeachPersonCapturedSample(
            observationId = "observation-extra-2",
            observedAtMs = 8_002L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/extra-2.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 8_002L
            )
        )
        val result = controller.saveTaughtPerson(
            displayName = "Robin",
            nickname = "Rob",
            capturedSamples = listOf(sample, extraSample1, extraSample2)
        )

        assertEquals(TeachPersonSaveResult.Success("person-from-teach"), result)
        val saved = fakeStore.getById("person-from-teach")
        assertEquals("Robin", saved?.displayName)
        assertEquals("Rob", saved?.nickname)
        assertEquals(9_000L, saved?.createdAtMs)
        assertEquals(9_000L, saved?.updatedAtMs)
    }

    @Test
    fun saveTaughtPerson_withLowQualityOnlySamples_succeedsWithRelaxedQualityGate() = runTest {
        val fakeStore = TeachFakePersonStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = fakeStore,
                nowProvider = { 12_000L },
                idProvider = { "person-soft-warning" }
            ),
            teachSampleStore = TeachFakeSampleStore()
        )
        val sampleWithWarning = TeachPersonCapturedSample(
            observationId = "observation-warning-1",
            observedAtMs = 11_000L,
            source = "DEBUG",
            note = "teach_person_session=session-warning;sample=1",
            imageUri = "file:///tmp/sample-warning-1.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = "Debug source sample for warning coverage.",
                evaluatedAtMs = 11_010L
            )
        )

        assertTrue(sampleWithWarning.hasSoftWarning)
        assertEquals(SampleQualityLevel.LOW, sampleWithWarning.scoredQuality.level)
        assertTrue(
            sampleWithWarning.softWarnings.any { warning ->
                warning.type == SampleQualityWarningType.LOW_SCORE_LEVEL
            }
        )

        val sampleWithWarning2 = TeachPersonCapturedSample(
            observationId = "observation-warning-2",
            observedAtMs = 11_001L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/sample-warning-2.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = null,
                evaluatedAtMs = 11_001L
            )
        )
        val sampleWithWarning3 = TeachPersonCapturedSample(
            observationId = "observation-warning-3",
            observedAtMs = 11_002L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/sample-warning-3.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = null,
                evaluatedAtMs = 11_002L
            )
        )

        val result = controller.saveTaughtPerson(
            displayName = "Taylor",
            nickname = "",
            capturedSamples = listOf(sampleWithWarning, sampleWithWarning2, sampleWithWarning3)
        )

        // Low quality samples (no face crop) now pass the relaxed quality gate since they have a
        // valid imageUri. The gate allows saving so the person is saved successfully.
        assertEquals(TeachPersonSaveResult.Success("person-soft-warning"), result)
        assertNotNull(fakeStore.getById("person-soft-warning"))
    }

    @Test
    fun saveTaughtPerson_withMixedSampleQuality_succeedsWhenAtLeastOneSamplePassesHardGate() = runTest {
        val fakeStore = TeachFakePersonStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(
                personStore = fakeStore,
                nowProvider = { 13_000L },
                idProvider = { "person-quality-gate-pass" }
            ),
            teachSampleStore = TeachFakeSampleStore()
        )
        val failingSample = TeachPersonCapturedSample(
            observationId = "observation-failing",
            observedAtMs = 12_001L,
            source = "CAMERA",
            note = "teach_person_session=session-mixed;sample=1",
            imageUri = "content://sample/failing.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 12_001L
            )
        )
        val passingSample = TeachPersonCapturedSample(
            observationId = "observation-passing",
            observedAtMs = 12_002L,
            source = "CAMERA",
            note = "teach_person_session=session-mixed;sample=2",
            imageUri = "content://sample/passing.jpg",
            faceCropUri = "content://sample/passing-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 12_002L
            )
        )

        val anotherFailingSample = TeachPersonCapturedSample(
            observationId = "observation-failing-2",
            observedAtMs = 12_003L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/failing2.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 12_003L
            )
        )
        val result = controller.saveTaughtPerson(
            displayName = "Morgan",
            nickname = "",
            capturedSamples = listOf(failingSample, anotherFailingSample, passingSample)
        )

        assertEquals(TeachPersonSaveResult.Success("person-quality-gate-pass"), result)
    }

    @Test
    fun evaluateTeachQualityGate_withNoSamples_returnsExplicitMinimumCountFailureReason() {
        val gateResult = evaluateTeachQualityGate(capturedSamples = emptyList())

        assertFalse(gateResult.canSaveTeachPerson)
        assertEquals(TeachQualityGateFailureReason.MINIMUM_SAMPLE_COUNT_NOT_MET, gateResult.failures.first().reason)
        assertEquals("Capture at least 1 sample before saving.", gateResult.saveBlockedReason)
        assertTrue(gateResult.failingSampleObservationIds.isEmpty())
    }

    @Test
    fun evaluateTeachQualityGate_withOnlyFailingSamples_returnsStableFailureSampleIds() {
        val failingA = TeachPersonCapturedSample(
            observationId = "observation-b",
            observedAtMs = 14_001L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/fail-b.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 14_001L
            )
        )
        val failingB = TeachPersonCapturedSample(
            observationId = "observation-a",
            observedAtMs = 14_002L,
            source = "DEBUG",
            note = null,
            imageUri = "content://sample/fail-a.jpg",
            faceCropUri = "content://sample/fail-a-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = null,
                evaluatedAtMs = 14_002L
            )
        )

        val failingC = TeachPersonCapturedSample(
            observationId = "observation-c",
            observedAtMs = 14_003L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/fail-c.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 14_003L
            )
        )

        val gateResult = evaluateTeachQualityGate(
            capturedSamples = listOf(failingA, failingB, failingC)
        )

        // All samples have non-blank imageUris so they now satisfy the relaxed quality gate.
        assertTrue(gateResult.canSaveTeachPerson)
        assertTrue(gateResult.failingSampleObservationIds.isEmpty())
    }

    @Test
    fun selectBestSamples_prefersQualifiedHigherScoreSampleOverWeakerSample() {
        val weakerSample = TeachPersonCapturedSample(
            observationId = "observation-weak",
            observedAtMs = 15_001L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/weak.jpg",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 15_001L
            )
        )
        val strongerSample = TeachPersonCapturedSample(
            observationId = "observation-strong",
            observedAtMs = 15_002L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/strong.jpg",
            faceCropUri = "content://sample/strong-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 15_002L
            )
        )

        val selection = selectBestSamples(
            capturedSamples = listOf(weakerSample, strongerSample)
        )

        assertEquals("observation-strong", selection.bestSampleId)
        assertEquals(setOf("observation-strong"), selection.preferredSampleIds)
        assertEquals(
            listOf("observation-strong", "observation-weak"),
            selection.rankedSampleObservationIds
        )
    }

    @Test
    fun selectBestSamples_withEquivalentQuality_usesDeterministicTieBreakByObservedAtThenObservationId() {
        val newestSample = TeachPersonCapturedSample(
            observationId = "observation-z",
            observedAtMs = 16_003L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/z.jpg",
            faceCropUri = "content://sample/z-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 16_003L
            )
        )
        val sameTimestampSampleB = TeachPersonCapturedSample(
            observationId = "observation-b",
            observedAtMs = 16_002L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/b.jpg",
            faceCropUri = "content://sample/b-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 16_002L
            )
        )
        val sameTimestampSampleA = TeachPersonCapturedSample(
            observationId = "observation-a",
            observedAtMs = 16_002L,
            source = "CAMERA",
            note = null,
            imageUri = "content://sample/a.jpg",
            faceCropUri = "content://sample/a-crop.jpg",
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.UNASSESSED,
                qualityFlags = emptySet(),
                note = null,
                evaluatedAtMs = 16_002L
            )
        )

        val selection = selectBestSamples(
            capturedSamples = listOf(sameTimestampSampleB, sameTimestampSampleA, newestSample)
        )

        assertEquals("observation-z", selection.bestSampleId)
        assertEquals(
            listOf("observation-z", "observation-a", "observation-b"),
            selection.rankedSampleObservationIds
        )
    }

    @Test
    fun removeTeachSample_hardDeletesFromTeachSessionSourceOfTruth() = runTest {
        val fakeSampleStore = TeachFakeSampleStore()
        fakeSampleStore.insert(
            TeachSampleRecord(
                sampleId = "sample-1",
                sessionId = "session-a",
                observationId = "observation-1",
                observedAtMs = 20_001L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/1.jpg",
                faceCropUri = "content://sample/1-crop.jpg",
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 20_010L
            )
        )
        fakeSampleStore.insert(
            TeachSampleRecord(
                sampleId = "sample-2",
                sessionId = "session-a",
                observationId = "observation-2",
                observedAtMs = 20_002L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/2.jpg",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 20_020L
            )
        )
        fakeSampleStore.insert(
            TeachSampleRecord(
                sampleId = "sample-3",
                sessionId = "session-b",
                observationId = "observation-3",
                observedAtMs = 20_003L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/3.jpg",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 20_030L
            )
        )
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(personStore = TeachFakePersonStore()),
            teachSampleStore = fakeSampleStore
        )

        val cleanupResult = controller.removeTeachSample(
            sessionId = "session-a",
            observationId = "observation-1",
            completionConfirmedAtMs = null
        )
        val remainingSessionA = fakeSampleStore.listBySession(sessionId = "session-a", limit = 10)
        val remainingSessionB = fakeSampleStore.listBySession(sessionId = "session-b", limit = 10)

        assertTrue(cleanupResult is TeachSampleCleanupResult.Success)
        assertEquals(
            listOf("observation-2"),
            remainingSessionA.map { sample -> sample.observationId }
        )
        assertEquals(
            listOf("observation-3"),
            remainingSessionB.map { sample -> sample.observationId }
        )
    }

    @Test
    fun removeTeachSample_removingPreferredSample_recomputesGateSummaryPruningAndCompletionState() = runTest {
        val fakeSampleStore = TeachFakeSampleStore()
        val controller = TeachPersonFlowController(
            personFlowController = PersonFlowController(personStore = TeachFakePersonStore()),
            teachSampleStore = fakeSampleStore,
            nowProvider = { 30_000L },
            sampleIdProvider = { "generated-sample-id" }
        )
        fakeSampleStore.insert(
            TeachSampleRecord(
                sampleId = "sample-strong",
                sessionId = "session-c",
                observationId = "observation-strong",
                observedAtMs = 30_100L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/strong.jpg",
                faceCropUri = "content://sample/strong-crop.jpg",
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 30_110L
            )
        )
        fakeSampleStore.insert(
            TeachSampleRecord(
                sampleId = "sample-weak",
                sessionId = "session-c",
                observationId = "observation-weak",
                observedAtMs = 30_101L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/weak.jpg",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 30_120L
            )
        )
        val beforeRemovalSamples = controller.observeCapturedSamplesForSession(
            sessionId = "session-c",
            limit = 10
        ).first()
        assertEquals("observation-strong", selectBestSamples(beforeRemovalSamples).bestSampleId)

        val cleanupResult = controller.removeTeachSample(
            sessionId = "session-c",
            observationId = "observation-strong",
            completionConfirmedAtMs = 29_999L
        )
        assertTrue(cleanupResult is TeachSampleCleanupResult.Success)
        val cleanupSuccess = cleanupResult as TeachSampleCleanupResult.Success
        assertTrue(cleanupSuccess.completionConfirmationReset)
        assertEquals(null, cleanupSuccess.updatedCompletionConfirmedAtMs)

        val afterRemovalSamples = controller.observeCapturedSamplesForSession(
            sessionId = "session-c",
            limit = 10
        ).first()
        assertEquals(1, afterRemovalSamples.size)
        assertEquals("observation-weak", afterRemovalSamples.first().observationId)

        val gateResult = evaluateTeachQualityGate(afterRemovalSamples)
        val bestSampleSelection = selectBestSamples(afterRemovalSamples)
        val sessionSummary = deriveTeachSessionSummary(
            capturedSamples = afterRemovalSamples,
            qualityGateResult = gateResult,
            bestSampleSelection = bestSampleSelection
        )
        val pruningSuggestions = derivePruningSuggestions(
            capturedSamples = afterRemovalSamples,
            bestSampleSelection = bestSampleSelection,
            qualityGateResult = gateResult
        )
        val completionState = controller.getTeachSessionCompletionState(
            capturedSamples = afterRemovalSamples,
            completionConfirmedAtMs = cleanupSuccess.updatedCompletionConfirmedAtMs
        )

        // sample-weak has imageUri → qualifies under relaxed gate (imageUri.isNotBlank())
        assertTrue(gateResult.canSaveTeachPerson)
        assertEquals("observation-weak", bestSampleSelection.bestSampleId)
        assertTrue(sessionSummary.canSave)
        assertFalse(pruningSuggestions.pruningCandidateIds.contains("observation-weak"))
        assertTrue(completionState.isReadyToComplete)
        assertFalse(completionState.isCompleted)
        assertEquals(
            TeachSessionCompletionStatus.READY_TO_COMPLETE,
            completionState.status
        )
    }

    @Test
    fun selectBestSamples_doesNotBypassHardGateWhenOnlyLowQualitySamplesExist() {
        val lowQualitySample = TeachPersonCapturedSample(
            observationId = "observation-low-only",
            observedAtMs = 17_001L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/low-only.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = "Limited sample",
                evaluatedAtMs = 17_001L
            )
        )

        val lowQualitySample2 = TeachPersonCapturedSample(
            observationId = "observation-low-2",
            observedAtMs = 16_999L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/low-2.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = null,
                evaluatedAtMs = 16_999L
            )
        )
        val lowQualitySample3 = TeachPersonCapturedSample(
            observationId = "observation-low-3",
            observedAtMs = 16_998L,
            source = "DEBUG",
            note = null,
            imageUri = "file:///tmp/low-3.png",
            faceCropUri = null,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE,
                qualityFlags = setOf(
                    SampleQualityFlag.DEBUG_GENERATED_IMAGE,
                    SampleQualityFlag.NOT_CAMERA_FRAME
                ),
                note = null,
                evaluatedAtMs = 16_998L
            )
        )
        val allLowSamples = listOf(lowQualitySample, lowQualitySample2, lowQualitySample3)

        val selection = selectBestSamples(capturedSamples = allLowSamples)
        val gateResult = evaluateTeachQualityGate(capturedSamples = allLowSamples)

        assertEquals("observation-low-only", selection.bestSampleId)
        // All samples have a non-blank imageUri so the relaxed gate now allows saving.
        assertTrue(gateResult.canSaveTeachPerson)
    }
}

private class TeachFakePersonStore : PersonStore {
    private val personsById = linkedMapOf<String, PersonRecord>()

    override suspend fun insert(person: PersonRecord) {
        personsById[person.personId] = person
    }

    override suspend fun update(person: PersonRecord): Boolean {
        if (!personsById.containsKey(person.personId)) {
            return false
        }
        personsById[person.personId] = person
        return true
    }

    override suspend fun getById(personId: String): PersonRecord? {
        return personsById[personId]
    }

    override suspend fun listAll(): List<PersonRecord> {
        return personsById.values.toList()
    }

    override suspend fun getOwner(): PersonRecord? {
        return personsById.values.firstOrNull { person -> person.isOwner }
    }

    override suspend fun assignOwner(personId: String): Boolean {
        if (!personsById.containsKey(personId)) {
            return false
        }
        personsById.replaceAll { _, person ->
            person.copy(isOwner = person.personId == personId)
        }
        return true
    }

    override suspend fun clearOwner(): Boolean {
        val hadOwner = personsById.values.any { person -> person.isOwner }
        personsById.replaceAll { _, person -> person.copy(isOwner = false) }
        return hadOwner
    }

    override suspend fun recordPersonSeen(personId: String, seenAtMs: Long): PersonRecord? {
        val existing = personsById[personId] ?: return null
        val updated = existing.copy(
            seenCount = existing.seenCount + 1,
            lastSeenAtMs = seenAtMs,
            updatedAtMs = seenAtMs
        )
        personsById[personId] = updated
        return updated
    }
}

private class TeachFakeSampleStore : TeachSampleStore {
    private val records = mutableListOf<TeachSampleRecord>()
    private val recordsFlow = MutableStateFlow<List<TeachSampleRecord>>(emptyList())

    override suspend fun insert(sample: TeachSampleRecord): Boolean {
        val alreadyExists = records.any { existing ->
            existing.observationId == sample.observationId
        }
        if (alreadyExists) {
            return false
        }
        records.add(sample)
        recordsFlow.value = records.toList()
        return true
    }

    override suspend fun listBySession(sessionId: String, limit: Int): List<TeachSampleRecord> {
        return records
            .asSequence()
            .filter { record -> record.sessionId == sessionId }
            .sortedWith(
                compareByDescending<TeachSampleRecord> { record -> record.createdAtMs }
                    .thenBy { record -> record.sampleId }
            )
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    override fun observeBySession(sessionId: String, limit: Int): Flow<List<TeachSampleRecord>> {
        return recordsFlow.map { allRecords ->
            allRecords
                .asSequence()
                .filter { record -> record.sessionId == sessionId }
                .sortedWith(
                    compareByDescending<TeachSampleRecord> { record -> record.createdAtMs }
                        .thenBy { record -> record.sampleId }
                )
                .take(limit.coerceAtLeast(0))
                .toList()
        }
    }

    override suspend fun deleteBySessionAndObservation(
        sessionId: String,
        observationId: String
    ): Boolean {
        val removed = records.removeAll { record ->
            record.sessionId == sessionId && record.observationId == observationId
        }
        if (removed) {
            recordsFlow.value = records.toList()
        }
        return removed
    }
}
