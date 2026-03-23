package com.aipet.brain.app.perception

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnknownObjectPromptSuppressionInspectorTest {
    @Test
    fun `returns empty state for blank canonical label`() {
        val state = inspectUnknownObjectPromptSuppression(
            canonicalLabel = "   ",
            suppressedUntilByLabel = mapOf("cup" to 5000L),
            nowMs = 1000L
        )

        assertNull(state.canonicalLabel)
        assertFalse(state.isSuppressed)
        assertNull(state.suppressedUntilMs)
        assertEquals(0L, state.remainingMs)
    }

    @Test
    fun `returns suppressed state when cooldown is still active`() {
        val state = inspectUnknownObjectPromptSuppression(
            canonicalLabel = "cup",
            suppressedUntilByLabel = mapOf("cup" to 5000L),
            nowMs = 3500L
        )

        assertEquals("cup", state.canonicalLabel)
        assertTrue(state.isSuppressed)
        assertEquals(5000L, state.suppressedUntilMs)
        assertEquals(1500L, state.remainingMs)
    }

    @Test
    fun `returns unsuppressed state when cooldown has expired`() {
        val state = inspectUnknownObjectPromptSuppression(
            canonicalLabel = "cup",
            suppressedUntilByLabel = mapOf("cup" to 5000L),
            nowMs = 5200L
        )

        assertEquals("cup", state.canonicalLabel)
        assertFalse(state.isSuppressed)
        assertEquals(5000L, state.suppressedUntilMs)
        assertEquals(0L, state.remainingMs)
    }
}
