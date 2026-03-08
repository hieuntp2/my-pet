package com.aipet.brain.memory.teachsessioncompletion

import com.aipet.brain.memory.db.TeachSessionCompletionDao
import com.aipet.brain.memory.db.TeachSessionCompletionEntity
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

class RoomTeachSessionCompletionStoreTest {
    @Test
    fun confirmCompletion_andGetBySessionId_persistsCompletionConfirmation() = runTest {
        val store = RoomTeachSessionCompletionStore(
            teachSessionCompletionDao = FakeTeachSessionCompletionDao(),
            nowProvider = { 9_000L }
        )

        val persisted = store.confirmCompletion(
            teachSessionId = "session-a",
            confirmedAtMs = 8_500L
        )
        val loaded = store.getBySessionId("session-a")

        assertTrue(persisted)
        assertNotNull(loaded)
        assertEquals("session-a", loaded?.teachSessionId)
        assertTrue(loaded?.isCompletedConfirmed == true)
        assertEquals(8_500L, loaded?.confirmedAtMs)
        assertEquals(9_000L, loaded?.updatedAtMs)
    }

    @Test
    fun observeBySessionId_reflectsConfirmedAndClearedStates() = runTest {
        val store = RoomTeachSessionCompletionStore(
            teachSessionCompletionDao = FakeTeachSessionCompletionDao(),
            nowProvider = { 10_000L }
        )

        val initiallyObserved = store.observeBySessionId("session-b").first()
        store.confirmCompletion(teachSessionId = "session-b", confirmedAtMs = 9_700L)
        val confirmedObserved = store.observeBySessionId("session-b").first()
        val cleared = store.clearCompletion("session-b")
        val clearedObserved = store.observeBySessionId("session-b").first()

        assertEquals(null, initiallyObserved)
        assertNotNull(confirmedObserved)
        assertEquals(true, confirmedObserved?.isCompletedConfirmed)
        assertEquals(9_700L, confirmedObserved?.confirmedAtMs)
        assertTrue(cleared)
        assertEquals(null, clearedObserved)
    }

    @Test
    fun clearCompletion_whenMissing_returnsFalse() = runTest {
        val store = RoomTeachSessionCompletionStore(
            teachSessionCompletionDao = FakeTeachSessionCompletionDao()
        )

        val cleared = store.clearCompletion("missing-session")

        assertFalse(cleared)
    }
}

private class FakeTeachSessionCompletionDao : TeachSessionCompletionDao {
    private val entries = linkedMapOf<String, TeachSessionCompletionEntity>()
    private val flow = MutableStateFlow<Map<String, TeachSessionCompletionEntity>>(emptyMap())

    override suspend fun upsert(completion: TeachSessionCompletionEntity): Long {
        entries[completion.teachSessionId] = completion
        flow.value = entries.toMap()
        return 1L
    }

    override suspend fun getBySessionId(teachSessionId: String): TeachSessionCompletionEntity? {
        return entries[teachSessionId]
    }

    override fun observeBySessionId(teachSessionId: String): Flow<TeachSessionCompletionEntity?> {
        return flow.map { map ->
            map[teachSessionId]
        }
    }

    override suspend fun deleteBySessionId(teachSessionId: String): Int {
        val removed = entries.remove(teachSessionId)
        if (removed != null) {
            flow.value = entries.toMap()
            return 1
        }
        return 0
    }
}
