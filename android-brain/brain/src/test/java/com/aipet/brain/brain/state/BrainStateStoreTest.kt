package com.aipet.brain.brain.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainStateStoreTest {
    @Test
    fun defaultState_isIdle() {
        val store = BrainStateStore(nowProvider = { 1_000L })

        val snapshot = store.currentSnapshot()

        assertEquals(BrainState.IDLE, snapshot.currentState)
        assertEquals(1_000L, snapshot.updatedAtMs)
    }

    @Test
    fun setState_updatesSnapshot_whenStateChanges() {
        val store = BrainStateStore(nowProvider = { 2_000L })

        val changed = store.setState(targetState = BrainState.HAPPY, timestampMs = 3_000L)

        assertTrue(changed)
        assertEquals(BrainState.HAPPY, store.currentSnapshot().currentState)
        assertEquals(3_000L, store.currentSnapshot().updatedAtMs)
    }

    @Test
    fun setState_returnsFalse_whenStateIsUnchanged() {
        val store = BrainStateStore(initialState = BrainState.CURIOUS, nowProvider = { 4_000L })

        val changed = store.setState(targetState = BrainState.CURIOUS, timestampMs = 5_000L)

        assertFalse(changed)
        assertEquals(BrainState.CURIOUS, store.currentSnapshot().currentState)
        assertEquals(4_000L, store.currentSnapshot().updatedAtMs)
    }
}
