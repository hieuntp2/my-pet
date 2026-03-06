package com.aipet.brain.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.aipet.brain.app.ui.debug.DebugScreen
import com.aipet.brain.app.ui.debug.EventViewerScreen
import com.aipet.brain.app.ui.home.HomeScreen
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.memory.db.AppDatabase
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.events.RoomEventStore
import kotlinx.coroutines.launch

private enum class AppScreen {
    Home,
    Debug,
    EventViewer
}

@Composable
fun PetBrainApp() {
    val appContext = LocalContext.current.applicationContext
    var currentScreen by remember { mutableStateOf(AppScreen.Home) }
    var latestEvent by remember { mutableStateOf<EventEnvelope?>(null) }
    val database = remember(appContext) {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "pet_brain.db").build()
    }
    val eventStore: EventStore = remember(database) {
        RoomEventStore(database.eventDao())
    }
    val eventBus = remember(eventStore) {
        InMemoryEventBus(persistEvent = { event -> eventStore.save(event) })
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
                    onNavigateToDebug = { currentScreen = AppScreen.Debug }
                )

                AppScreen.Debug -> DebugScreen(
                    latestEvent = latestEvent,
                    onNavigateToHome = { currentScreen = AppScreen.Home },
                    onNavigateToEventViewer = { currentScreen = AppScreen.EventViewer },
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
                    onNavigateBack = { currentScreen = AppScreen.Debug }
                )
            }
        }
    }
}
