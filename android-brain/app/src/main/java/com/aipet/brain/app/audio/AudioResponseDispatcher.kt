package com.aipet.brain.app.audio

import android.util.Log
import com.aipet.brain.app.ui.audio.AudioPlaybackEngine
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload
import kotlinx.coroutines.flow.collect

internal class AudioResponseDispatcher(
    private val eventBus: EventBus,
    private val playbackEngine: AudioPlaybackEngine
) {
    suspend fun observeRequestsAndDispatch() {
        eventBus.observe().collect { event ->
            if (event.type != EventType.AUDIO_RESPONSE_REQUESTED) {
                return@collect
            }
            dispatchRequestedResponse(event)
        }
    }

    private fun dispatchRequestedResponse(event: EventEnvelope) {
        val request = AudioResponseRequestPayload.fromJson(event.payloadJson)
        if (request == null) {
            Log.w(
                TAG,
                "Ignored malformed ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                    "eventId=${event.eventId}, payload=${event.payloadJson}"
            )
            return
        }

        val category = resolveCategory(request.category)
        if (category == null) {
            Log.w(
                TAG,
                "Ignored ${EventType.AUDIO_RESPONSE_REQUESTED.name} with unknown category. " +
                    "eventId=${event.eventId}, category=${request.category}"
            )
            return
        }

        Log.d(
            TAG,
            "Received ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                "eventId=${event.eventId}, category=${request.category}, " +
                "clipId=${request.clipId ?: "-"}, priority=${request.priority ?: "-"}, " +
                "interruptPolicy=${request.interruptPolicy ?: "-"}, " +
                "cooldownKey=${request.cooldownKey ?: "-"}"
        )

        val playbackResult = playbackEngine.playRandomClipWithDetails(category)
        Log.d(
            TAG,
            "Routed audio response request to playback engine. eventId=${event.eventId}, " +
                "category=${category.label}, requestedClipId=${request.clipId ?: "-"}, " +
                "started=${playbackResult.started}, reason=${playbackResult.reason}, " +
                "selectedClip=${playbackResult.clipLogicalName ?: "-"}"
        )
    }

    private fun resolveCategory(rawCategory: String): AudioCategory? {
        val normalizedCategory = rawCategory.trim()
        if (normalizedCategory.isBlank()) {
            return null
        }
        return AudioCategory.entries.firstOrNull { category ->
            category.label.equals(normalizedCategory, ignoreCase = true) ||
                category.name.equals(normalizedCategory, ignoreCase = true)
        }
    }

    companion object {
        private const val TAG = "AudioResponseDispatcher"
    }
}
