package com.aipet.brain.app.ui.persons

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.brain.observations.ObservationRecorder
import com.aipet.brain.brain.observations.ObservationSource
import com.aipet.brain.memory.db.AppDatabase
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.events.RoomEventStore
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.persons.RoomPersonStore
import com.aipet.brain.memory.teachsessioncompletion.RoomTeachSessionCompletionStore
import com.aipet.brain.memory.teachsessioncompletion.TeachSessionCompletionStore
import com.aipet.brain.memory.teachsamples.RoomTeachSampleStore
import com.aipet.brain.memory.teachsamples.TeachSampleStore
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeachPersonInstrumentationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var appContext: Context
    private lateinit var database: AppDatabase
    private lateinit var personStore: PersonStore
    private lateinit var eventStore: EventStore
    private lateinit var eventBus: InMemoryEventBus
    private lateinit var observationRecorder: ObservationRecorder
    private lateinit var personSeenEventPublisher: PersonSeenEventPublisher
    private lateinit var teachSampleStore: TeachSampleStore
    private lateinit var teachSessionCompletionStore: TeachSessionCompletionStore
    private lateinit var teachSampleImageStorage: TeachSampleImageStorage

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
        appContext.deleteDatabase(AppDatabase.DB_NAME)
        database = Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12,
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_14_15,
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        eventStore = RoomEventStore(database.eventDao())
        personStore = RoomPersonStore(database.personDao())
        teachSampleStore = RoomTeachSampleStore(database.teachSampleDao())
        teachSessionCompletionStore = RoomTeachSessionCompletionStore(
            database.teachSessionCompletionDao()
        )
        teachSampleImageStorage = TeachSampleImageStorage(appContext)
        eventBus = InMemoryEventBus(persistEvent = { event ->
            eventStore.save(event)
        })
        observationRecorder = ObservationRecorder(eventBus)
        personSeenEventPublisher = PersonSeenEventPublisher(eventBus)
    }

    @After
    fun tearDown() {
        database.close()
        appContext.deleteDatabase(AppDatabase.DB_NAME)
    }

    @Test
    fun teachPerson_canAddSampleAndSave_onDevice() {
        val sessionId = "instrumentation-session-1"
        val displayName = "TeachPerson ${System.currentTimeMillis()}"
        var savedPersonId: String? = null

        composeRule.setContent {
            TeachPersonScreen(
                teachSessionId = sessionId,
                teachSampleStore = teachSampleStore,
                teachSampleImageStorage = teachSampleImageStorage,
                personStore = personStore,
                onCaptureSample = { note, imageUri ->
                    runCatching {
                        val observation = observationRecorder.recordPersonLikeObservation(
                            source = ObservationSource.CAMERA,
                            note = note
                        )
                        val faceCropUri = teachSampleImageStorage.createFaceCropImageUri(
                            "${observation.observationId}_crop"
                        ).toString()
                        TeachPersonCapturedObservation(
                            observationId = observation.observationId,
                            observedAtMs = observation.observedAtMs,
                            source = observation.source.name,
                            note = observation.note,
                            imageUri = imageUri,
                            faceCropUri = faceCropUri
                        )
                    }
                },
                captureImageUriOverride = { sampleNote ->
                    runCatching {
                        val captureId = sampleNote.replace(';', '_').replace('=', '_')
                        teachSampleImageStorage.createCameraCaptureImageUri(captureId).toString()
                    }
                },
                onNavigateBack = {},
                onPersonSaved = { savedPersonId = it }
            )
        }

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SCREEN_ROOT).assertIsDisplayed()
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_DISPLAY_NAME_INPUT)
            .performTextInput(displayName)
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_ADD_SAMPLE_BUTTON).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PersonsTestTags.TEACH_PERSON_SAMPLE_ITEM)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAMPLE_COUNT_TEXT)
            .assertTextContains("1")
        composeRule.onNodeWithText("Face crop: Available").assertIsDisplayed()
        composeRule.onNodeWithText("Quality status: UNASSESSED").assertIsDisplayed()

        runBlocking {
            val persisted = teachSampleStore.listBySession(sessionId = sessionId, limit = 10)
            assertTrue(persisted.isNotEmpty())
            assertTrue(persisted.first().imageUri.startsWith("content://"))
            assertTrue(persisted.first().faceCropUri?.startsWith("content://") == true)
            assertEquals("CAMERA", persisted.first().source)
        }

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAVE_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { savedPersonId != null }
        composeRule.waitForIdle()

        composeRule.setContent {
            PersonsScreen(
                personStore = personStore,
                personSeenEventPublisher = personSeenEventPublisher,
                onNavigateBack = {},
                onNavigateToTeachPerson = {},
                onNavigateToCreatePerson = {},
                onNavigateToEditPerson = {}
            )
        }

        composeRule.onNodeWithTag(PersonsTestTags.PERSONS_SCREEN_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Name: $displayName")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Name: $displayName").assertIsDisplayed()
    }

    @Test
    fun teachPerson_requiresAtLeastOneSample_onDevice() {
        var savedPersonId: String? = null

        composeRule.setContent {
            TeachPersonScreen(
                teachSessionId = "instrumentation-session-2",
                teachSampleStore = teachSampleStore,
                teachSampleImageStorage = teachSampleImageStorage,
                personStore = personStore,
                onCaptureSample = { _, _ ->
                    Result.failure(IllegalStateException("not used"))
                },
                onNavigateBack = {},
                onPersonSaved = { savedPersonId = it }
            )
        }

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_DISPLAY_NAME_INPUT)
            .performTextInput("NoSamplePerson")
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAVE_BUTTON).assertIsNotEnabled()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PersonsTestTags.TEACH_PERSON_SAVE_GATE_STATUS_TEXT)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAVE_GATE_STATUS_TEXT)
            .assertTextContains("Capture at least one sample before saving.")
        assertNull(savedPersonId)
        runBlocking {
            assertTrue(personStore.listAll().isEmpty())
        }
    }

    @Test
    fun personList_showsSavedPerson_onDevice() {
        val now = System.currentTimeMillis()
        val displayName = "ListPerson ${now}"
        runBlocking {
            personStore.insert(
                PersonRecord(
                    personId = UUID.randomUUID().toString(),
                    displayName = displayName,
                    nickname = null,
                    isOwner = false,
                    createdAtMs = now,
                    updatedAtMs = now,
                    lastSeenAtMs = null,
                    seenCount = 0
                )
            )
        }

        composeRule.setContent {
            PersonsScreen(
                personStore = personStore,
                personSeenEventPublisher = personSeenEventPublisher,
                onNavigateBack = {},
                onNavigateToTeachPerson = {},
                onNavigateToCreatePerson = {},
                onNavigateToEditPerson = {}
            )
        }

        composeRule.onNodeWithTag(PersonsTestTags.PERSONS_LIST_ROOT).assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Name: $displayName")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Name: $displayName").assertIsDisplayed()
    }

    @Test
    fun teachPerson_completionConfirmationPersistsAndRestoresForSameTeachSessionId_onDevice() {
        val teachSessionId = "instrumentation-completion-restore-${System.currentTimeMillis()}"
        setTeachPersonScreenWithCompletionPersistence(teachSessionId)

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SCREEN_ROOT).assertIsDisplayed()
        composeRule.onNodeWithText("Session: $teachSessionId").assertIsDisplayed()

        captureOneSampleAndWaitForSession(teachSessionId = teachSessionId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: READY")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_CONFIRM_COMPLETION_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: CONFIRMED")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_COMPLETION_CONFIRMED_AT_TEXT)
            .assertIsDisplayed()

        val persistedCompletion = runBlocking {
            teachSessionCompletionStore.getBySessionId(teachSessionId)
        }
        assertNotNull(persistedCompletion)
        assertEquals(teachSessionId, persistedCompletion?.teachSessionId)
        assertEquals(true, persistedCompletion?.isCompletedConfirmed)
        assertNotNull(persistedCompletion?.confirmedAtMs)

        composeRule.activityRule.scenario.recreate()
        setTeachPersonScreenWithCompletionPersistence(teachSessionId)

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SCREEN_ROOT).assertIsDisplayed()
        composeRule.onNodeWithText("Session: $teachSessionId").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: CONFIRMED")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_COMPLETION_CONFIRMED_AT_TEXT)
            .assertIsDisplayed()
    }

    @Test
    fun teachPerson_cleanupResetClearsPersistedCompletionForSameTeachSessionId_onDevice() {
        val teachSessionId = "instrumentation-completion-clear-${System.currentTimeMillis()}"
        setTeachPersonScreenWithCompletionPersistence(teachSessionId)

        captureOneSampleAndWaitForSession(teachSessionId = teachSessionId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: READY")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_CONFIRM_COMPLETION_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: CONFIRMED")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val persistedBeforeCleanup = runBlocking {
            teachSessionCompletionStore.getBySessionId(teachSessionId)
        }
        assertNotNull(persistedBeforeCleanup)

        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_REMOVE_SAMPLE_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: BLOCKED")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_MESSAGE_TEXT)
            .assertTextContains("Completion confirmation was reset because session data changed.")

        val persistedAfterCleanup = runBlocking {
            teachSessionCompletionStore.getBySessionId(teachSessionId)
        }
        assertNull(persistedAfterCleanup)

        composeRule.activityRule.scenario.recreate()
        setTeachPersonScreenWithCompletionPersistence(teachSessionId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: BLOCKED")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun teachPerson_completionDoesNotRestoreForDifferentTeachSessionId_onDevice() {
        val originalTeachSessionId = "instrumentation-completion-original-${System.currentTimeMillis()}"
        val differentTeachSessionId = "$originalTeachSessionId-different"
        setTeachPersonScreenWithCompletionPersistence(originalTeachSessionId)

        captureOneSampleAndWaitForSession(teachSessionId = originalTeachSessionId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: READY")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_CONFIRM_COMPLETION_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: CONFIRMED")
                .fetchSemanticsNodes().isNotEmpty()
        }

        val originalPersistedCompletion = runBlocking {
            teachSessionCompletionStore.getBySessionId(originalTeachSessionId)
        }
        assertNotNull(originalPersistedCompletion)

        setTeachPersonScreenWithCompletionPersistence(differentTeachSessionId)
        composeRule.onNodeWithText("Session: $differentTeachSessionId").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Completion: BLOCKED")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAVE_GATE_STATUS_TEXT)
            .assertTextContains("Capture at least one sample before saving.")

        val differentPersistedCompletion = runBlocking {
            teachSessionCompletionStore.getBySessionId(differentTeachSessionId)
        }
        assertNull(differentPersistedCompletion)
    }

    private fun setTeachPersonScreenWithCompletionPersistence(teachSessionId: String) {
        val restoredCompletionAtMs = runBlocking {
            teachSessionCompletionStore.getBySessionId(teachSessionId)?.confirmedAtMs
        }
        composeRule.setContent {
            TeachPersonScreen(
                teachSessionId = teachSessionId,
                teachSampleStore = teachSampleStore,
                teachSampleImageStorage = teachSampleImageStorage,
                personStore = personStore,
                onCaptureSample = { note, imageUri ->
                    runCatching {
                        val observation = observationRecorder.recordPersonLikeObservation(
                            source = ObservationSource.CAMERA,
                            note = note
                        )
                        val faceCropUri = teachSampleImageStorage.createFaceCropImageUri(
                            "${observation.observationId}_crop"
                        ).toString()
                        TeachPersonCapturedObservation(
                            observationId = observation.observationId,
                            observedAtMs = observation.observedAtMs,
                            source = observation.source.name,
                            note = observation.note,
                            imageUri = imageUri,
                            faceCropUri = faceCropUri
                        )
                    }
                },
                captureImageUriOverride = { sampleNote ->
                    runCatching {
                        val captureId = sampleNote.replace(';', '_').replace('=', '_')
                        teachSampleImageStorage.createCameraCaptureImageUri(captureId).toString()
                    }
                },
                initialCompletionConfirmedAtMs = restoredCompletionAtMs,
                onTeachSessionCompletionConfirmed = { completedAtMs ->
                    teachSessionCompletionStore.confirmCompletion(
                        teachSessionId = teachSessionId,
                        confirmedAtMs = completedAtMs
                    )
                },
                onTeachSessionCompletionCleared = {
                    teachSessionCompletionStore.clearCompletion(teachSessionId = teachSessionId)
                },
                onNavigateBack = {},
                onPersonSaved = {}
            )
        }
    }

    private fun captureOneSampleAndWaitForSession(teachSessionId: String) {
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_ADD_SAMPLE_BUTTON).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(PersonsTestTags.TEACH_PERSON_SAMPLE_ITEM)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(PersonsTestTags.TEACH_PERSON_SAMPLE_COUNT_TEXT)
            .assertTextContains("1")
        runBlocking {
            val persisted = teachSampleStore.listBySession(sessionId = teachSessionId, limit = 10)
            assertTrue(persisted.isNotEmpty())
        }
    }
}
