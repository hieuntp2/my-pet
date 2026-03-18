package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.pet.PetEmotion

data class PetVisibleReaction(
    val reactionId: Long,
    val emotion: PetEmotion,
    val durationMs: Long,
    val source: String
)
