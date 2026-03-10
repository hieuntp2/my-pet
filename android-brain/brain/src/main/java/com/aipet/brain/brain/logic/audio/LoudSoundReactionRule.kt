package com.aipet.brain.brain.logic.audio

import android.util.Log
import kotlinx.coroutines.flow.collect

class LoudSoundReactionRule(
    private val audioStimulusObserver: AudioStimulusObserver,
    private val audioResponseRequestEmitter: AudioResponseRequestEmitter,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val loudSmoothedEnergyThreshold: Double = DEFAULT_LOUD_SMOOTHED_ENERGY_THRESHOLD,
    private val loudPeakEnergyThreshold: Double = DEFAULT_LOUD_PEAK_ENERGY_THRESHOLD,
    private val ruleCooldownMs: Long = DEFAULT_RULE_COOLDOWN_MS
) {
    init {
        require(loudSmoothedEnergyThreshold > 0.0) { "loudSmoothedEnergyThreshold must be > 0" }
        require(loudPeakEnergyThreshold > 0.0) { "loudPeakEnergyThreshold must be > 0" }
        require(ruleCooldownMs >= 0L) { "ruleCooldownMs must be >= 0" }
    }

    suspend fun observeStimuliAndReact() {
        audioStimulusObserver.observeLatestStimulus().collect { stimulus ->
            val soundStimulus = stimulus as? SoundStimulus ?: return@collect
            handleSoundStimulus(soundStimulus)
        }
    }

    private suspend fun handleSoundStimulus(stimulus: SoundStimulus) {
        Log.d(
            TAG,
            "Received sound stimulus. kind=${stimulus.kind}, smoothed=${formatEnergy(stimulus.smoothedEnergy)}, " +
                "peak=${formatEnergy(stimulus.peak)}, rms=${formatEnergy(stimulus.rms)}, " +
                "source=${stimulus.sourceEventType.name}, timestamp=${stimulus.timestampMs}"
        )

        if (!isLoud(stimulus)) {
            Log.d(
                TAG,
                "Ignored sound stimulus: below loud threshold. kind=${stimulus.kind}, " +
                    "smoothed=${formatEnergy(stimulus.smoothedEnergy)}, peak=${formatEnergy(stimulus.peak)}"
            )
            return
        }

        val nowMs = nowProvider()
        val elapsedSinceLastTriggerMs = nowMs - lastTriggeredAtMs
        if (lastTriggeredAtMs > 0L && elapsedSinceLastTriggerMs in 0 until ruleCooldownMs) {
            Log.d(
                TAG,
                "Skipped loud-sound reaction due to internal cooldown. " +
                    "elapsedMs=$elapsedSinceLastTriggerMs cooldownMs=$ruleCooldownMs"
            )
            return
        }

        Log.d(
            TAG,
            "Activating loud-sound reaction. category=$SURPRISED_CATEGORY, " +
                "smoothed=${formatEnergy(stimulus.smoothedEnergy)}, peak=${formatEnergy(stimulus.peak)}"
        )

        val emitted = audioResponseRequestEmitter.emitFromStimulus(
            AudioResponseRequestInput(
                stimulus = stimulus,
                category = SURPRISED_CATEGORY,
                cooldownKey = RULE_COOLDOWN_KEY
            )
        )
        if (emitted) {
            lastTriggeredAtMs = nowMs
            Log.d(
                TAG,
                "Loud-sound reaction request emitted. category=$SURPRISED_CATEGORY, stimulusTs=${stimulus.timestampMs}"
            )
        } else {
            Log.w(
                TAG,
                "Loud-sound reaction request was not emitted. category=$SURPRISED_CATEGORY"
            )
        }
    }

    private fun isLoud(stimulus: SoundStimulus): Boolean {
        return stimulus.smoothedEnergy >= loudSmoothedEnergyThreshold ||
            stimulus.peak >= loudPeakEnergyThreshold
    }

    private fun formatEnergy(value: Double): String {
        return String.format(java.util.Locale.US, "%.4f", value)
    }

    private var lastTriggeredAtMs: Long = 0L

    companion object {
        private const val TAG = "LoudSoundReactionRule"
        private const val SURPRISED_CATEGORY = "SURPRISED"
        private const val RULE_COOLDOWN_KEY = "loud_sound_reaction"
        private const val DEFAULT_LOUD_SMOOTHED_ENERGY_THRESHOLD = 0.25
        private const val DEFAULT_LOUD_PEAK_ENERGY_THRESHOLD = 0.45
        private const val DEFAULT_RULE_COOLDOWN_MS = 1_000L
    }
}
