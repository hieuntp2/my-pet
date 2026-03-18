package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.audio.AudioRuntimeDebugState
import com.aipet.brain.app.ui.navigation.PetPrimaryDestination
import com.aipet.brain.app.ui.navigation.PetPrimaryNavigationBar
import com.aipet.brain.app.ui.audio.AudioPlaybackDebugState
import com.aipet.brain.brain.behavior.PetBehaviorCandidate
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.memory.WorkingMemory
import com.aipet.brain.brain.personality.PetPersonalitySummary
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    currentPetState: PetState?,
    currentPetTraits: PetTrait?,
    currentPetPersonalitySummary: PetPersonalitySummary?,
    currentPetConditions: Set<PetCondition>,
    latestBehaviorDecisionSource: String?,
    latestBehaviorDecision: PetBehaviorDecision<PetEmotion>?,
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEventViewer: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToObservationViewer: () -> Unit,
    onNavigateToProfileAssociations: () -> Unit,
    onNavigateToPersons: () -> Unit,
    onNavigateToTraits: () -> Unit,
    onNavigateToWorkingMemoryDebug: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAudioDebug: () -> Unit,
    onForceSleep: () -> Unit,
    onForceWake: () -> Unit,
    onEmitAudioResponseRequestFromStimulus: () -> Unit,
    onEmitTestEvent: () -> Unit,
    onCreateObject: suspend (String) -> Result<String>,
    recognitionProbeSummary: String,
    onRunRecognitionProbe: () -> Unit
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var objectNameInput by remember { mutableStateOf("") }
    var creatingObject by remember { mutableStateOf(false) }
    var objectCreateMessage by remember { mutableStateOf<String?>(null) }
    val audioStimulusSummaryText = latestAudioStimulusSummary.ifBlank { "None" }
    val hasDebugData = latestEvent != null ||
        latestOwnerSeenEvent != null ||
        latestOwnerGreetingEvent != null ||
        currentWorkingMemory.currentPersonId != null ||
        currentWorkingMemory.currentObjectId != null ||
        currentWorkingMemory.lastStimulusTs != null ||
        currentPetState != null ||
        audioRuntimeDebugState.latestEnergyTimestampMs != null ||
        audioRuntimeDebugState.lastSoundEventTimestampMs != null ||
        audioPlaybackDebugState.lastPlayedAtMs != null ||
        audioPlaybackDebugState.lastSkippedAtMs != null ||
        latestBehaviorDecision != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PetPrimaryNavigationBar(
            selectedDestination = PetPrimaryDestination.Debug,
            onNavigateHome = onNavigateToHome,
            onNavigateDiary = onNavigateToDiary,
            onNavigateDebug = {}
        )
        Text(text = "Debug")
        if (!hasDebugData) {
            Text(
                text = "No debug data available yet.",
                modifier = Modifier.fillMaxWidth()
            )
        }
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
            text = "Latest audio stimulus: $audioStimulusSummaryText",
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
            text = "WorkingMemory.object: ${currentWorkingMemory.currentObjectId ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "WorkingMemory.lastStimulus: ${currentWorkingMemory.lastStimulusTs ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet mood: ${currentPetState?.mood?.name ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet stats: energy=${currentPetState?.energy ?: "-"}, hunger=${currentPetState?.hunger ?: "-"}, sleepiness=${currentPetState?.sleepiness ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet social/bond: social=${currentPetState?.social ?: "-"}, bond=${currentPetState?.bond ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet personality: ${currentPetPersonalitySummary?.label ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet traits: ${formatPetTraitsSummary(currentPetTraits)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Pet conditions: ${formatPetConditionsSummary(currentPetConditions)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Behavior source: ${latestBehaviorDecisionSource ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Behavior selection: ${formatBehaviorSelectionSummary(latestBehaviorDecision)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Behavior rationale: ${formatSelectedBehaviorRationale(latestBehaviorDecision)}",
            modifier = Modifier.fillMaxWidth()
        )
        latestBehaviorDecision?.candidates?.forEach { candidate ->
            Text(
                text = "• ${candidate.label}: ${formatWeight(candidate.totalWeight)} ${formatCandidateAdjustments(candidate)}",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            text = "Recognition probe: $recognitionProbeSummary",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Manual object creation",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        OutlinedTextField(
            value = objectNameInput,
            onValueChange = { objectNameInput = it },
            label = { Text("Object name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (creatingObject) {
                    return@Button
                }
                val requestedName = objectNameInput
                creatingObject = true
                objectCreateMessage = "Creating object..."
                coroutineScope.launch {
                    val creationResult = onCreateObject(requestedName)
                    objectCreateMessage = creationResult.fold(
                        onSuccess = { successMessage ->
                            objectNameInput = ""
                            successMessage
                        },
                        onFailure = { error ->
                            "Create object failed: ${error.message ?: "Unknown error"}"
                        }
                    )
                    creatingObject = false
                }
            },
            enabled = !creatingObject && objectNameInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (creatingObject) "Creating..." else "Create Object")
        }
        if (objectCreateMessage != null) {
            Text(
                text = objectCreateMessage.orEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
        }

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
        DebugActionButton(
            label = "Run Recognition Probe",
            onClick = onRunRecognitionProbe
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
            label = "Open Diary",
            onClick = onNavigateToDiary
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
            label = "Open Working Memory",
            onClick = onNavigateToWorkingMemoryDebug
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

private fun formatPetTraitsSummary(traits: PetTrait?): String {
    val current = traits ?: return "-"
    return "playful=${formatWeight(current.playful)} lazy=${formatWeight(current.lazy)} curious=${formatWeight(current.curious)} social=${formatWeight(current.social)}"
}

private fun formatPetConditionsSummary(conditions: Set<PetCondition>): String {
    if (conditions.isEmpty()) {
        return "None"
    }
    return conditions.joinToString(separator = ", ") { it.name }
}

private fun formatBehaviorSelectionSummary(
    decision: PetBehaviorDecision<PetEmotion>?
): String {
    val current = decision ?: return "-"
    return "${current.selectedBehavior.name} via ${current.selectedLabel}"
}

private fun formatSelectedBehaviorRationale(
    decision: PetBehaviorDecision<PetEmotion>?
): String {
    val current = decision ?: return "-"
    val selectedCandidate = current.candidates.firstOrNull { candidate ->
        candidate.label == current.selectedLabel && candidate.behavior == current.selectedBehavior
    } ?: return current.selectedLabel
    if (selectedCandidate.adjustments.isEmpty()) {
        return selectedCandidate.label
    }
    val summary = selectedCandidate.adjustments.joinToString(separator = ", ") { adjustment ->
        "${adjustment.source}=${formatSignedWeight(adjustment.delta)}"
    }
    return "${selectedCandidate.label} ($summary)"
}

private fun formatCandidateAdjustments(
    candidate: PetBehaviorCandidate<PetEmotion>
): String {
    if (candidate.adjustments.isEmpty()) {
        return "(base ${formatWeight(candidate.baseWeight)})"
    }
    val adjustments = candidate.adjustments.joinToString(separator = "; ") { adjustment ->
        "${adjustment.source}=${formatSignedWeight(adjustment.delta)}"
    }
    return "(base ${formatWeight(candidate.baseWeight)}; $adjustments)"
}


private fun formatWeight(value: Float): String {
    return String.format(Locale.US, "%.2f", value)
}

private fun formatSignedWeight(value: Float): String {
    return String.format(Locale.US, "%+.2f", value)
}
