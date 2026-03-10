package com.aipet.brain.brain.logic.audio

import android.util.Log
import kotlinx.coroutines.flow.collect

class VoiceActivityAcknowledgmentRule(
    private val audioStimulusObserver: AudioStimulusObserver,
    private val audioResponseRequestEmitter: AudioResponseRequestEmitter,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
    private val ruleCooldownMs: Long = DEFAULT_RULE_COOLDOWN_MS
) {
    init {
        require(minConfidence in 0f..1f) { "minConfidence must be in [0, 1]" }
        require(ruleCooldownMs >= 0L) { "ruleCooldownMs must be >= 0" }
    }

    suspend fun observeStimuliAndReact() {
        audioStimulusObserver.observeLatestStimulus().collect { stimulus ->
            val voiceStimulus = stimulus as? VoiceActivityStimulus ?: return@collect
            handleVoiceStimulus(voiceStimulus)
        }
    }

    private suspend fun handleVoiceStimulus(stimulus: VoiceActivityStimulus) {
        Log.d(
            TAG,
            "Received voice stimulus. state=${stimulus.state.name}, " +
                "confidence=${formatConfidence(stimulus.confidence)}, " +
                "vadState=${stimulus.vadState ?: "-"}, source=${stimulus.sourceEventType.name}, " +
                "timestamp=${stimulus.timestampMs}"
        )

        if (stimulus.state != VoiceActivityStimulusState.STARTED) {
            Log.d(
                TAG,
                "Ignored voice stimulus: state=${stimulus.state.name}, expected=${VoiceActivityStimulusState.STARTED.name}"
            )
            return
        }

        if (!stimulus.confidence.isFinite() || stimulus.confidence !in 0f..1f) {
            Log.w(
                TAG,
                "Ignored malformed voice stimulus: invalid confidence=${stimulus.confidence}"
            )
            return
        }

        if (stimulus.confidence < minConfidence) {
            Log.d(
                TAG,
                "Ignored weak voice stimulus: confidence=${formatConfidence(stimulus.confidence)} " +
                    "threshold=${formatConfidence(minConfidence)}"
            )
            return
        }

        val nowMs = nowProvider()
        val elapsedSinceLastTriggerMs = nowMs - lastTriggeredAtMs
        if (lastTriggeredAtMs > 0L && elapsedSinceLastTriggerMs in 0 until ruleCooldownMs) {
            Log.d(
                TAG,
                "Skipped voice acknowledgment due to internal cooldown. " +
                    "elapsedMs=$elapsedSinceLastTriggerMs cooldownMs=$ruleCooldownMs"
            )
            return
        }

        Log.d(
            TAG,
            "Activating voice acknowledgment reaction. category=$ACKNOWLEDGMENT_CATEGORY, " +
                "confidence=${formatConfidence(stimulus.confidence)}"
        )

        val emitted = audioResponseRequestEmitter.emitFromStimulus(
            AudioResponseRequestInput(
                stimulus = stimulus,
                category = ACKNOWLEDGMENT_CATEGORY,
                cooldownKey = RULE_COOLDOWN_KEY
            )
        )

        if (emitted) {
            lastTriggeredAtMs = nowMs
            Log.d(
                TAG,
                "Voice acknowledgment request emitted. category=$ACKNOWLEDGMENT_CATEGORY, " +
                    "stimulusTs=${stimulus.timestampMs}"
            )
        } else {
            Log.w(
                TAG,
                "Voice acknowledgment request was not emitted. category=$ACKNOWLEDGMENT_CATEGORY"
            )
        }
    }

    private fun formatConfidence(value: Float): String {
        return String.format(java.util.Locale.US, "%.3f", value)
    }

    private var lastTriggeredAtMs: Long = 0L

    companion object {
        private const val TAG = "VoiceAckRule"
        private const val ACKNOWLEDGMENT_CATEGORY = "ACKNOWLEDGMENT"
        private const val RULE_COOLDOWN_KEY = "voice_activity_acknowledgment"
        private const val DEFAULT_MIN_CONFIDENCE = 0.03f
        private const val DEFAULT_RULE_COOLDOWN_MS = 1_000L
    }
}
