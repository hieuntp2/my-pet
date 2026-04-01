package com.aipet.brain.brain.evolution

/**
 * Classifies how the pet should treat the current app-open event.
 * Richer than AbsenceBucket — includes recovery states and habit expectation.
 */
enum class ReunionType {
    /** User came back within ~30 min — nearly continuous. */
    QUICK_RETURN,
    /** Normal gap within the user's usual routine window. */
    ROUTINE_RETURN,
    /** Later than usual but within same day. */
    LATE_RETURN,
    /** Multi-hour or multi-day absence (no prior neglect). */
    LONG_ABSENCE,
    /** First return after a repeated neglect pattern — cautious recovery. */
    RECOVERY_RETURN,
    /** User missed their expected window — pet was "waiting". */
    MISSED_EXPECTED_RETURN
}
