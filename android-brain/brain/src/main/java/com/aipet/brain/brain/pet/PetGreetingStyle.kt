package com.aipet.brain.brain.pet

/**
 * How the pet greets — not just its emotional label, but its approach style.
 * The same emotion can be delivered with very different styles (warm vs hesitant).
 */
enum class PetGreetingStyle {
    WARM,       // enthusiastic, affectionate open
    GENTLE,     // soft, tender — high bond but moderate energy
    SLEEPY,     // slow, drowsy open
    PLAYFUL,    // bouncy, stimulation-ready
    NEEDY,      // seeking reassurance quietly
    HESITANT,   // cautious approach; trust is not fully comfortable
    DISTANT,    // cooled warmth; neglect has lowered openness
    RELIEVED    // warmth after recent repair or emotionally charged return
}
