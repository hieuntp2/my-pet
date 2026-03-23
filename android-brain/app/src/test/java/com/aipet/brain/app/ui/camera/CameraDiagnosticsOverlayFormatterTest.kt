package com.aipet.brain.app.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraDiagnosticsOverlayFormatterTest {
    @Test
    fun `formatPromptCooldownText renders n-a for blank canonical label`() {
        assertEquals(
            "Prompt cooldown: n/a",
            formatPromptCooldownText(CameraObjectPromptSuppressionState())
        )
    }

    @Test
    fun `formatPromptCooldownText renders remaining time when suppressed`() {
        assertEquals(
            "Prompt cooldown: cup (2400 ms left)",
            formatPromptCooldownText(
                CameraObjectPromptSuppressionState(
                    canonicalLabel = "cup",
                    isSuppressed = true,
                    remainingMs = 2400L
                )
            )
        )
    }

    @Test
    fun `formatDetectionSummaryLine renders known detection with bbox and age`() {
        val line = formatDetectionSummaryLine(
            index = 1,
            detection = CameraObjectDebugItem(
                canonicalLabel = "cup",
                displayLabel = "Blue Cup",
                knownState = CameraObjectKnownState.KNOWN,
                confidence = 0.812f,
                boundingBoxWidthPx = 40,
                boundingBoxHeightPx = 50,
                boundingBoxAreaPercent = 10.0f,
                observedAtMs = 2000L
            ),
            nowMs = 2600L
        )

        assertEquals(
            "  2. Blue Cup [known] 0.812, bbox=40x50px 10.0%, age=600 ms",
            line
        )
    }

    @Test
    fun `formatDetectionSummaryLine renders unresolved detection without bbox`() {
        val line = formatDetectionSummaryLine(
            index = 0,
            detection = CameraObjectDebugItem(
                canonicalLabel = "Unknown object",
                displayLabel = "Unknown object",
                knownState = CameraObjectKnownState.UNRESOLVED,
                confidence = null,
                boundingBoxWidthPx = null,
                boundingBoxHeightPx = null,
                boundingBoxAreaPercent = null,
                observedAtMs = 5000L
            ),
            nowMs = 4500L
        )

        assertEquals(
            "  1. Unknown object [unresolved] n/a, bbox=n/a, age=0 ms",
            line
        )
    }
}
