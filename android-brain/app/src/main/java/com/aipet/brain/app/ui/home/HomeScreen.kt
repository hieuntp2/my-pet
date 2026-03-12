package com.aipet.brain.app.ui.home

import android.util.Log
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
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponsePayload
import com.aipet.brain.memory.objects.ObjectRecord
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.ui.avatar.AvatarFace

@Composable
fun HomeScreen(
    currentBrainState: BrainState,
    latestEvent: EventEnvelope?,
    recentInteractions: List<EventEnvelope>,
    topPersons: List<PersonRecord>,
    recentObjects: List<ObjectRecord>,
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

    LaunchedEffect(latestEvent?.eventId, avatarHomeState) {
        val event = latestEvent ?: return@LaunchedEffect
        when (event.type) {
            EventType.AUDIO_RESPONSE_STARTED -> {
                Log.d(TAG, "Playback event received: ${event.type.name}")
                val payload = AudioResponsePayload.fromJson(event.payloadJson)
                if (payload == null) {
                    Log.w(
                        TAG,
                        "Ignored ${event.type.name}: invalid payload. payload=${event.payloadJson}"
                    )
                    return@LaunchedEffect
                }
                val reactionEmotion = AudioPlaybackAvatarReactionMapper
                    .toAvatarEmotion(payload.category)
                if (reactionEmotion == null) {
                    Log.d(
                        TAG,
                        "No avatar reaction mapping for category=${payload.category}"
                    )
                    return@LaunchedEffect
                }
                val applied = avatarHomeState.applyTemporaryEmotionOverride(reactionEmotion)
                if (applied) {
                    Log.d(
                        TAG,
                        "Applied temporary avatar reaction. category=${payload.category}, " +
                            "emotion=${reactionEmotion.name}"
                    )
                }
            }

            EventType.AUDIO_RESPONSE_COMPLETED -> {
                Log.d(TAG, "Playback event received: ${event.type.name}")
                val restored = avatarHomeState.clearTemporaryEmotionOverride()
                if (restored) {
                    Log.d(TAG, "Avatar temporary reaction restored after playback completion.")
                }
            }

            EventType.AUDIO_RESPONSE_SKIPPED -> {
                Log.d(
                    TAG,
                    "Playback event received: ${event.type.name}. Avatar state unchanged."
                )
            }

            else -> Unit
        }
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
        if (latestEvent == null) {
            Text(
                text = "No activity yet.",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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

        RecentObjectsCard(
            objects = recentObjects,
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

private const val TAG = "HomeScreen"
