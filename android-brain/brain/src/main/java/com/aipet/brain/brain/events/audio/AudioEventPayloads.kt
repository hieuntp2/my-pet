package com.aipet.brain.brain.events.audio

import com.aipet.brain.brain.events.toJsonEscaped

/**
 * Phase 1.5 audio interaction payload contracts.
 *
 * These payloads are shared across perception, behavior, and audio-response modules.
 * They serialize into EventEnvelope.payloadJson for EventBus, Room persistence, and Event Viewer.
 */
data class AudioCaptureEventPayload(
    val sampleRate: Int,
    val frameSize: Int,
    val channelCount: Int,
    val timestamp: Long
) {
    fun toJson(): String {
        return buildString(capacity = 120) {
            append("{")
            append("\"sampleRate\":").append(sampleRate).append(",")
            append("\"frameSize\":").append(frameSize).append(",")
            append("\"channelCount\":").append(channelCount).append(",")
            append("\"timestamp\":").append(timestamp)
            append("}")
        }
    }
}

/**
 * Used by SOUND_ENERGY_CHANGED and can also represent SOUND_DETECTED snapshots.
 */
data class SoundEnergyPayload(
    val rms: Double,
    val peak: Double,
    val smoothedEnergy: Double,
    val timestamp: Long,
    val kind: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 160) {
            append("{")
            append("\"rms\":").append(rms).append(",")
            append("\"peak\":").append(peak).append(",")
            append("\"smoothedEnergy\":").append(smoothedEnergy).append(",")
            append("\"timestamp\":").append(timestamp)
            if (!kind.isNullOrBlank()) {
                append(",\"kind\":\"").append(kind.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }
}

enum class VoiceActivityState {
    STARTED,
    ENDED
}

data class VoiceActivityPayload(
    val state: VoiceActivityState,
    val confidence: Float,
    val timestamp: Long,
    val vadState: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 152) {
            append("{")
            append("\"state\":\"").append(state.name.toJsonEscaped()).append("\",")
            append("\"confidence\":").append(confidence).append(",")
            append("\"timestamp\":").append(timestamp)
            if (!vadState.isNullOrBlank()) {
                append(",\"vadState\":\"").append(vadState.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }
}

data class AudioResponsePayload(
    val category: String,
    val clipId: String? = null,
    val durationMs: Long,
    val priority: Int,
    val timestamp: Long,
    val reason: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 168) {
            append("{")
            append("\"category\":\"").append(category.toJsonEscaped()).append("\",")
            append("\"durationMs\":").append(durationMs).append(",")
            append("\"priority\":").append(priority).append(",")
            append("\"timestamp\":").append(timestamp)
            if (!clipId.isNullOrBlank()) {
                append(",\"clipId\":\"").append(clipId.toJsonEscaped()).append("\"")
            }
            if (!reason.isNullOrBlank()) {
                append(",\"reason\":\"").append(reason.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }
}
