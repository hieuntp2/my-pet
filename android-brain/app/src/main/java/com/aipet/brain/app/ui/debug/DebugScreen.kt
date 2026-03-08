package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.memory.WorkingMemory
import com.aipet.brain.brain.events.EventEnvelope

@Composable
fun DebugScreen(
    latestEvent: EventEnvelope?,
    latestOwnerSeenEvent: EventEnvelope?,
    latestOwnerGreetingEvent: EventEnvelope?,
    currentBrainState: String,
    currentWorkingMemory: WorkingMemory,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEventViewer: () -> Unit,
    onNavigateToObservationViewer: () -> Unit,
    onNavigateToProfileAssociations: () -> Unit,
    onNavigateToPersons: () -> Unit,
    onNavigateToTraits: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAudioDebug: () -> Unit,
    onForceSleep: () -> Unit,
    onForceWake: () -> Unit,
    onEmitTestEvent: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Debug")
        Text(
            text = "Latest event: ${latestEvent?.type ?: "None"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Latest id: ${latestEvent?.eventId ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Owner detected: ${if (latestOwnerSeenEvent != null) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Greeting triggered: ${if (latestOwnerGreetingEvent != null) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Current brain state: $currentBrainState",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "WorkingMemory.person: ${currentWorkingMemory.currentPersonId ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "WorkingMemory.object: ${currentWorkingMemory.currentObjectLabel ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "WorkingMemory.lastStimulus: ${currentWorkingMemory.lastStimulusAtMs ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Actions",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        DebugActionButton(
            label = "Emit TEST_EVENT",
            onClick = onEmitTestEvent
        )
        DebugActionButton(
            label = "Force Sleep (Debug)",
            onClick = onForceSleep
        )
        DebugActionButton(
            label = "Force Wake (Debug)",
            onClick = onForceWake
        )

        Text(
            text = "Tools",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        DebugActionButton(
            label = "Open Event Viewer",
            onClick = onNavigateToEventViewer
        )
        DebugActionButton(
            label = "Open Observations",
            onClick = onNavigateToObservationViewer
        )
        DebugActionButton(
            label = "Open Profile Associations",
            onClick = onNavigateToProfileAssociations
        )
        DebugActionButton(
            label = "Open Camera",
            onClick = onNavigateToCamera
        )
        DebugActionButton(
            label = "Open Audio Debug",
            onClick = onNavigateToAudioDebug
        )
        DebugActionButton(
            label = "Open Settings",
            onClick = onNavigateToSettings
        )
        DebugActionButton(
            label = "Open Persons",
            onClick = onNavigateToPersons
        )
        DebugActionButton(
            label = "Open Traits",
            onClick = onNavigateToTraits
        )

        DebugActionButton(
            label = "Back to Home",
            onClick = onNavigateToHome,
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun DebugActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = label)
    }
}
