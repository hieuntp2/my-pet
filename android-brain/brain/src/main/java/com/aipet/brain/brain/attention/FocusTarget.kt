package com.aipet.brain.brain.attention

/**
 * Enumeration of possible focus target types the pet can attend to.
 */
enum class FocusTargetType {
    USER_FACE,
    USER_VOICE,
    TOUCH_SOURCE,
    SOUND_SOURCE,
    INTERNAL_NEED,
    GAME_TARGET,
    AMBIENT_SPACE,
    NONE
}

/**
 * Represents what the pet is currently paying attention to.
 *
 * @param type          The category of focus target.
 * @param id            Optional identifier (e.g., person ID, object type).
 * @param confidence    How confident we are this target is real/present.
 * @param salience      How important this target is right now.
 * @param acquiredAtMs  When attention was first locked on.
 * @param lastReinforcedAtMs  When the target was last reinforced by a signal.
 */
data class FocusTarget(
    val type: FocusTargetType = FocusTargetType.NONE,
    val id: String? = null,
    val confidence: Float = 0f,
    val salience: Float = 0f,
    val acquiredAtMs: Long = 0L,
    val lastReinforcedAtMs: Long = 0L
) {
    fun isActive(): Boolean = type != FocusTargetType.NONE && confidence > 0f

    /** How long this target has been held in ms. */
    fun holdDurationMs(nowMs: Long): Long =
        if (acquiredAtMs > 0L) (nowMs - acquiredAtMs).coerceAtLeast(0L) else 0L

    companion object {
        val NONE = FocusTarget(type = FocusTargetType.NONE)
    }
}
