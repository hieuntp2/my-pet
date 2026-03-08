package com.aipet.brain.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.ui.avatar.AvatarFace

@Composable
fun HomeScreen(
    currentBrainState: BrainState,
    latestEvent: EventEnvelope?,
    recentInteractions: List<EventEnvelope>,
    topPersons: List<PersonRecord>,
    onPetInteraction: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    val avatarHomeState = remember {
        AvatarHomeState()
    }

    LaunchedEffect(currentBrainState, avatarHomeState) {
        avatarHomeState.setEmotion(
            BrainStateAvatarMapper.toAvatarEmotion(currentBrainState)
        )
    }

    LaunchedEffect(avatarHomeState) {
        avatarHomeState.runBlinkLoop()
    }

    LaunchedEffect(avatarHomeState, "idle_loop") {
        avatarHomeState.runIdleLoop()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Home")
        Text(
            text = "Latest event: ${latestEvent?.type ?: "None"}",
            modifier = Modifier.padding(top = 8.dp)
        )
        AvatarFace(
            avatarState = avatarHomeState.currentAvatarState,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        Button(
            onClick = onPetInteraction,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(text = "Pet")
        }

        RecentInteractionsCard(
            interactions = recentInteractions,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        TopPersonsCard(
            persons = topPersons,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNavigateToCamera) {
                Text(text = "Go to Camera")
            }
            Button(onClick = onNavigateToDebug) {
                Text(text = "Go to Debug")
            }
        }
    }
}
