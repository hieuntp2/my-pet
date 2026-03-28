package com.aipet.brain.app.audio

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioIntent
import com.aipet.brain.brain.events.audio.LocalAudioIntentEvent
import com.aipet.brain.brain.state.BrainState
import kotlinx.coroutines.flow.collect

internal class LocalAudioIntentCommandRule(
    private val eventBus: EventBus,
    private val currentBrainState: () -> BrainState,
    private val isTeachPersonFlowActive: () -> Boolean,
    private val isTeachObjectFlowActive: () -> Boolean,
    private val isExclusiveFlowActive: () -> Boolean,
    private val isPlayRandomEntryAvailable: () -> Boolean = { true },
    private val localAudioIntentMapper: LocalAudioIntentMapper = LocalAudioIntentMapper(),
    private val onAcceptedAudioFeedback: suspend (AudioIntent) -> Boolean = { false },
    private val onAcceptedAvatarReaction: suspend (AudioIntent) -> Boolean = { false },
    private val onWakeUp: suspend (Long) -> Unit,
    private val onLearnPerson: suspend () -> Unit,
    private val onLearnObject: suspend () -> Unit,
    private val onPlayRandom: suspend () -> Unit
) {
    suspend fun observeEventsAndRoute() {
        eventBus.observe().collect { event ->
            if (event.type != EventType.LOCAL_AUDIO_INTENT_DETECTED) {
                return@collect
            }
            val localIntentEvent = LocalAudioIntentEvent.fromJson(event.payloadJson)
            if (localIntentEvent == null) {
                Log.w(TAG, "Ignored ${event.type.name}: invalid payload.")
                return@collect
            }
            Log.d(
                TAG,
                "Local audio intent received. intent=${localIntentEvent.intent.name} " +
                    "rawText=\"${localIntentEvent.rawText}\" confidence=${localIntentEvent.confidence}"
            )
            routeIntent(localIntentEvent, event.timestampMs)
        }
    }

    private suspend fun routeIntent(
        localIntentEvent: LocalAudioIntentEvent,
        timestampMs: Long
    ) {
        // normalizedText is re-derived here from rawText for trace logging purposes only;
        // routing decisions are based on localIntentEvent.intent which is mapped upstream.
        val normalizedText = localAudioIntentMapper.normalize(localIntentEvent.rawText)
        val brainState = currentBrainState()
        if (brainState == BrainState.SLEEPY && localIntentEvent.intent != AudioIntent.WAKE_UP) {
            logTrace(
                VoiceInteractionTrace(
                    rawText = localIntentEvent.rawText,
                    normalizedText = normalizedText,
                    intent = localIntentEvent.intent,
                    accepted = false,
                    action = "REJECT_SLEEPING_ONLY_WAKE_UP",
                    audioPlayed = false,
                    avatarPlayed = false
                )
            )
            return
        }

        when (localIntentEvent.intent) {
            AudioIntent.WAKE_UP -> {
                if (brainState != BrainState.SLEEPY) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = true,
                            action = "NO_OP_ALREADY_AWAKE_${brainState.name}",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                onWakeUp(timestampMs)
                val audioPlayed = onAcceptedAudioFeedback(AudioIntent.WAKE_UP)
                val avatarPlayed = onAcceptedAvatarReaction(AudioIntent.WAKE_UP)
                logTrace(
                    VoiceInteractionTrace(
                        rawText = localIntentEvent.rawText,
                        normalizedText = normalizedText,
                        intent = localIntentEvent.intent,
                        accepted = true,
                        action = "ROUTE_WAKE_FOCUS",
                        audioPlayed = audioPlayed,
                        avatarPlayed = avatarPlayed
                    )
                )
            }

            AudioIntent.LEARN_PERSON -> {
                if (isTeachPersonFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_PERSON_ALREADY_ACTIVE",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                if (isTeachObjectFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_PERSON_OBJECT_FLOW_ACTIVE",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                if (isExclusiveFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_PERSON_EXCLUSIVE_FLOW",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                onLearnPerson()
                val audioPlayed = onAcceptedAudioFeedback(AudioIntent.LEARN_PERSON)
                val avatarPlayed = onAcceptedAvatarReaction(AudioIntent.LEARN_PERSON)
                logTrace(
                    VoiceInteractionTrace(
                        rawText = localIntentEvent.rawText,
                        normalizedText = normalizedText,
                        intent = localIntentEvent.intent,
                        accepted = true,
                        action = "ROUTE_LEARN_PERSON_TEACH_FLOW",
                        audioPlayed = audioPlayed,
                        avatarPlayed = avatarPlayed
                    )
                )
            }

            AudioIntent.LEARN_OBJECT -> {
                if (isTeachObjectFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_OBJECT_ALREADY_ACTIVE",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                if (isTeachPersonFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_OBJECT_PERSON_FLOW_ACTIVE",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                if (isExclusiveFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_LEARN_OBJECT_EXCLUSIVE_FLOW",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                onLearnObject()
                val audioPlayed = onAcceptedAudioFeedback(AudioIntent.LEARN_OBJECT)
                val avatarPlayed = onAcceptedAvatarReaction(AudioIntent.LEARN_OBJECT)
                logTrace(
                    VoiceInteractionTrace(
                        rawText = localIntentEvent.rawText,
                        normalizedText = normalizedText,
                        intent = localIntentEvent.intent,
                        accepted = true,
                        action = "ROUTE_LEARN_OBJECT_UNKNOWN_OBJECT_TEACH_FLOW",
                        audioPlayed = audioPlayed,
                        avatarPlayed = avatarPlayed
                    )
                )
            }

            AudioIntent.PLAY_RANDOM -> {
                if (!isPlayRandomEntryAvailable()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_PLAY_RANDOM_NO_ENTRYPOINT",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                if (isExclusiveFlowActive()) {
                    logTrace(
                        VoiceInteractionTrace(
                            rawText = localIntentEvent.rawText,
                            normalizedText = normalizedText,
                            intent = localIntentEvent.intent,
                            accepted = false,
                            action = "REJECT_PLAY_RANDOM_EXCLUSIVE_FLOW",
                            audioPlayed = false,
                            avatarPlayed = false
                        )
                    )
                    return
                }
                onPlayRandom()
                val audioPlayed = onAcceptedAudioFeedback(AudioIntent.PLAY_RANDOM)
                val avatarPlayed = onAcceptedAvatarReaction(AudioIntent.PLAY_RANDOM)
                logTrace(
                    VoiceInteractionTrace(
                        rawText = localIntentEvent.rawText,
                        normalizedText = normalizedText,
                        intent = localIntentEvent.intent,
                        accepted = true,
                        action = "ROUTE_PLAY_RANDOM_ENGAGEMENT",
                        audioPlayed = audioPlayed,
                        avatarPlayed = avatarPlayed
                    )
                )
            }

            AudioIntent.UNKNOWN -> {
                logTrace(
                    VoiceInteractionTrace(
                        rawText = localIntentEvent.rawText,
                        normalizedText = normalizedText,
                        intent = localIntentEvent.intent,
                        accepted = false,
                        action = "IGNORE_UNKNOWN",
                        audioPlayed = false,
                        avatarPlayed = false
                    )
                )
            }
        }
    }

    private fun logTrace(trace: VoiceInteractionTrace) {
        Log.d(
            TAG,
            "voice_trace raw=\"${trace.rawText}\" normalized=\"${trace.normalizedText}\" " +
                "intent=${trace.intent.name} accepted=${trace.accepted} action=${trace.action} " +
                "audioPlayed=${trace.audioPlayed} avatarPlayed=${trace.avatarPlayed}"
        )
    }

    private data class VoiceInteractionTrace(
        val rawText: String,
        val normalizedText: String,
        val intent: AudioIntent,
        val accepted: Boolean,
        val action: String,
        val audioPlayed: Boolean,
        val avatarPlayed: Boolean
    )

    companion object {
        private const val TAG = "LocalAudioIntentRule"
    }
}
