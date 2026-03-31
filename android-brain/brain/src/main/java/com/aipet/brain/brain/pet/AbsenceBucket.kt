package com.aipet.brain.brain.pet

/**
 * Classifies how long the user was away before opening the app.
 * Used as the primary axis of greeting style selection.
 */
enum class AbsenceBucket {
    /** User came back within ~30 minutes — pet barely noticed. */
    SHORT_RETURN,
    /** Normal gap (30 min – 3 hr) — pet has some presence update but not dramatic. */
    MEDIUM_RETURN,
    /** Meaningful absence (3 hr – 24 hr) — pet reacts based on bond and trust state. */
    LONG_RETURN,
    /** Repeated or harsh gap pattern — trust cools, warmth backs off temporarily. */
    NEGLECT_RETURN
}
