package com.aipet.brain.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.aipet.brain.app.ui.camera.CameraScreen
import com.aipet.brain.app.ui.debug.DebugScreen
import com.aipet.brain.app.ui.debug.EventViewerScreen
import com.aipet.brain.app.ui.debug.ObservationViewerScreen
import com.aipet.brain.app.ui.home.HomeScreen
import com.aipet.brain.app.ui.persons.PersonEditorScreen
import com.aipet.brain.app.ui.persons.PersonsScreen
import com.aipet.brain.app.ui.profiles.ProfileAssociationsScreen
import com.aipet.brain.brain.events.CameraFrameReceivedPayload
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.brain.observations.ObservationRecorder
import com.aipet.brain.brain.observations.ObservationSource
import com.aipet.brain.memory.db.AppDatabase
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.events.RoomEventStore
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.persons.RoomPersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.memory.profiles.RoomFaceProfileStore
import com.aipet.brain.perception.camera.FrameDiagnostics
import kotlinx.coroutines.launch

private enum class AppScreen {
    Home,
    Debug,
    EventViewer,
    ObservationViewer,
    ProfileAssociations,
    Camera,
    Persons,
    PersonEditor
}

@Composable
fun PetBrainApp() {
    val appContext = LocalContext.current.applicationContext
    var currentScreenName by rememberSaveable { mutableStateOf(AppScreen.Home.name) }
    var editingPersonId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasRequestedCameraPermission by rememberSaveable { mutableStateOf(false) }
    val currentScreen = currentScreenName.toAppScreen()
    var latestEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    val database = remember(appContext) {
        Room.databaseBuilder(appContext, AppDatabase::class.java, AppDatabase.DB_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
    val eventStore: EventStore = remember(database) {
        RoomEventStore(database.eventDao())
    }
    val personStore: PersonStore = remember(database) {
        RoomPersonStore(database.personDao())
    }
    val faceProfileStore: FaceProfileStore = remember(database, personStore) {
        RoomFaceProfileStore(
            faceProfileDao = database.faceProfileDao(),
            personStore = personStore
        )
    }
    val eventBus = remember(eventStore) {
        InMemoryEventBus(persistEvent = { event -> eventStore.save(event) })
    }
    val observationRecorder = remember(eventBus) {
        ObservationRecorder(eventBus)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eventBus) {
        eventBus.observe().collect { event ->
            latestEvent = event
        }
    }

    LaunchedEffect(eventBus, "app_started") {
        eventBus.publish(EventEnvelope.create(type = EventType.APP_STARTED))
    }

    MaterialTheme {
        Surface {
            when (currentScreen) {
                AppScreen.Home -> HomeScreen(
                    latestEvent = latestEvent,
                    onNavigateToDebug = { currentScreenName = AppScreen.Debug.name },
                    onNavigateToCamera = { currentScreenName = AppScreen.Camera.name }
                )

                AppScreen.Debug -> DebugScreen(
                    latestEvent = latestEvent,
                    onNavigateToHome = { currentScreenName = AppScreen.Home.name },
                    onNavigateToEventViewer = { currentScreenName = AppScreen.EventViewer.name },
                    onNavigateToObservationViewer = { currentScreenName = AppScreen.ObservationViewer.name },
                    onNavigateToProfileAssociations = { currentScreenName = AppScreen.ProfileAssociations.name },
                    onNavigateToPersons = { currentScreenName = AppScreen.Persons.name },
                    onNavigateToCamera = { currentScreenName = AppScreen.Camera.name },
                    onEmitTestEvent = {
                        coroutineScope.launch {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.TEST_EVENT,
                                    payloadJson = "{\"source\":\"debug_screen\"}"
                                )
                            )
                        }
                    }
                )

                AppScreen.EventViewer -> EventViewerScreen(
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.ObservationViewer -> ObservationViewerScreen(
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    onRecordDebugObservation = {
                        try {
                            observationRecorder.recordPersonLikeObservation(
                                source = ObservationSource.DEBUG,
                                note = "manual_debug_observation"
                            )
                            Result.success(Unit)
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                    }
                )

                AppScreen.ProfileAssociations -> ProfileAssociationsScreen(
                    faceProfileStore = faceProfileStore,
                    personStore = personStore,
                    eventStore = eventStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name }
                )

                AppScreen.Camera -> CameraScreen(
                    hasRequestedPermission = hasRequestedCameraPermission,
                    onPermissionRequestTracked = { hasRequestedCameraPermission = true },
                    onFrameDiagnostics = { diagnostics ->
                        coroutineScope.launch {
                            eventBus.publish(
                                EventEnvelope.create(
                                    type = EventType.CAMERA_FRAME_RECEIVED,
                                    payloadJson = diagnostics.toCameraFramePayloadJson()
                                )
                            )
                        }
                    },
                    onRecordPersonLikeObservation = { note ->
                        try {
                            observationRecorder.recordPersonLikeObservation(
                                source = ObservationSource.CAMERA,
                                note = note
                            )
                            Result.success(Unit)
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                    },
                    onNavigateBack = { currentScreenName = AppScreen.Home.name }
                )

                AppScreen.Persons -> PersonsScreen(
                    personStore = personStore,
                    onNavigateBack = { currentScreenName = AppScreen.Debug.name },
                    onNavigateToCreatePerson = {
                        editingPersonId = null
                        currentScreenName = AppScreen.PersonEditor.name
                    },
                    onNavigateToEditPerson = { personId ->
                        editingPersonId = personId
                        currentScreenName = AppScreen.PersonEditor.name
                    }
                )

                AppScreen.PersonEditor -> PersonEditorScreen(
                    personStore = personStore,
                    personId = editingPersonId,
                    onNavigateBack = { currentScreenName = AppScreen.Persons.name },
                    onPersonSaved = {
                        currentScreenName = AppScreen.Persons.name
                    }
                )
            }
        }
    }
}

private fun String.toAppScreen(): AppScreen {
    return AppScreen.entries.firstOrNull { it.name == this } ?: AppScreen.Home
}

private fun FrameDiagnostics.toCameraFramePayloadJson(): String {
    return CameraFrameReceivedPayload(
        width = width,
        height = height,
        analyzedAtMs = timestampMs,
        rotationDegrees = rotationDegrees,
        processingDurationMs = processingDurationMs
    ).toJson()
}
