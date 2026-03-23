package com.aipet.brain.app.ui.camera

import com.aipet.brain.app.perception.UnknownObjectPromptSuppressionDebugState
import com.aipet.brain.perception.vision.objectdetection.model.DetectedObject
import com.aipet.brain.perception.vision.objectdetection.model.ObjectBoundingBox
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraObjectPerceptionDebugStateMapperTest {
    @Test
    fun `resolveObjectLabelForDisplay returns fallback unknown label for blank values`() {
        val resolved = resolveObjectLabelForDisplay("   ")

        assertEquals(UNKNOWN_OBJECT_LABEL, resolved.label)
        assertEquals("missing_label", resolved.fallbackReason)
    }

    @Test
    fun `resolveObjectLabelForDisplay returns fallback unknown label for class id values`() {
        val resolved = resolveObjectLabelForDisplay("class_42")

        assertEquals(UNKNOWN_OBJECT_LABEL, resolved.label)
        assertEquals("class_id_fallback:class_42", resolved.fallbackReason)
    }

    @Test
    fun `buildCameraObjectPerceptionDebugState maps known unknown and unresolved detections`() {
        val detectionResult = ObjectDetectionResult(
            timestampMs = 1234L,
            sourceFrameWidth = 200,
            sourceFrameHeight = 100,
            modelName = "model.tflite",
            inferenceDurationMs = 17L,
            detections = listOf(
                DetectedObject(
                    label = "cup",
                    confidence = 0.91f,
                    boundingBox = ObjectBoundingBox(left = 10, top = 10, right = 50, bottom = 60)
                ),
                DetectedObject(
                    label = "chair",
                    confidence = 1.4f,
                    boundingBox = ObjectBoundingBox(left = 0, top = 0, right = 100, bottom = 50)
                ),
                DetectedObject(
                    label = "class_9",
                    confidence = Float.NaN,
                    boundingBox = null
                )
            )
        )

        val state = buildCameraObjectPerceptionDebugState(
            detectionResult = detectionResult,
            knownDisplayNamesByCanonical = mapOf(
                "cup" to "Blue Cup",
                "chair" to null
            ),
            promptSuppression = UnknownObjectPromptSuppressionDebugState(
                canonicalLabel = "cup",
                isSuppressed = true,
                suppressedUntilMs = 5000L,
                remainingMs = 1200L
            )
        )

        assertEquals("model.tflite", state.modelName)
        assertEquals(17L, state.inferenceDurationMs)
        assertEquals(1234L, state.updatedAtMs)
        assertTrue(state.promptSuppression.isSuppressed)
        assertEquals("cup", state.promptSuppression.canonicalLabel)
        assertEquals(3, state.detections.size)

        val known = state.detections[0]
        assertEquals("cup", known.canonicalLabel)
        assertEquals("Blue Cup", known.displayLabel)
        assertEquals(CameraObjectKnownState.KNOWN, known.knownState)
        assertEquals(40, known.boundingBoxWidthPx)
        assertEquals(50, known.boundingBoxHeightPx)
        assertEquals(10.0f, known.boundingBoxAreaPercent ?: 0f, 0.001f)
        assertEquals(0.91f, known.confidence ?: 0f, 0.001f)

        val unknown = state.detections[1]
        assertEquals("chair", unknown.canonicalLabel)
        assertEquals("chair", unknown.displayLabel)
        assertEquals(CameraObjectKnownState.UNKNOWN, unknown.knownState)
        assertEquals(1.0f, unknown.confidence ?: 0f, 0.001f)
        assertEquals(50.0f, unknown.boundingBoxAreaPercent ?: 0f, 0.001f)

        val unresolved = state.detections[2]
        assertEquals(UNKNOWN_OBJECT_LABEL, unresolved.canonicalLabel)
        assertEquals(UNKNOWN_OBJECT_LABEL, unresolved.displayLabel)
        assertEquals(CameraObjectKnownState.UNRESOLVED, unresolved.knownState)
        assertNull(unresolved.confidence)
        assertNull(unresolved.boundingBoxWidthPx)
        assertNull(unresolved.boundingBoxAreaPercent)
    }

    @Test
    fun `preferredObjectDisplayLabel prefers alias only for non fallback labels`() {
        assertEquals(
            "Desk Plant",
            preferredObjectDisplayLabel(
                rawLabel = "plant",
                knownDisplayNamesByCanonical = mapOf("plant" to "Desk Plant")
            )
        )
        assertEquals(
            UNKNOWN_OBJECT_LABEL,
            preferredObjectDisplayLabel(
                rawLabel = "???",
                knownDisplayNamesByCanonical = mapOf(UNKNOWN_OBJECT_LABEL to "Should not be used")
            )
        )
        assertEquals(
            "n/a",
            formatConfidence(null)
        )
        assertEquals("0.125", formatConfidence(0.125f))
        assertFalse(formatConfidence(null).isBlank())
    }
}
