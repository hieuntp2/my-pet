package com.aipet.brain.brain.pet

/**
 * Named behavior families the pet can exhibit.
 * Each family maps to a cluster of visible reactions when selected.
 *
 * The scoring engine outputs one of these as the winner.
 */
enum class PetBehaviorFamily {
    GREET_WARM,         // enthusiastic reunion
    GREET_GENTLE,       // soft, low-key return greeting
    GREET_SLEEPY,       // drowsy, slow greeting
    GREET_DISTANT,      // cooled, hesitant
    SEEK_COMFORT,       // seeking closeness after discomfort or absence
    INVITE_PLAY,        // initiating fun
    ASK_FOR_FOOD,       // hunger-driven attention-seeking
    SETTLE_CALMLY,      // content and still
    CURIOUS_EXPLORE,    // stimulation-driven investigation
    WITHDRAW_SLIGHTLY,  // overstimulation or low patience
    RELIEF_AFTER_SOOTHE // post-repair emotional exhale
}
