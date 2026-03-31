package com.aipet.brain.brain.pet

/**
 * Emotional relationship stage — not RPG levels, but emotional depth.
 * Higher stages unlock warmer behavior ceilings, not new powers.
 */
enum class RelationshipStage {
    STRANGER,   // fresh start — pet is cautious, needs to learn user
    FAMILIAR,   // user is recognized; pet opens slightly
    ATTACHED,   // clear preference; pet seeks user more
    BONDED      // deep trust; warmest greetings and rarest affection accessible
}
