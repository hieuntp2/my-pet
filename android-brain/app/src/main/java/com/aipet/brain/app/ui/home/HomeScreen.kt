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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.ui.avatar.AvatarFace
import com.aipet.brain.ui.avatar.model.AvatarEmotion

@Composable
fun HomeScreen(
    latestEvent: EventEnvelope?,
    onNavigateToDebug: () -> Unit,
    onNavigateToCamera: () -> Unit
) {
    var selectedEmotionName by rememberSaveable { mutableStateOf(AvatarEmotion.NEUTRAL.name) }
    val avatarHomeState = remember {
        AvatarHomeState()
    }

    LaunchedEffect(selectedEmotionName, avatarHomeState) {
        avatarHomeState.setEmotion(selectedEmotionName.toAvatarEmotion())
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Button(onClick = { selectedEmotionName = AvatarEmotion.NEUTRAL.name }) {
                Text(text = "Neutral")
            }
            Button(onClick = { selectedEmotionName = AvatarEmotion.HAPPY.name }) {
                Text(text = "Happy")
            }
            Button(onClick = { selectedEmotionName = AvatarEmotion.CURIOUS.name }) {
                Text(text = "Curious")
            }
            Button(onClick = { selectedEmotionName = AvatarEmotion.SLEEPY.name }) {
                Text(text = "Sleepy")
            }
        }

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

private fun String.toAvatarEmotion(): AvatarEmotion {
    return AvatarEmotion.entries.firstOrNull { it.name == this } ?: AvatarEmotion.NEUTRAL
}
