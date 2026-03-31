package com.aipet.brain.ui.avatar.model

/**
 * Canonical animation intent describing the purpose/context of the current animation.
 * Maps a pet state + event into a named animation category used by the animation pack.
 */
enum class AnimationIntent {
    /** Pet is resting without active engagement — slow bob, blink loop */
    IDLE_RESTING,
    /** Pet noticed something — slightly raised attention, gaze shift */
    IDLE_ATTENTIVE,
    /** Soft, gentle greeting — small nod or smile blink */
    GREET_SOFT,
    /** Excited greeting after long absence or high-energy bond */
    GREET_EXCITED,
    /** Tap acknowledged while happy/normal state */
    REACT_TAP_HAPPY,
    /** Tap acknowledged while sleepy or low-energy */
    REACT_TAP_SLEEPY,
    /** Tap acknowledged with warm/attached response */
    REACT_TAP_ATTACHED,
    /** Pet heard a sound — attentive listening posture */
    REACT_SOUND_LISTEN,
    /** Pet heard a sudden sound — small startle */
    REACT_SOUND_STARTLE,
    /** Feed activity completed successfully — satisfied expression */
    ACTIVITY_FEED_SATISFIED,
    /** Play activity completed — bouncy energetic expression */
    ACTIVITY_PLAY_BOUNCY,
    /** Rest activity — settling down, eyes drooping */
    ACTIVITY_REST_SETTLE,
    /** Pet is needy/hungry — seeking attention motion */
    NEEDY_HUNGRY_PROMPT,
    /** Pet is drifting toward sleep — slow settle */
    SLEEPY_DRIFT
}
