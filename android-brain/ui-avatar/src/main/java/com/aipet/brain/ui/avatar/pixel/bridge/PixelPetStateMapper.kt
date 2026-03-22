package com.aipet.brain.ui.avatar.pixel.bridge

import com.aipet.brain.ui.avatar.pixel.model.Asking
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Looking
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelPetVisualState
import com.aipet.brain.ui.avatar.pixel.model.Sleepy
import com.aipet.brain.ui.avatar.pixel.model.Thinking

fun interface PixelPetStateMapper<T> {
    fun map(state: T): PixelPetVisualState
}

enum class PixelPetAvatarIntent {
    NEUTRAL,
    ENGAGED,
    ATTENTIVE,
    LOOKING,
    ASKING,
    PROCESSING,
    LOW_ENERGY
}

data class PixelPetBridgeDebugMetadata(
    val chosenIntent: String,
    val priorityReason: String,
    val sourceSummary: String,
    val policySummary: String
) {
    fun toLogSummary(): String {
        return "intent=$chosenIntent reason=$priorityReason policy=$policySummary sources=$sourceSummary"
    }
}

data class PixelPetBridgeState(
    val intent: PixelPetAvatarIntent,
    val debugMetadata: PixelPetBridgeDebugMetadata? = null
)

class DefaultPixelPetStateMapper : PixelPetStateMapper<PixelPetBridgeState> {
    override fun map(state: PixelPetBridgeState): PixelPetVisualState {
        return when (state.intent) {
            PixelPetAvatarIntent.NEUTRAL -> Neutral
            PixelPetAvatarIntent.ENGAGED -> Happy
            PixelPetAvatarIntent.ATTENTIVE -> Curious
            PixelPetAvatarIntent.LOOKING -> Looking
            PixelPetAvatarIntent.ASKING -> Asking
            PixelPetAvatarIntent.PROCESSING -> Thinking
            PixelPetAvatarIntent.LOW_ENERGY -> Sleepy
        }
    }
}
