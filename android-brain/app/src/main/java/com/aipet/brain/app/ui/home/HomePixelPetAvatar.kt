package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.pixel.bridge.DefaultPixelPetStateMapper
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestrator
import com.aipet.brain.ui.avatar.pixel.mock.MockPixelPetAnimationPack
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionStrategy
import com.aipet.brain.ui.avatar.pixel.ui.OrchestratedPixelPetAvatar

@Composable
fun HomePixelPetAvatar(
    avatarSignal: HomePixelPetAvatarSignal,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val registry = remember { MockPixelPetAnimationPack.createRegistry() }
    val animationController = remember { PixelAnimationController() }
    val variantSelector = remember {
        PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.SimpleRoundRobin
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
    val bridgeStateAdapter = remember { RealPixelPetBridgeStateAdapter() }
    val bridgeState = remember(avatarSignal) {
        bridgeStateAdapter.map(avatarSignal)
    }

    OrchestratedPixelPetAvatar(
        orchestrator = orchestrator,
        animationController = animationController,
        bridgeState = bridgeState,
        modifier = modifier,
        size = 220.dp,
        onTap = onTap,
        onLongPress = onLongPress
    )
}
