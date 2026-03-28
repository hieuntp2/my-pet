package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.pixel.bridge.DefaultPixelPetStateMapper
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestrator
import com.aipet.brain.ui.avatar.pixel.catalog.AuthoredPixelPetAnimationPack
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionStrategy
import com.aipet.brain.ui.avatar.pixel.ui.AnimatedPixelPetAvatar
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestratorDiagnostics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun HomePixelPetAvatar(
    bridgeState: PixelPetBridgeState,
    modifier: Modifier = Modifier,
    variantCategoryBias: Map<String, Float> = emptyMap(),
    onOrchestratorDiagnosticsChanged: ((PixelAnimationOrchestratorDiagnostics) -> Unit)? = null,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val registry = remember { AuthoredPixelPetAnimationPack.createRegistry() }
    val animationController = remember { PixelAnimationController() }
    val variantSelector = remember {
        PixelAnimationVariantSelector(
            // WeightedRandom: respects tier weights + anti-repeat history so the same
            // variant doesn't appear twice in a row during idle cycling.
            strategy = PixelVariantSelectionStrategy.WeightedRandom(nonRepeatingHistorySize = 2)
        )
    }
    val orchestrator = remember {
        PixelAnimationOrchestrator(
            stateMapper = DefaultPixelPetStateMapper(),
            animationSetRegistry = registry,
            variantSelector = variantSelector,
            animationController = animationController
        )
    }

    // Idle timer: synchronize on bridge state change, then cycle through variants to prevent
    // the pet from showing the same eye animation indefinitely.
    // Each bridge state change (greeting, reaction, etc.) restarts this effect and cancels the
    // previous timer, so active reactions are never interrupted by the idle cycle.
    LaunchedEffect(orchestrator, bridgeState) {
        orchestrator.synchronize(bridgeState, variantCategoryBias)
        onOrchestratorDiagnosticsChanged?.invoke(orchestrator.getDiagnostics())
        while (isActive) {
            delay(IDLE_VARIANT_ADVANCE_INTERVAL_MS)
            orchestrator.forceAdvanceVariant(bridgeState, variantCategoryBias)
            onOrchestratorDiagnosticsChanged?.invoke(orchestrator.getDiagnostics())
        }
    }

    AnimatedPixelPetAvatar(
        animationController = animationController,
        modifier = modifier,
        size = 220.dp,
        onTap = onTap,
        onLongPress = onLongPress
    )
}

private const val IDLE_VARIANT_ADVANCE_INTERVAL_MS = 5_000L
