package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.logic.audio.AudioStimulus
import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.SoundStimulus
import com.aipet.brain.brain.logic.audio.VoiceActivityStimulus
import com.aipet.brain.brain.logic.audio.VoiceActivityStimulusState
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Maps audio stimuli to transient sound-reaction avatar intents and their display duration.
 *
 * Priority relative to other reactions:
 *  1. Greeting boost (absolute max)
 *  2. Tap/activity transient reaction
 *  3. Sound reaction (this mapper) — cleared by tap reactions
 *  4. Normal bridge candidate resolution (PROCESSING for keywords, etc.)
 *
 * Keyword stimuli return null: the existing [hasAudioAttention] → PROCESSING path
 * in the intent resolver already handles keyword visual feedback at higher priority.
 */
object SoundReactionPresentationMapper {

    /** Duration for the "pet is listening" attentive hold while voice is active. */
    const val ATTENTIVE_DURATION_MS: Long = 1_000L

    /** Duration for the brief look-around when an ambient sound is detected. */
    const val STARTLE_DURATION_MS: Long = 700L

    /**
     * Returns the transient intent for this stimulus, or null if no visual reaction is needed.
     */
    fun mapToAvatarIntent(stimulus: AudioStimulus): PixelPetAvatarIntent? = when (stimulus) {
        is VoiceActivityStimulus -> when (stimulus.state) {
            VoiceActivityStimulusState.STARTED -> PixelPetAvatarIntent.ATTENTIVE
            VoiceActivityStimulusState.ENDED   -> null
        }
        is SoundStimulus  -> PixelPetAvatarIntent.LOOKING  // brief startle: look around
        is KeywordStimulus -> null                          // let hasAudioAttention handle it
    }

    /**
     * Returns the display duration in milliseconds for this stimulus' reaction.
     * Returns 0 if no reaction is produced.
     */
    fun durationMs(stimulus: AudioStimulus): Long = when (stimulus) {
        is VoiceActivityStimulus -> if (stimulus.state == VoiceActivityStimulusState.STARTED) {
            ATTENTIVE_DURATION_MS
        } else {
            0L
        }
        is SoundStimulus  -> STARTLE_DURATION_MS
        is KeywordStimulus -> 0L
    }
}
