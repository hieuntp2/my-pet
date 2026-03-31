package com.aipet.brain.brain.attention

/**
 * How the pet is currently attending — not just what it's attending to.
 * This mode controls gaze behavior, distraction susceptibility, and switch thresholds.
 */
enum class AttentionMode {
    /** No strong target; slow gaze drift, low stickiness, broad soft awareness. */
    IDLE_SCANNING,
    /** User present nearby but no direct engagement; warm re-checks, medium stickiness. */
    PASSIVE_COMPANION,
    /** Something novel has drawn curiosity; quick orient, short hold, follow-up glance. */
    CURIOUS_INSPECTION,
    /** Engaged directly with user; strong center focus, high stickiness, reduced distraction. */
    SOCIAL_LOCK,
    /** Voice activity detected; directional orient, subtle listening beat. */
    LISTENING,
    /** Unexpected or loud event; fast acquisition, short intense hold. */
    ALERT,
    /** Sleepy/low-energy mode; drifting focus, strong switch threshold, delayed re-acquisition. */
    DOZING,
    /** Mini-game or invitation active; precise target lock, fast purposeful shifts. */
    PLAY_FOCUS,
    /** Overstimulated or annoyed; reduced social lock, inward bias, slower re-engagement. */
    WITHDRAWN
}
