package com.aipet.brain.ui.avatar.pixel.model

sealed interface PixelPetVisualState {
    val id: String

    companion object {
        val coreStates: List<PixelPetVisualState> = listOf(
            Neutral,
            Happy,
            Curious,
            Looking,
            Asking,
            Sleepy,
            Thinking,
            Excited,
            Surprised,
            Hungry,
            Lonely
        )
    }
}

object Neutral : PixelPetVisualState {
    override val id: String = "neutral"
}

object Happy : PixelPetVisualState {
    override val id: String = "happy"
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

/** Short one-shot reaction: wide bright eyes + bounce — used for greeting/win/happy reaction. */
object Excited : PixelPetVisualState {
    override val id: String = "excited"
}

/** Short one-shot reaction: snap-wide + freeze + recover — used for loud sound / spam tap. */
object Surprised : PixelPetVisualState {
    override val id: String = "surprised"
}

/** Ambient loop: drooped-attentive eyes with side-scan — shown when pet is hungry. */
object Hungry : PixelPetVisualState {
    override val id: String = "hungry"
}

/** Ambient loop: expectant soft center gaze with glance-away cycles — shown when pet is lonely. */
object Lonely : PixelPetVisualState {
    override val id: String = "lonely"
}

data class CustomPixelPetVisualState(
    override val id: String
) : PixelPetVisualState {
    init {
        require(id.isNotBlank()) { "Custom pixel pet visual state id must not be blank." }
    }
}
