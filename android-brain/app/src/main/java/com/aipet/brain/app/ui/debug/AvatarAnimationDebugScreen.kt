package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.ui.components.SelectableButton
import com.aipet.brain.ui.avatar.pixel.bridge.DefaultPixelPetStateMapper
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestrator
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelPetVisualState
import com.aipet.brain.ui.avatar.pixel.model.Sleepy
import com.aipet.brain.ui.avatar.pixel.model.Thinking
import com.aipet.brain.ui.avatar.pixel.catalog.AuthoredPixelPetAnimationPack
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationPlaybackState
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionStrategy
import com.aipet.brain.ui.avatar.pixel.ui.AnimatedPixelPetAvatar
import com.aipet.brain.ui.avatar.pixel.ui.debugLabel
import com.aipet.brain.ui.avatar.pixel.ui.debugSummary

@Composable
fun AvatarAnimationDebugScreen(
    runtimeBridgeState: PixelPetBridgeState,
    onNavigateBack: () -> Unit
) {
    val registry = remember { AuthoredPixelPetAnimationPack.createRegistry() }
    val animationController = remember { PixelAnimationController() }
    val variantSelector = remember {
        PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.SimpleRoundRobin
        )
    }
    val stateMapper = remember { DefaultPixelPetStateMapper() }
    val orchestrator = remember {
        PixelAnimationOrchestrator(
            stateMapper = stateMapper,
            animationSetRegistry = registry,
            variantSelector = variantSelector,
            animationController = animationController
        )
    }
    val runtimeVisualState = remember(runtimeBridgeState) { stateMapper.map(runtimeBridgeState) }
    val availableStates = remember { PixelPetVisualState.coreStates }
    var selectedStateId by rememberSaveable { mutableStateOf(runtimeVisualState.id) }
    var autoVariantEnabled by rememberSaveable { mutableStateOf(true) }
    var manualVariantId by rememberSaveable { mutableStateOf("") }
    var isPlaying by rememberSaveable { mutableStateOf(true) }
    var speedMultiplier by rememberSaveable { mutableStateOf(1f) }
    var playbackState by remember { mutableStateOf<PixelAnimationPlaybackState?>(null) }
    var displayedVariant by remember { mutableStateOf<PixelAnimationVariant?>(null) }
    val selectedVisualState = remember(selectedStateId) { visualStateFromId(selectedStateId) }
    val selectedAnimationSet = remember(registry, selectedVisualState) {
        registry.requireAnimationSet(selectedVisualState)
    }
    val sectionHeaderStyle = MaterialTheme.typography.titleMedium
    val sectionHeaderColor = MaterialTheme.colorScheme.onSurface

    LaunchedEffect(selectedVisualState.id) {
        manualVariantId = selectedAnimationSet.variants.first().id
    }

    LaunchedEffect(selectedVisualState.id, autoVariantEnabled) {
        if (autoVariantEnabled) {
            val variant = orchestrator.synchronize(selectedVisualState.toBridgeState())
            displayedVariant = variant
            manualVariantId = variant.id
        }
    }

    LaunchedEffect(selectedVisualState.id, manualVariantId, autoVariantEnabled) {
        if (!autoVariantEnabled) {
            val variant = selectedAnimationSet.variants.firstOrNull { it.id == manualVariantId }
                ?: return@LaunchedEffect
            animationController.setClip(variant.clip)
            displayedVariant = variant
        }
    }

    val orchestratorDiagnostics = orchestrator.getDiagnostics()
    val controllerDiagnostics = animationController.getDiagnostics()
    val currentPlaybackState = playbackState ?: animationController.getPlaybackState()
    val currentVariant = displayedVariant ?: orchestrator.getActiveVariant()
    val recentTransitionSummary = orchestratorDiagnostics.recentTransitions
        .asReversed()
        .joinToString(separator = "\n") { transition ->
            "${transition.visualStateId}/${transition.variantId} via ${transition.reason}"
        }
        .ifBlank { "No orchestrator transitions yet." }
    val recentClipSummary = controllerDiagnostics.recentClipHistory
        .asReversed()
        .joinToString(separator = " -> ")
        .ifBlank { "No clip activations yet." }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Avatar Animation Debug",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Inspect all five authored packs, compare tempo, and spot clip resets without changing the production Home avatar path.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedPixelPetAvatar(
                    animationController = animationController,
                    size = 220.dp,
                    isPlaying = isPlaying,
                    speedMultiplier = speedMultiplier,
                    onPlaybackStateChanged = { updatedState -> playbackState = updatedState }
                )
                Text(text = "State: ${selectedVisualState.label()}")
                Text(text = "Variant mode: ${if (autoVariantEnabled) "auto" else "manual"}")
                Text(text = "Variant: ${currentVariant?.id ?: "-"}")
                Text(text = "Clip: ${currentVariant?.debugSummary ?: "-"}")
                Text(text = "Frame: ${((currentPlaybackState?.frameIndex ?: -1) + 1).coerceAtLeast(0)}/${currentPlaybackState?.clip?.frames?.size ?: 0}")
                Text(text = "Playback: ${if (isPlaying) "playing" else "paused"} @ ${speedMultiplier}x")
                Text(text = "Playback mode: ${currentPlaybackState?.clip?.playbackMode?.debugLabel ?: "-"}")
                Text(text = "Clip duration: ${currentPlaybackState?.clip?.totalDurationMillis ?: 0} ms")
            }
        }

        DebugSectionCard(
            title = "Manual state selection",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            DebugButtonRows(
                labels = availableStates.map { it.label() },
                selectedLabel = selectedVisualState.label(),
                onLabelSelected = { label ->
                    selectedStateId = availableStates.first { it.label() == label }.id
                }
            )
        }

        DebugSectionCard(
            title = "Variant inspection",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectableButton(
                    selected = autoVariantEnabled,
                    onClick = { autoVariantEnabled = true },
                    modifier = Modifier.weight(1f)
                )
                {
                    Text(text = "Auto")
                }
                SelectableButton(
                    selected = !autoVariantEnabled,
                    onClick = { autoVariantEnabled = false },
                    modifier = Modifier.weight(1f)
                )
                {
                    Text(text = "Manual")
                }
            }
            DebugButtonRows(
                labels = selectedAnimationSet.variants.map { it.id },
                selectedLabel = manualVariantId,
                enabled = !autoVariantEnabled,
                onLabelSelected = { manualVariantId = it }
            )
        }

        DebugSectionCard(
            title = "Playback controls",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectableButton(
                    selected = isPlaying,
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.weight(1f)
                )
                {
                    Text(text = if (isPlaying) "Pause" else "Play")
                }
                OutlinedButton(
                    onClick = {
                        currentVariant?.let { variant ->
                            animationController.setClip(variant.clip)
                            displayedVariant = variant
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Restart")
                }
            }
            DebugButtonRows(
                labels = PLAYBACK_SPEEDS.map(::speedLabel),
                selectedLabel = speedLabel(speedMultiplier),
                onLabelSelected = { label ->
                    speedMultiplier = PLAYBACK_SPEEDS.first { speedLabel(it) == label }
                }
            )
        }

        DebugSectionCard(
            title = "Inspection helpers",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            Text(text = "Synchronizations: ${orchestratorDiagnostics.synchronizationCount}")
            Text(text = "State transitions: ${orchestratorDiagnostics.stateTransitionCount}")
            Text(text = "Clip activations: ${controllerDiagnostics.clipActivationCount}")
            Text(text = "Explicit clip restarts: ${controllerDiagnostics.clipRestartCount}")
            Text(text = "Clip reuse skips: ${orchestratorDiagnostics.clipReuseCount}")
            Text(text = "Clip resyncs: ${orchestratorDiagnostics.clipResyncCount}")
            Text(
                text = "Recent orchestrator transitions:\n$recentTransitionSummary",
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Recent clip history: $recentClipSummary",
                modifier = Modifier.fillMaxWidth()
            )
        }

        DebugSectionCard(
            title = "Runtime bridge context",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            Text(text = "Runtime visual state: ${runtimeVisualState.label()}")
            Text(text = "Runtime intent: ${runtimeBridgeState.intent.name.lowercase()}")
            Text(text = "Reason: ${runtimeBridgeState.debugMetadata?.priorityReason ?: "-"}")
            Text(text = "Sources: ${runtimeBridgeState.debugMetadata?.sourceSummary ?: "-"}")
            Text(text = "Policy: ${runtimeBridgeState.debugMetadata?.policySummary ?: "-"}")
        }

        DebugSectionCard(
            title = "Tuning guidance",
            headerStyle = sectionHeaderStyle,
            headerColor = sectionHeaderColor,
            contentSpacing = 10.dp,
            contentPadding = 16.dp
        ) {
            Text(text = "Compare Neutral against Sleepy for slow-lid timing and energy drop.")
            Text(text = "Compare Curious against Thinking for hold length and micro-shifts.")
            Text(text = "Use 0.5x and 2x speeds to stress blink cadence and clip pacing.")
            Text(text = "Clip reuse skips and resync counts help confirm repeated-state stability.")
        }

        DebugBackButton(
            onClick = onNavigateBack,
            outlined = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DebugButtonRows(
    labels: List<String>,
    selectedLabel: String,
    enabled: Boolean = true,
    onLabelSelected: (String) -> Unit
) {
    labels.chunked(2).forEach { rowLabels ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rowLabels.forEach { label ->
                SelectableButton(
                    selected = label == selectedLabel,
                    enabled = enabled,
                    onClick = { onLabelSelected(label) },
                    modifier = Modifier.weight(1f)
                )
                {
                    Text(text = label)
                }
            }
            if (rowLabels.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun visualStateFromId(stateId: String): PixelPetVisualState {
    return PixelPetVisualState.coreStates.firstOrNull { it.id == stateId } ?: Neutral
}

private fun PixelPetVisualState.toBridgeState(): PixelPetBridgeState {
    return PixelPetBridgeState(
        intent = when (this) {
            Neutral -> PixelPetAvatarIntent.NEUTRAL
            Happy -> PixelPetAvatarIntent.ENGAGED
            Curious -> PixelPetAvatarIntent.ATTENTIVE
            Thinking -> PixelPetAvatarIntent.PROCESSING
            Sleepy -> PixelPetAvatarIntent.LOW_ENERGY
            else -> PixelPetAvatarIntent.NEUTRAL
        }
    )
}

private fun PixelPetVisualState.label(): String {
    return id.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun speedLabel(speed: Float): String = "${speed}x"

private val PLAYBACK_SPEEDS = listOf(0.5f, 1f, 1.5f, 2f)
