package com.aipet.brain.app.audio

import android.util.Log
import com.aipet.brain.app.ui.audio.AudioPlaybackEngine
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponsePayload
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload
import kotlinx.coroutines.flow.collect

internal class AudioResponseDispatcher(
    private val eventBus: EventBus,
    private val playbackEngine: AudioPlaybackEngine,
    private val isSoundEnabled: () -> Boolean = { true }
) {
    suspend fun observeRequestsAndDispatch() {
        eventBus.observe().collect { event ->
            if (event.type != EventType.AUDIO_RESPONSE_REQUESTED) {
                return@collect
            }
            dispatchRequestedResponse(event)
        }
    }

    private suspend fun dispatchRequestedResponse(event: EventEnvelope) {
        val request = AudioResponseRequestPayload.fromJson(event.payloadJson)
        if (request == null) {
            Log.w(
                TAG,
                "Ignored malformed ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                    "eventId=${event.eventId}, payload=${event.payloadJson}"
            )
            publishDispatcherSkippedEvent(
                requestEvent = event,
                rawCategory = "MALFORMED",
                reason = DispatcherSkipReason.MALFORMED_REQUEST
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
            publishDispatcherSkippedEvent(
                requestEvent = event,
                rawCategory = request.category,
                reason = DispatcherSkipReason.UNKNOWN_CATEGORY
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

        if (!isSoundEnabled()) {
            Log.d(
                TAG,
                "Suppressed ${EventType.AUDIO_RESPONSE_REQUESTED.name} because pet sound is disabled. " +
                    "eventId=${event.eventId}, category=${request.category}"
            )
            publishDispatcherSkippedEvent(
                requestEvent = event,
                rawCategory = request.category,
                reason = DispatcherSkipReason.SOUND_DISABLED
            )
            return
        }

        val playbackResult = playbackEngine.playRandomClipWithDetails(
            category = category,
            cooldownKey = request.cooldownKey
        )
        Log.d(
            TAG,
            "Routed audio response request to playback engine. eventId=${event.eventId}, " +
                "category=${category.label}, requestedClipId=${request.clipId ?: "-"}, " +
                "started=${playbackResult.started}, reason=${playbackResult.reason}, " +
                "selectedClip=${playbackResult.clipLogicalName ?: "-"}"
        )
    }

    private suspend fun publishDispatcherSkippedEvent(
        requestEvent: EventEnvelope,
        rawCategory: String,
        reason: DispatcherSkipReason
    ) {
        val timestampMs = requestEvent.timestampMs.takeIf { it > 0L } ?: System.currentTimeMillis()
        val normalizedCategory = rawCategory.trim().ifBlank { "UNKNOWN" }
        val payloadJson = AudioResponsePayload(
            category = normalizedCategory,
            clipId = null,
            durationMs = 0L,
            priority = 0,
            timestamp = timestampMs,
            reason = reason.name
        ).toJson()
        try {
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.AUDIO_RESPONSE_SKIPPED,
                    timestampMs = timestampMs,
                    payloadJson = payloadJson
                )
            )
            Log.d(
                TAG,
                "Published ${EventType.AUDIO_RESPONSE_SKIPPED.name}. " +
                    "eventId=${requestEvent.eventId}, category=$normalizedCategory, reason=${reason.name}"
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to publish ${EventType.AUDIO_RESPONSE_SKIPPED.name}. " +
                    "eventId=${requestEvent.eventId}, reason=${reason.name}",
                error
            )
        }
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

private enum class DispatcherSkipReason {
    MALFORMED_REQUEST,
    UNKNOWN_CATEGORY,
    SOUND_DISABLED
}
