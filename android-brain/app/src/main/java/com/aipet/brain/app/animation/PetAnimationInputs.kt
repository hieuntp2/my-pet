package com.aipet.brain.app.animation

/**
 * Stable animation-facing grammar for future Rive state machines and any temporary adapters.
 *
 * This model intentionally uses small enums/bands instead of raw runtime values so animation
 * runtimes can stay decoupled from product-state internals.
 */
data class PetAnimationFrame(
    val emotion: PetAnimationEmotion = PetAnimationEmotion.CALM,
    val energyBand: PetAnimationBand = PetAnimationBand.MID,
    val hungerBand: PetAnimationBand = PetAnimationBand.MID,
    val socialBand: PetAnimationBand = PetAnimationBand.MID,
    val flavor: PetAnimationFlavor = PetAnimationFlavor.BALANCED,
    val trigger: PetAnimationTrigger = PetAnimationTrigger.none()
)

enum class PetAnimationEmotion {
    CALM,
    HAPPY,
    CURIOUS,
    SLEEPY,
    SAD,
    EXCITED,
    HUNGRY
}

enum class PetAnimationBand {
    LOW,
    MID,
    HIGH
}

enum class PetAnimationFlavor {
    BALANCED,
    PLAYFUL,
    CALM,
    CURIOUS,
    AFFECTIONATE
}

data class PetAnimationTrigger(
    val reactionType: PetAnimationReactionType,
    val emotion: PetAnimationEmotion? = null,
    val greetingType: PetAnimationGreetingType? = null,
    val activityResult: PetAnimationActivityResult? = null
) {
    companion object {
        fun none(): PetAnimationTrigger {
            return PetAnimationTrigger(reactionType = PetAnimationReactionType.NONE)
        }
    }
}

enum class PetAnimationReactionType {
    NONE,
    GREETING,
    TAP,
    LONG_PRESS,
    FEED,
    PLAY,
    REST,
    SOUND,
    REACTION
}

enum class PetAnimationGreetingType {
    CALM,
    WARM,
    HUNGRY,
    SLEEPY,
    LONELY,
    CURIOUS,
    PLAYFUL
}

enum class PetAnimationActivityResult {
    FEED,
    PLAY,
    REST
}
