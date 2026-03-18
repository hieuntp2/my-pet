package com.aipet.brain.app.ui.home

data class HomeInteractionUiState(
    val feedbackMessage: String? = null,
    val feedbackIsBlocked: Boolean = false,
    val canTapPet: Boolean = true,
    val canLongPressPet: Boolean = true,
    val careHint: String = "Feed, play, or rest with one tap.",
    val interactionHint: String = "Tap for a quick hello or hold for a longer cuddle.",
    val cooldownHint: String? = null
)
