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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.ui.navigation.PetPrimaryDestination
import com.aipet.brain.app.ui.navigation.PetPrimaryNavigationBar
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState

@Composable
fun HomeScreen(
    homeUiModel: HomeUiModel,
    homeInteractionUiState: HomeInteractionUiState,
    avatarBridgeState: PixelPetBridgeState,
    appOpenGreeting: PetGreetingReaction?,
    onPetTap: () -> Unit,
    onPetLongPress: () -> Unit,
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = homeUiModel.petName,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = homeUiModel.identityLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MoodPill(moodLabel = homeUiModel.moodLabel)
            homeUiModel.personalityLabel?.let { personalityLabel ->
                PersonalityPill(personalityLabel = personalityLabel)
            }
        }

        if (appOpenGreeting != null) {
            StatusCard(
                title = "Greeting",
                body = appOpenGreeting.message
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PetAvatarSurface(
                    avatarBridgeState = avatarBridgeState,
                    onTap = { if (homeInteractionUiState.canTapPet) onPetTap() },
                    onLongPress = { if (homeInteractionUiState.canLongPressPet) onPetLongPress() }
                )
                Text(
                    text = homeInteractionUiState.interactionHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        StatusCard(
            title = "How ${homeUiModel.petName} is doing",
            body = homeUiModel.statusLine
        )

        homeUiModel.todaySummary?.let { todaySummary ->
            StatusCard(
                title = todaySummary.title,
                body = todaySummary.body
            )
        }

        KnownEntityCountsSection(
            knownPersons = homeUiModel.knownPersons,
            knownObjects = homeUiModel.knownObjects
        )

        if (homeInteractionUiState.feedbackMessage != null) {
            StatusCard(
                title = if (homeInteractionUiState.feedbackIsBlocked) {
                    "Give it a moment"
                } else {
                    "Just now"
                },
                body = homeInteractionUiState.feedbackMessage
            )
        }

        PetStateIndicatorsSection(
            indicators = homeUiModel.indicators
        )

        ActivitiesSection(
            onFeedPet = onFeedPet,
            onPlayWithPet = onPlayWithPet,
            onLetPetRest = onLetPetRest,
            careHint = homeInteractionUiState.careHint,
            cooldownHint = homeInteractionUiState.cooldownHint,
            modifier = Modifier
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title)
            Text(
                text = body,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MoodPill(
    moodLabel: String
) {
    Card {
        Text(
            text = moodLabel,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun PersonalityPill(
    personalityLabel: String
) {
    Card {
        Text(
            text = "Personality: $personalityLabel",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PetAvatarSurface(
    avatarBridgeState: PixelPetBridgeState,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    HomePixelPetAvatar(
        bridgeState = avatarBridgeState,
        modifier = Modifier.size(220.dp),
        onTap = onTap,
        onLongPress = onLongPress
    )
}

@Composable
private fun KnownEntityCountsSection(
    knownPersons: List<HomeKnownEntityCount>,
    knownObjects: List<HomeKnownEntityCount>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KnownEntityCountCard(
            title = "Known people",
            items = knownPersons,
            emptyMessage = "No known people yet."
        )
        KnownEntityCountCard(
            title = "Known objects",
            items = knownObjects,
            emptyMessage = "No known objects yet."
        )
    }
}

@Composable
private fun KnownEntityCountCard(
    title: String,
    items: List<HomeKnownEntityCount>,
    emptyMessage: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (items.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Seen ${item.seenCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetStateIndicatorsSection(
    indicators: List<HomeStateIndicator>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "At a glance",
            style = MaterialTheme.typography.titleMedium
        )
        indicators.chunked(2).forEach { rowIndicators ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowIndicators.forEach { indicator ->
                    StateIndicatorCard(
                        indicator = indicator,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowIndicators.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = indicator.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = indicator.value,
                style = MaterialTheme.typography.titleSmall
            )
            LinearProgressIndicator(
                progress = indicator.progress,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
