package com.aipet.brain.memory.teachsessioncompletion

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aipet.brain.memory.db.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomTeachSessionCompletionStoreHostTest {
    private val appContext: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun confirmCompletion_persistsAndRestores_whenSameTeachSessionIdResumed() = runTest {
        val teachSessionId = "host-session-restore"
        val otherTeachSessionId = "host-session-other"
        val confirmedAtMs = 12_345L
        val database = openInMemoryDatabase()

        try {
            val firstStore = RoomTeachSessionCompletionStore(
                teachSessionCompletionDao = database.teachSessionCompletionDao(),
                nowProvider = { 20_000L }
            )
            assertTrue(
                firstStore.confirmCompletion(
                    teachSessionId = teachSessionId,
                    confirmedAtMs = confirmedAtMs
                )
            )
            // Recreate the store to simulate app/repository re-creation.
            val restoredStore = RoomTeachSessionCompletionStore(
                teachSessionCompletionDao = database.teachSessionCompletionDao(),
                nowProvider = { 30_000L }
            )
            val restored = restoredStore.getBySessionId(teachSessionId)
            assertNotNull(restored)
            assertEquals(teachSessionId, restored?.teachSessionId)
            assertEquals(true, restored?.isCompletedConfirmed)
            assertEquals(confirmedAtMs, restored?.confirmedAtMs)
            assertEquals(20_000L, restored?.updatedAtMs)

            val otherSession = restoredStore.getBySessionId(otherTeachSessionId)
            assertNull(otherSession)
        } finally {
            database.close()
        }
    }

    @Test
    fun clearCompletion_removesPersistedConfirmation_forSameTeachSessionId() = runTest {
        val teachSessionId = "host-session-clear"
        val database = openInMemoryDatabase()

        try {
            val firstStore = RoomTeachSessionCompletionStore(
                teachSessionCompletionDao = database.teachSessionCompletionDao(),
                nowProvider = { 45_000L }
            )
            assertTrue(
                firstStore.confirmCompletion(
                    teachSessionId = teachSessionId,
                    confirmedAtMs = 44_000L
                )
            )
            // Recreate the store to ensure clear/reset goes through real persistence path.
            val clearStore = RoomTeachSessionCompletionStore(
                teachSessionCompletionDao = database.teachSessionCompletionDao(),
                nowProvider = { 55_000L }
            )
            assertTrue(clearStore.clearCompletion(teachSessionId))
            assertNull(clearStore.getBySessionId(teachSessionId))
            val restoredAfterClear = RoomTeachSessionCompletionStore(
                teachSessionCompletionDao = database.teachSessionCompletionDao(),
                nowProvider = { 65_000L }
            ).getBySessionId(teachSessionId)
            assertNull(restoredAfterClear)
        } finally {
            database.close()
        }
    }

    private fun openInMemoryDatabase(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            appContext,
            AppDatabase::class.java
        ).build()
    }
}
