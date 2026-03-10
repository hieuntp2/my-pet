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

    companion object {
        private val RMS_PATTERN = Regex("\"rms\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val PEAK_PATTERN = Regex("\"peak\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val SMOOTHED_ENERGY_PATTERN = Regex("\"smoothedEnergy\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")
        private val KIND_PATTERN = Regex("\"kind\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): SoundEnergyPayload? {
            val rms = RMS_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
                ?: return null
            val peak = PEAK_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
                ?: return null
            val smoothedEnergy = SMOOTHED_ENERGY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toDoubleOrNull()
                ?: return null
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val kind = KIND_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }

            return SoundEnergyPayload(
                rms = rms,
                peak = peak,
                smoothedEnergy = smoothedEnergy,
                timestamp = timestamp,
                kind = kind
            )
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

    companion object {
        private val STATE_PATTERN = Regex("\"state\"\\s*:\\s*\"([^\"]+)\"")
        private val CONFIDENCE_PATTERN = Regex("\"confidence\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")
        private val VAD_STATE_PATTERN = Regex("\"vadState\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): VoiceActivityPayload? {
            val state = STATE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.let { rawState ->
                    VoiceActivityState.entries.firstOrNull { state ->
                        state.name == rawState
                    }
                }
                ?: return null
            val confidence = CONFIDENCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val vadState = VAD_STATE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }

            return VoiceActivityPayload(
                state = state,
                confidence = confidence,
                timestamp = timestamp,
                vadState = vadState
            )
        }
    }
}

data class AudioResponseRequestPayload(
    val category: String,
    val clipId: String? = null,
    val priority: Int? = null,
    val interruptPolicy: String? = null,
    val cooldownKey: String? = null,
    val timestamp: Long? = null
) {
    fun toJson(): String {
        return buildString(capacity = 196) {
            append("{")
            append("\"category\":\"").append(category.toJsonEscaped()).append("\"")
            if (!clipId.isNullOrBlank()) {
                append(",\"clipId\":\"").append(clipId.toJsonEscaped()).append("\"")
            }
            if (priority != null) {
                append(",\"priority\":").append(priority)
            }
            if (!interruptPolicy.isNullOrBlank()) {
                append(",\"interruptPolicy\":\"").append(interruptPolicy.toJsonEscaped()).append("\"")
            }
            if (!cooldownKey.isNullOrBlank()) {
                append(",\"cooldownKey\":\"").append(cooldownKey.toJsonEscaped()).append("\"")
            }
            if (timestamp != null) {
                append(",\"timestamp\":").append(timestamp)
            }
            append("}")
        }
    }

    companion object {
        private val CATEGORY_PATTERN = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"")
        private val CLIP_ID_PATTERN = Regex("\"clipId\"\\s*:\\s*\"([^\"]+)\"")
        private val PRIORITY_PATTERN = Regex("\"priority\"\\s*:\\s*(-?\\d+)")
        private val INTERRUPT_POLICY_PATTERN = Regex("\"interruptPolicy\"\\s*:\\s*\"([^\"]+)\"")
        private val COOLDOWN_KEY_PATTERN = Regex("\"cooldownKey\"\\s*:\\s*\"([^\"]+)\"")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")
        private val REQUESTED_AT_MS_PATTERN = Regex("\"requestedAtMs\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): AudioResponseRequestPayload? {
            val category = CATEGORY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
                ?: return null
            val clipId = CLIP_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val priority = PRIORITY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
            val interruptPolicy = INTERRUPT_POLICY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val cooldownKey = COOLDOWN_KEY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: REQUESTED_AT_MS_PATTERN.find(payloadJson)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()

            return AudioResponseRequestPayload(
                category = category,
                clipId = clipId,
                priority = priority,
                interruptPolicy = interruptPolicy,
                cooldownKey = cooldownKey,
                timestamp = timestamp
            )
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

    companion object {
        private val CATEGORY_PATTERN = Regex("\"category\"\\s*:\\s*\"([^\"]+)\"")
        private val CLIP_ID_PATTERN = Regex("\"clipId\"\\s*:\\s*\"([^\"]+)\"")
        private val DURATION_PATTERN = Regex("\"durationMs\"\\s*:\\s*(-?\\d+)")
        private val PRIORITY_PATTERN = Regex("\"priority\"\\s*:\\s*(-?\\d+)")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")
        private val REASON_PATTERN = Regex("\"reason\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): AudioResponsePayload? {
            val category = CATEGORY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.ifBlank { null }
                ?: return null
            val clipId = CLIP_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
            val durationMs = DURATION_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val priority = PRIORITY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val reason = REASON_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }

            return AudioResponsePayload(
                category = category,
                clipId = clipId,
                durationMs = durationMs,
                priority = priority,
                timestamp = timestamp,
                reason = reason
            )
        }
    }
}
