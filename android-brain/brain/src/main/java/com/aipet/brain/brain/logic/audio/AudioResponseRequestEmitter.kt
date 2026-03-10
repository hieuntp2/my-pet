package com.aipet.brain.brain.logic.audio

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload

data class AudioResponseRequestInput(
    val stimulus: AudioStimulus?,
    val category: String,
    val clipId: String? = null,
    val priority: Int? = null,
    val interruptPolicy: String? = null,
    val cooldownKey: String? = null
)

class AudioResponseRequestEmitter(
    private val eventBus: EventBus,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun emitFromStimulus(input: AudioResponseRequestInput): Boolean {
        val stimulus = input.stimulus ?: run {
            Log.w(
                TAG,
                "Skipped ${EventType.AUDIO_RESPONSE_REQUESTED.name}: stimulus is missing."
            )
            return false
        }
        val category = input.category.trim()
        if (category.isBlank()) {
            Log.w(
                TAG,
                "Skipped ${EventType.AUDIO_RESPONSE_REQUESTED.name}: category is blank. " +
                    "sourceEvent=${stimulus.sourceEventType.name}"
            )
            return false
        }

        val timestampMs = nowProvider()
        val payload = AudioResponseRequestPayload(
            category = category,
            clipId = input.clipId?.trim()?.ifBlank { null },
            priority = input.priority ?: DEFAULT_PRIORITY,
            interruptPolicy = input.interruptPolicy?.trim()?.ifBlank { null }
                ?: DEFAULT_INTERRUPT_POLICY,
            cooldownKey = input.cooldownKey?.trim()?.ifBlank { null } ?: category,
            timestamp = timestampMs
        )

        return try {
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.AUDIO_RESPONSE_REQUESTED,
                    timestampMs = timestampMs,
                    payloadJson = payload.toJson()
                )
            )
            Log.d(
                TAG,
                "Published ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                    "category=$category, sourceEvent=${stimulus.sourceEventType.name}, " +
                    "stimulusTimestamp=${stimulus.timestampMs}, priority=${payload.priority ?: -1}, " +
                    "interruptPolicy=${payload.interruptPolicy ?: "-"}, cooldownKey=${payload.cooldownKey ?: "-"}"
            )
            true
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to publish ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                    "category=$category, sourceEvent=${stimulus.sourceEventType.name}",
                error
            )
            false
        }
    }

    companion object {
        private const val TAG = "AudioRequestEmitter"
        private const val DEFAULT_PRIORITY = 1
        private const val DEFAULT_INTERRUPT_POLICY = "INTERRUPT_NONE"
    }
}
