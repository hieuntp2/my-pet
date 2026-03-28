package com.aipet.brain.ui.avatar.pixel.model

sealed interface PixelPetVisualState {
    val id: String

    companion object {
        val coreStates: List<PixelPetVisualState> = listOf(
            Neutral,
            Happy,
            Excited,
            Curious,
            Looking,
            Asking,
            Sleepy,
            Thinking,
            Sad,
            Hungry
        )
    }
}

object Neutral : PixelPetVisualState {
    override val id: String = "neutral"
}

object Happy : PixelPetVisualState {
    override val id: String = "happy"
}

object Excited : PixelPetVisualState {
    override val id: String = "excited"
}

object Curious : PixelPetVisualState {
    override val id: String = "curious"
}

object Looking : PixelPetVisualState {
    override val id: String = "looking"
}

object Asking : PixelPetVisualState {
    override val id: String = "asking"
}

object Sleepy : PixelPetVisualState {
    override val id: String = "sleepy"
}

object Thinking : PixelPetVisualState {
    override val id: String = "thinking"
}

object Sad : PixelPetVisualState {
    override val id: String = "sad"
}

object Hungry : PixelPetVisualState {
    override val id: String = "hungry"
}

data class CustomPixelPetVisualState(
    override val id: String
) : PixelPetVisualState {
    init {
        require(id.isNotBlank()) { "Custom pixel pet visual state id must not be blank." }
    }
}
