package com.aipet.brain.app.ui.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.ui.audio.model.MicrophonePermissionState
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponsePayload
import com.aipet.brain.memory.objects.ObjectRecord
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.ui.avatar.AvatarFace
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    petName: String,
    petEmotion: PetEmotion,
    appOpenGreeting: PetGreetingReaction?,
    petVisibleReaction: PetVisibleReaction?,
    microphonePermissionState: MicrophonePermissionState,
    latestEvent: EventEnvelope?,
    recentInteractions: List<EventEnvelope>,
    topPersons: List<PersonRecord>,
    recentObjects: List<ObjectRecord>,
    onPetTap: () -> Unit,
    onPetLongPress: () -> Unit,
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    onNavigateToDebug: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToDiary: () -> Unit
) {
    val avatarHomeState = remember {
        AvatarHomeState()
    }
    val homeStatusMessage = remember(
        petName,
        petEmotion,
        appOpenGreeting?.message,
        petVisibleReaction?.reactionId,
        petVisibleReaction?.source
    ) {
        when {
            appOpenGreeting != null -> appOpenGreeting.message
            petVisibleReaction != null -> "$petName is reacting to ${petVisibleReaction.source.replace('_', ' ')}."
            else -> when (petEmotion) {
                PetEmotion.HAPPY -> "$petName looks happy to see you."
                PetEmotion.EXCITED -> "$petName is full of energy."
                PetEmotion.CURIOUS -> "$petName is watching for something interesting."
                PetEmotion.SLEEPY -> "$petName is sleepy, but still paying attention."
                PetEmotion.SAD -> "$petName needs a little attention."
                PetEmotion.HUNGRY -> "$petName is ready for a snack."
                PetEmotion.IDLE -> "$petName is calmly hanging out with you."
            }
        }
    }
    val audioAvailabilityMessage = remember(microphonePermissionState) {
        when (microphonePermissionState) {
            MicrophonePermissionState.NotRequested -> {
                "Voice listening is optional and stays off until you enable microphone access."
            }
            MicrophonePermissionState.Requesting -> {
                "Waiting for microphone permission so voice listening can start."
            }
            is MicrophonePermissionState.Denied -> {
                "Microphone access is off. Touch, camera, memory, and pet reactions still work."
            }
            MicrophonePermissionState.Granted -> {
                "Microphone access is available for audio listening features."
            }
        }
    }

    LaunchedEffect(petEmotion, avatarHomeState) {
        avatarHomeState.setEmotion(PetEmotionAvatarMapper.toAvatarEmotion(petEmotion))
    }

    LaunchedEffect(petVisibleReaction?.reactionId, avatarHomeState) {
        val reaction = petVisibleReaction ?: return@LaunchedEffect
        avatarHomeState.applyTemporaryEmotionOverride(
            PetEmotionAvatarMapper.toAvatarEmotion(reaction.emotion)
        )
        delay(reaction.durationMs)
        avatarHomeState.clearTemporaryEmotionOverride()
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Home")
        Text(
            text = "Pet: $petName",
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = "Emotion: ${petEmotion.name}",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Latest event: ${latestEvent?.type ?: "None"}",
            modifier = Modifier.padding(top = 4.dp)
        )
        if (latestEvent == null) {
            Text(
                text = "No activity yet.",
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        StatusCard(
            title = "Pet status",
            body = homeStatusMessage
        )
        StatusCard(
            title = "Audio listening",
            body = audioAvailabilityMessage
        )
        AvatarFace(
            avatarState = avatarHomeState.currentAvatarState,
            modifier = Modifier.padding(vertical = 8.dp),
            onTap = onPetTap,
            onLongPress = onPetLongPress
        )
        Text(
            text = "Tap or long press the avatar.",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onPetTap,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Pet")
        }

        ActivitiesSection(
            onFeedPet = onFeedPet,
            onPlayWithPet = onPlayWithPet,
            onLetPetRest = onLetPetRest,
            modifier = Modifier
        )

        RecentInteractionsCard(
            interactions = recentInteractions,
            modifier = Modifier
        )

        TopPersonsCard(
            persons = topPersons,
            modifier = Modifier
        )

        RecentObjectsCard(
            objects = recentObjects,
            modifier = Modifier
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNavigateToCamera) {
                Text(text = "Go to Camera")
            }
            Button(onClick = onNavigateToDiary) {
                Text(text = "Diary")
            }
            Button(onClick = onNavigateToDebug) {
                Text(text = "Go to Debug")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

private const val TAG = "HomeScreen"

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
