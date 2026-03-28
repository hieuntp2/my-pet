package com.aipet.brain.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.ui.navigation.PetPrimaryDestination
import com.aipet.brain.app.ui.navigation.PetPrimaryNavigationBar
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.ui.avatar.model.PetBubblePayload
import com.aipet.brain.ui.avatar.pixel.bridge.PixelAnimationOrchestratorDiagnostics
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState

@Composable
fun HomeScreen(
    homeUiModel: HomeUiModel,
    homeInteractionUiState: HomeInteractionUiState,
    avatarBridgeState: PixelPetBridgeState,
    appOpenGreeting: PetGreetingReaction?,
    variantCategoryBias: Map<String, Float> = emptyMap(),
    onOrchestratorDiagnosticsChanged: ((PixelAnimationOrchestratorDiagnostics) -> Unit)? = null,
    onPetTap: () -> Unit,
    onPetLongPress: () -> Unit,
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
    val bubblePolicy = rememberTalkingBubblePolicy()
    val bubblePayload by bubblePolicy.currentBubble.collectAsState()

    // Greeting uses showForce so a hot-reload or fast navigation back to Home never silently
    // drops the greeting bubble due to an active cooldown window.
    LaunchedEffect(appOpenGreeting) {
        if (appOpenGreeting != null) {
            bubblePolicy.showForce(PetBubblePayload(text = appOpenGreeting.message))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PetPrimaryNavigationBar(
            selectedDestination = PetPrimaryDestination.Home,
            onNavigateHome = onNavigateToHome,
            onNavigateDiary = onNavigateToDiary,
            onNavigateDebug = onNavigateToDebug
        )

        // --- Pet Stage (dominant focal point) ---
        PetStage(
            petName = homeUiModel.petName,
            moodLabel = homeUiModel.moodLabel,
            statusLine = homeUiModel.statusLine,
            avatarBridgeState = avatarBridgeState,
            variantCategoryBias = variantCategoryBias,
            onOrchestratorDiagnosticsChanged = onOrchestratorDiagnosticsChanged,
            bubblePayload = bubblePayload,
            canTap = homeInteractionUiState.canTapPet,
            canLongPress = homeInteractionUiState.canLongPressPet,
            feedbackMessage = homeInteractionUiState.feedbackMessage,
            feedbackIsBlocked = homeInteractionUiState.feedbackIsBlocked,
            onPetTap = onPetTap,
            onPetLongPress = onPetLongPress
        )

        // --- Quick actions ---
        ActivitiesSection(
            onFeedPet = onFeedPet,
            onPlayWithPet = onPlayWithPet,
            onLetPetRest = onLetPetRest,
            careHint = homeInteractionUiState.careHint,
            cooldownHint = homeInteractionUiState.cooldownHint,
            modifier = Modifier
        )

        // --- Lightweight state indicators ---
        PetStateIndicatorsSection(
            indicators = homeUiModel.indicators
        )

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PetStage(
    petName: String,
    moodLabel: String,
    statusLine: String,
    avatarBridgeState: PixelPetBridgeState,
    variantCategoryBias: Map<String, Float> = emptyMap(),
    onOrchestratorDiagnosticsChanged: ((PixelAnimationOrchestratorDiagnostics) -> Unit)? = null,
    bubblePayload: PetBubblePayload?,
    canTap: Boolean,
    canLongPress: Boolean,
    feedbackMessage: String?,
    feedbackIsBlocked: Boolean,
    onPetTap: () -> Unit,
    onPetLongPress: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pet name and mood — lightweight header
        Text(
            text = petName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        MoodPill(moodLabel = moodLabel)

        // Avatar — primary focal point
        HomePixelPetAvatar(
            bridgeState = avatarBridgeState,
            modifier = Modifier.size(280.dp),
            variantCategoryBias = variantCategoryBias,
            onOrchestratorDiagnosticsChanged = onOrchestratorDiagnosticsChanged,
            onTap = { if (canTap) onPetTap() },
            onLongPress = { if (canLongPress) onPetLongPress() }
        )

        // Talking bubble — auto-dismissing pet speech anchored below the avatar
        PetTalkingBubble(
            payload = bubblePayload,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Feedback result — shown only when present
        if (feedbackMessage != null) {
            Text(
                text = feedbackMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (feedbackIsBlocked) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.secondary
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        // Status line — single line of wellbeing text
        Text(
            text = statusLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun MoodPill(
    moodLabel: String
) {
    Card {
        Text(
            text = moodLabel,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun PetStateIndicatorsSection(
    indicators: List<HomeStateIndicator>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        indicators.chunked(3).forEach { rowIndicators ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowIndicators.forEach { indicator ->
                    StateIndicatorCard(
                        indicator = indicator,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StateIndicatorCard(
    indicator: HomeStateIndicator,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = indicator.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = indicator.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = indicator.value,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}


