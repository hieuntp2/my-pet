package com.aipet.brain.perception.audio

import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.VadResult
import com.aipet.brain.perception.audio.model.VadState

/**
 * Lightweight VAD state machine for live microphone input.
 *
 * Pipeline position:
 * AudioFrameSource -> EnergyEstimator -> VadLightStateMachine -> future event publication.
 *
 * The detector combines:
 * - energy smoothing (detector-local EMA)
 * - hysteresis (separate enter/exit thresholds)
 * - hangover timing (hold active states briefly on drop)
 */
class VadLightStateMachine(
    private val config: VadLightConfig = VadLightConfig()
) : VoiceActivityDetector {
    init {
        config.validate()
    }

    override val detectorId: String = DETECTOR_ID

    @Synchronized
    override fun processFrame(frame: AudioFrame, energyMetrics: AudioEnergyMetrics): VadResult {
        val frameTimestampMs = energyMetrics.frameTimestampMs
        val detectorInputEnergy = energyMetrics.smoothed.coerceIn(0.0, 1.0)
        val smoothedEnergy = smoothEnergy(detectorInputEnergy)
        val nextState = resolveNextState(
            currentTimestampMs = frameTimestampMs,
            smoothedEnergy = smoothedEnergy
        )
        currentState = nextState

        return VadResult(
            state = nextState,
            confidence = confidenceForState(nextState, smoothedEnergy),
            timestampMs = frameTimestampMs
        )
    }

    @Synchronized
    override fun reset() {
        currentState = VadState.SILENT
        smoothedEnergy = 0.0
        hasSmoothedEnergy = false
        soundHoldUntilMs = 0L
        voiceHoldUntilMs = 0L
    }

    private fun resolveNextState(
        currentTimestampMs: Long,
        smoothedEnergy: Double
    ): VadState {
        return when (currentState) {
            VadState.SILENT -> {
                when {
                    smoothedEnergy >= config.voiceEnterThreshold -> {
                        voiceHoldUntilMs = currentTimestampMs + config.voiceHangoverMs
                        VadState.VOICE_LIKELY
                    }

                    smoothedEnergy >= config.soundEnterThreshold -> {
                        soundHoldUntilMs = currentTimestampMs + config.soundHangoverMs
                        VadState.SOUND_PRESENT
                    }

                    else -> VadState.SILENT
                }
            }

            VadState.SOUND_PRESENT -> {
                when {
                    smoothedEnergy >= config.voiceEnterThreshold -> {
                        voiceHoldUntilMs = currentTimestampMs + config.voiceHangoverMs
                        VadState.VOICE_LIKELY
                    }

                    smoothedEnergy >= config.soundExitThreshold -> {
                        soundHoldUntilMs = currentTimestampMs + config.soundHangoverMs
                        VadState.SOUND_PRESENT
                    }

                    currentTimestampMs <= soundHoldUntilMs -> VadState.SOUND_PRESENT

                    else -> VadState.SILENT
                }
            }

            VadState.VOICE_LIKELY -> {
                when {
                    smoothedEnergy >= config.voiceExitThreshold -> {
                        voiceHoldUntilMs = currentTimestampMs + config.voiceHangoverMs
                        VadState.VOICE_LIKELY
                    }

                    currentTimestampMs <= voiceHoldUntilMs -> VadState.VOICE_LIKELY

                    smoothedEnergy >= config.soundEnterThreshold -> {
                        soundHoldUntilMs = currentTimestampMs + config.soundHangoverMs
                        VadState.SOUND_PRESENT
                    }

                    currentTimestampMs <= soundHoldUntilMs -> VadState.SOUND_PRESENT

                    else -> VadState.SILENT
                }
            }
        }
    }

    private fun smoothEnergy(rawEnergy: Double): Double {
        return if (!hasSmoothedEnergy) {
            hasSmoothedEnergy = true
            smoothedEnergy = rawEnergy
            smoothedEnergy
        } else {
            smoothedEnergy = (config.smoothingAlpha * rawEnergy) +
                ((1.0 - config.smoothingAlpha) * smoothedEnergy)
            smoothedEnergy
        }
    }

    private fun confidenceForState(state: VadState, smoothedEnergy: Double): Float {
        val confidence = when (state) {
            VadState.SILENT -> {
                val normalizedNoise = (smoothedEnergy / config.soundEnterThreshold).coerceIn(0.0, 1.0)
                1.0 - normalizedNoise
            }

            VadState.SOUND_PRESENT -> {
                val low = config.soundExitThreshold
                val high = config.voiceEnterThreshold
                ((smoothedEnergy - low) / (high - low)).coerceIn(0.0, 1.0)
            }

            VadState.VOICE_LIKELY -> {
                ((smoothedEnergy - config.voiceExitThreshold) / (1.0 - config.voiceExitThreshold))
                    .coerceIn(0.0, 1.0)
            }
        }
        return confidence.toFloat()
    }

    private var currentState: VadState = VadState.SILENT
    private var smoothedEnergy: Double = 0.0
    private var hasSmoothedEnergy: Boolean = false
    private var soundHoldUntilMs: Long = 0L
    private var voiceHoldUntilMs: Long = 0L

    companion object {
        private const val DETECTOR_ID = "vad_light_energy_v1"
    }
}

data class VadLightConfig(
    val smoothingAlpha: Double = 0.35,
    val soundEnterThreshold: Double = 0.045,
    val soundExitThreshold: Double = 0.030,
    val voiceEnterThreshold: Double = 0.120,
    val voiceExitThreshold: Double = 0.085,
    val soundHangoverMs: Long = 220L,
    val voiceHangoverMs: Long = 480L
) {
    fun validate() {
        require(smoothingAlpha in 0.0..1.0) { "smoothingAlpha must be in [0, 1]" }
        require(soundEnterThreshold in 0.0..1.0) { "soundEnterThreshold must be in [0, 1]" }
        require(soundExitThreshold in 0.0..1.0) { "soundExitThreshold must be in [0, 1]" }
        require(voiceEnterThreshold in 0.0..1.0) { "voiceEnterThreshold must be in [0, 1]" }
        require(voiceExitThreshold in 0.0..1.0) { "voiceExitThreshold must be in [0, 1]" }
        require(soundExitThreshold < soundEnterThreshold) {
            "soundExitThreshold must be lower than soundEnterThreshold"
        }
        require(soundEnterThreshold < voiceEnterThreshold) {
            "soundEnterThreshold must be lower than voiceEnterThreshold"
        }
        require(voiceExitThreshold < voiceEnterThreshold) {
            "voiceExitThreshold must be lower than voiceEnterThreshold"
        }
        require(soundHangoverMs >= 0L) { "soundHangoverMs must be >= 0" }
        require(voiceHangoverMs >= 0L) { "voiceHangoverMs must be >= 0" }
    }
}
