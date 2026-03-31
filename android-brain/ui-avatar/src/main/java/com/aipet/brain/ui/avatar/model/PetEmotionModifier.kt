package com.aipet.brain.ui.avatar.model

/**
 * Optional micro-state modifier that adjusts the visual expression of the dominant emotion.
 * Does not replace the emotion — only biases visual detail (speed, posture intensity, etc.).
 */
enum class PetEmotionModifier {
    /** Sluggish, slow blink — combined with SLEEPY or post-sleep */
    GROGGY,
    /** Energetic, springy motion — combined with HAPPY/EXCITED */
    BOUNCY,
    /** Thoughtful, slightly tilted — combined with CURIOUS/THINKING */
    PENSIVE,
    /** Gentle, soft expression — combined with affectionate state */
    WARM,
    /** Seeking, slightly desperate expression — combined with HUNGRY */
    NEEDY
}
