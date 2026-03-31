package com.aipet.brain.brain.pet

/**
 * The full set of care actions the user can perform.
 * Merges and extends the former PetInteractionType + PetActivityType into one unified model.
 * Legacy enums are preserved for compatibility with existing use cases.
 */
enum class CareActionType {
    TAP,        // light touch / quick affection
    LONG_PRESS, // hold / closeness / calming
    SOOTHE,     // intentional emotional repair; best after stress or neglect
    PLAY,       // engagement and stimulation
    FEED,       // direct care; resolves hunger and builds comfort
    LINGER;     // user stays without acting — passive presence
}
