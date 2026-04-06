package com.aipet.brain.app.behavior.experience

import com.aipet.brain.brain.pet.PetEmotion

data class EmotionInertiaDebugState(
    val baseEmotion: PetEmotion,
    val momentumDriver: String,
    val momentumStrength: Float,
    val finalEmotion: PetEmotion
) {
    companion object {
        val DEFAULT = EmotionInertiaDebugState(
            baseEmotion = PetEmotion.IDLE,
            momentumDriver = "none",
            momentumStrength = 0f,
            finalEmotion = PetEmotion.IDLE
        )
    }
}
