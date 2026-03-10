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
import com.aipet.brain.app.audio.AudioRuntimeDebugState
import com.aipet.brain.app.ui.audio.AudioPlaybackDebugState
import com.aipet.brain.brain.memory.WorkingMemory
import com.aipet.brain.brain.events.EventEnvelope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugScreen(
    latestEvent: EventEnvelope?,
    latestOwnerSeenEvent: EventEnvelope?,
    latestOwnerGreetingEvent: EventEnvelope?,
    latestAudioStimulusSummary: String,
    audioRuntimeDebugState: AudioRuntimeDebugState,
    audioPlaybackDebugState: AudioPlaybackDebugState,
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
    onEmitAudioResponseRequestFromStimulus: () -> Unit,
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
            text = "Latest audio stimulus: $latestAudioStimulusSummary",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Audio runtime:",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "Energy: ${formatAudioEnergySummary(audioRuntimeDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "VAD: ${audioRuntimeDebugState.vadState?.name ?: "IDLE"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last sound event: ${formatLastSoundEventSummary(audioRuntimeDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Playback: ${formatPlaybackStatusSummary(audioPlaybackDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last played: ${formatLastPlayedSummary(audioPlaybackDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last skipped: ${formatLastSkippedSummary(audioPlaybackDebugState)}",
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
            label = "Emit AUDIO_RESPONSE_REQUESTED (Audio Stimulus)",
            onClick = onEmitAudioResponseRequestFromStimulus
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

private fun formatAudioEnergySummary(state: AudioRuntimeDebugState): String {
    val smoothed = state.latestEnergySmoothed ?: return "-"
    val rms = state.latestEnergyRms ?: return "-"
    val peak = state.latestEnergyPeak ?: return "-"
    val timestamp = formatDebugTimestamp(state.latestEnergyTimestampMs)
    return "sm=${formatAudioMetric(smoothed)} rms=${formatAudioMetric(rms)} peak=${formatAudioMetric(peak)} @ $timestamp"
}

private fun formatLastSoundEventSummary(state: AudioRuntimeDebugState): String {
    val eventType = state.lastSoundEventType?.name ?: return "-"
    val sequence = if (state.lastSoundEventSequence > 0L) {
        state.lastSoundEventSequence.toString()
    } else {
        "-"
    }
    return "$eventType #$sequence @ ${formatDebugTimestamp(state.lastSoundEventTimestampMs)}"
}

private fun formatPlaybackStatusSummary(state: AudioPlaybackDebugState): String {
    val lastSkipReason = state.lastSkippedReason?.name ?: "-"
    return "${state.readinessState.name} clips=${state.loadedClipCount}/${state.totalClipCount} " +
        "failed=${state.failedClipCount} lastSkip=$lastSkipReason"
}

private fun formatLastPlayedSummary(state: AudioPlaybackDebugState): String {
    val category = state.lastPlayedCategory ?: return "-"
    val clipName = state.lastPlayedClipName ?: "-"
    return "$category/$clipName @ ${formatDebugTimestamp(state.lastPlayedAtMs)}"
}

private fun formatLastSkippedSummary(state: AudioPlaybackDebugState): String {
    val reason = state.lastSkippedReason?.name ?: return "-"
    return "$reason @ ${formatDebugTimestamp(state.lastSkippedAtMs)}"
}

private fun formatAudioMetric(value: Double): String {
    return String.format(Locale.US, "%.4f", value)
}

private fun formatDebugTimestamp(timestampMs: Long?): String {
    val timestamp = timestampMs ?: return "-"
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return "${formatter.format(Date(timestamp))} ($timestamp)"
}
