package com.aipet.brain.brain.logic.intent

import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.KeywordStimulusKind
import com.aipet.brain.brain.state.BrainState
import java.util.Locale

class KeywordIntentMapper {
    fun mapKeywordStimulus(stimulus: KeywordStimulus): KeywordCommand? {
        if (stimulus.kind != KeywordStimulusKind.KEYWORD) {
            return null
        }
        val normalizedKeywordId = stimulus.keywordId.trim().lowercase(Locale.US)
        if (normalizedKeywordId.isEmpty()) {
            return null
        }
        val definition = intentDefinitions[normalizedKeywordId] ?: return null

        val intent = KeywordIntent(
            intentType = definition.intentType,
            keywordId = stimulus.keywordId,
            timestampMs = stimulus.timestampMs,
            confidence = stimulus.confidence,
            sourceEventType = stimulus.sourceEventType
        )
        return KeywordCommand(
            intent = intent,
            responseCategory = definition.responseCategory,
            responseCooldownKey = definition.responseCooldownKey,
            targetState = definition.targetState
        )
    }

    private data class KeywordIntentDefinition(
        val intentType: KeywordIntentType,
        val responseCategory: String,
        val responseCooldownKey: String,
        val targetState: BrainState? = null
    )

    private val intentDefinitions: Map<String, KeywordIntentDefinition> = DEFAULT_INTENT_DEFINITIONS

    companion object {
        private val DEFAULT_INTENT_DEFINITIONS: Map<String, KeywordIntentDefinition> = mapOf(
            "hello" to KeywordIntentDefinition(
                intentType = KeywordIntentType.HELLO,
                responseCategory = "GREETING",
                responseCooldownKey = "keyword_intent_hello",
                targetState = null
            ),
            "come" to KeywordIntentDefinition(
                intentType = KeywordIntentType.COME,
                responseCategory = "CURIOUS",
                responseCooldownKey = "keyword_intent_come",
                targetState = BrainState.CURIOUS
            ),
            "look" to KeywordIntentDefinition(
                intentType = KeywordIntentType.LOOK,
                responseCategory = "CURIOUS",
                responseCooldownKey = "keyword_intent_look",
                targetState = BrainState.CURIOUS
            )
        )
    }
}
