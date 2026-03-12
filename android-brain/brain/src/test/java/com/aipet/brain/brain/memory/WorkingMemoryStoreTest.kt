package com.aipet.brain.brain.memory

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkingMemoryStoreTest {
    @Test
    fun initialState_isEmptyWorkingMemory() {
        val store = WorkingMemoryStore()

        val memory = store.currentSnapshot()

        assertEquals(null, memory.currentPersonId)
        assertEquals(null, memory.currentObjectId)
        assertEquals(null, memory.lastStimulusTs)
    }

    @Test
    fun update_appliesTransformAndNotifiesSnapshot() {
        val store = WorkingMemoryStore()

        store.update { current ->
            current.copy(
                currentPersonId = "person-1",
                currentObjectId = "object-1",
                lastStimulusTs = 1_000L
            )
        }

        val memory = store.observe().value
        assertEquals("person-1", memory.currentPersonId)
        assertEquals("object-1", memory.currentObjectId)
        assertEquals(1_000L, memory.lastStimulusTs)
    }
}
