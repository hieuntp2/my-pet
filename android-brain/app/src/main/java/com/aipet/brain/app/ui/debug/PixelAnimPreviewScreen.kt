package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.pixel.avatar.clips.ClipRegistry
import com.aipet.brain.pixel.avatar.composable.PixelPetAvatar
import com.aipet.brain.pixel.avatar.controller.PixelAnimationController
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Debug screen for inspecting pixel eye animation states and variants.
 * Accessible via Debug screen → "Pixel Animation Preview".
 */
@Composable
fun PixelAnimPreviewScreen(
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val controller = remember {
        PixelAnimationController(
            scope = scope,
            stateRegistry = ClipRegistry.ALL_STATES
        )
    }

    val currentFrame by controller.currentFrame.collectAsState()
    val stateName by controller.activeStateName.collectAsState()
    val variantName by controller.activeVariantName.collectAsState()
    val frameIndex by controller.activeFrameIndex.collectAsState()
    val frameTotal by controller.activeFrameTotal.collectAsState()
    val holdMs by controller.activeHoldMs.collectAsState()

    var selectedState by remember { mutableStateOf(PetVisualState.NEUTRAL) }
    var forcedVariantIndex by remember { mutableIntStateOf(-1) }
    var showGrid by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Pixel Animation Preview", style = MaterialTheme.typography.titleMedium)

        PixelPetAvatar(
            frame = currentFrame,
            displaySize = 320.dp,
            showDebugGrid = showGrid
        )

        Text(text = "State: $stateName | Variant: $variantName")
        Text(text = "Frame: $frameIndex / ${frameTotal - 1} | Hold: ${holdMs}ms")

        // State selector
        Text(text = "Select State:", modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PetVisualState.entries.forEach { state ->
                val isSelected = state == selectedState
                if (isSelected) {
                    Button(
                        onClick = {
                            selectedState = state
                            forcedVariantIndex = -1
                            controller.clearDebugOverrides()
                            controller.setVisualState(state)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(text = state.name.take(4)) }
                } else {
                    OutlinedButton(
                        onClick = {
                            selectedState = state
                            forcedVariantIndex = -1
                            controller.clearDebugOverrides()
                            controller.setVisualState(state)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(text = state.name.take(4)) }
                }
            }
        }

        // Variant selector
        val variantCount = ClipRegistry.ALL_STATES[selectedState]?.variants?.size ?: 0
        if (variantCount > 0) {
            Text(text = "Force Variant (-1=auto):", modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val options = listOf(-1) + (0 until variantCount).toList()
                options.forEach { idx ->
                    val label = if (idx == -1) "Auto" else "$idx"
                    val isActive = idx == forcedVariantIndex
                    if (isActive) {
                        Button(
                            onClick = {
                                forcedVariantIndex = idx
                                if (idx == -1) controller.forceVariant(selectedState, null)
                                else controller.forceVariant(selectedState, idx)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(text = label) }
                    } else {
                        OutlinedButton(
                            onClick = {
                                forcedVariantIndex = idx
                                if (idx == -1) controller.forceVariant(selectedState, null)
                                else controller.forceVariant(selectedState, idx)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(text = label) }
                    }
                }
            }
        }

        // Playback controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { controller.pause() }, modifier = Modifier.weight(1f)) {
                Text(text = "Pause")
            }
            OutlinedButton(onClick = { controller.resume() }, modifier = Modifier.weight(1f)) {
                Text(text = "Resume")
            }
            OutlinedButton(onClick = { controller.restartClip() }, modifier = Modifier.weight(1f)) {
                Text(text = "Restart")
            }
        }

        OutlinedButton(
            onClick = { showGrid = !showGrid },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (showGrid) "Hide Grid" else "Show Grid")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Back to Debug")
        }
    }
}
