package com.aipet.brain.brain.pet

/**
 * Categories of pet-initiated bids for attention.
 * Each type maps to a different visible expression and intensity level.
 */
enum class InvitationType {
    SOFT_CHECKIN,    // quiet social bid — pet glances over softly
    PLAY_INVITE,     // bouncy, stimulation-seeking bid
    NEEDY_LOOK,      // need-driven check-in; seeking reassurance
    CURIOUS_GLANCE   // curiosity-led engagement; low intensity
}
