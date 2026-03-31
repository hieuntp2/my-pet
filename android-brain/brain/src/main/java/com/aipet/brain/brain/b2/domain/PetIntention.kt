package com.aipet.brain.brain.b2.domain

/**
 * Represents what the pet "wants" to do right now.
 * Intention is not the same as animation or emotion.
 * Same intention can appear with different emotional expressions depending on state.
 */
enum class PetIntention {
    /** Low-activity idle; remain still, gentle drift. */
    REST,
    /** Drifting towards sleep; drooping, slow movement. */
    DOZE,
    /** Seek user attention; approach, orient, make presence known. */
    SEEK_ATTENTION,
    /** Seek physical comfort; cuddle-seeking behavior. */
    SEEK_COMFORT,
    /** Invite the user to play; anticipation build + invitation bubble. */
    INVITE_PLAY,
    /** Active play engagement; precise movement, high energy. */
    PLAY,
    /** Respond to a direct user action (tap, voice, long press). */
    RESPOND_TO_USER,
    /** Passively watch; calm observation, no direct engagement. */
    OBSERVE,
    /** Curious investigation of something in the environment. */
    INVESTIGATE,
    /** Request food; hungry-driven attention-seeking behavior. */
    REQUEST_FOOD,
    /** Self-soothe when overstimulated or distressed. */
    SELF_SOOTHE,
    /** Stay near the user without initiating; passive companion presence. */
    STAY_NEAR,
    /** Withdraw inward; reduced interaction, inward-facing state. */
    WITHDRAW,
    /** Listen to voice/sound with oriented attention. */
    LISTEN,
    /** Short startled recovery after unexpected loud event. */
    STARTLE_RECOVER,
    /** Brief celebratory expression (game win, feeding, greeting). */
    CELEBRATE,
    /** General recovery from a negative state or overstimulation. */
    RECOVER
}
