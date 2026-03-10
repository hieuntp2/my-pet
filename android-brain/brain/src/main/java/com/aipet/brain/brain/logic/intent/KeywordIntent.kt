package com.aipet.brain.brain.logic.intent

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.state.BrainState

enum class KeywordIntentType {
    HELLO,
    COME,
    LOOK
}

data class KeywordIntent(
    val intentType: KeywordIntentType,
    val keywordId: String,
    val timestampMs: Long,
    val confidence: Float,
    val sourceEventType: EventType
)

data class KeywordCommand(
    val intent: KeywordIntent,
    val responseCategory: String,
    val responseCooldownKey: String,
    val targetState: BrainState? = null
)
