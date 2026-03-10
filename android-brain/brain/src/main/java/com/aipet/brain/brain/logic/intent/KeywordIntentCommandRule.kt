package com.aipet.brain.brain.logic.intent

import android.util.Log
import com.aipet.brain.brain.events.BrainStateChangedEventPayload
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.logic.audio.AudioResponseRequestEmitter
import com.aipet.brain.brain.logic.audio.AudioResponseRequestInput
import com.aipet.brain.brain.logic.audio.AudioStimulusObserver
import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.KeywordStimulusKind
import com.aipet.brain.brain.state.BrainStateStore
import kotlinx.coroutines.flow.collect

class KeywordIntentCommandRule(
    private val audioStimulusObserver: AudioStimulusObserver,
    private val keywordIntentMapper: KeywordIntentMapper,
    private val audioResponseRequestEmitter: AudioResponseRequestEmitter,
    private val eventBus: EventBus,
    private val brainStateStore: BrainStateStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val intentCooldownMs: Long = DEFAULT_INTENT_COOLDOWN_MS
) {
    init {
        require(intentCooldownMs >= 0L) { "intentCooldownMs must be >= 0" }
    }

    suspend fun observeStimuliAndReact() {
        audioStimulusObserver.observeLatestStimulus().collect { stimulus ->
            val keywordStimulus = stimulus as? KeywordStimulus ?: return@collect
            if (keywordStimulus.kind != KeywordStimulusKind.KEYWORD) {
                return@collect
            }
            handleKeywordStimulus(keywordStimulus)
        }
    }

    private suspend fun handleKeywordStimulus(stimulus: KeywordStimulus) {
        Log.d(
            TAG,
            "Received keyword event. eventType=${stimulus.sourceEventType.name}, " +
                "keywordId=${stimulus.keywordId}, text=${stimulus.keywordText ?: "-"}, " +
                "confidence=${formatConfidence(stimulus.confidence)}, engine=${stimulus.engine}, " +
                "ts=${stimulus.timestampMs}"
        )

        if (stimulus.keywordId.isBlank()) {
            Log.w(TAG, "Ignored keyword event: keywordId is blank.")
            return
        }
        if (!stimulus.confidence.isFinite() || stimulus.confidence !in 0f..1f) {
            Log.w(
                TAG,
                "Ignored keyword event: invalid confidence=${stimulus.confidence}, " +
                    "keywordId=${stimulus.keywordId}"
            )
            return
        }

        val command = keywordIntentMapper.mapKeywordStimulus(stimulus)
        if (command == null) {
            Log.d(
                TAG,
                "Ignored unmapped keyword event. keywordId=${stimulus.keywordId}, " +
                    "eventType=${stimulus.sourceEventType.name}"
            )
            return
        }
        Log.d(
            TAG,
            "Mapped keyword intent. intent=${command.intent.intentType.name}, " +
                "keywordId=${command.intent.keywordId}, responseCategory=${command.responseCategory}"
        )

        val nowMs = nowProvider()
        val lastTriggeredAtMs = lastTriggeredAtByIntent[command.intent.intentType] ?: 0L
        if (lastTriggeredAtMs > 0L && (nowMs - lastTriggeredAtMs) in 0 until intentCooldownMs) {
            Log.d(
                TAG,
                "Ignored keyword intent due to cooldown. intent=${command.intent.intentType.name}, " +
                    "elapsedMs=${nowMs - lastTriggeredAtMs}, cooldownMs=$intentCooldownMs"
            )
            return
        }

        applyTargetStateIfNeeded(
            command = command,
            eventTimestampMs = stimulus.timestampMs.takeIf { it > 0L } ?: nowMs
        )

        val emitted = audioResponseRequestEmitter.emitFromStimulus(
            AudioResponseRequestInput(
                stimulus = stimulus,
                category = command.responseCategory,
                cooldownKey = command.responseCooldownKey
            )
        )
        if (emitted) {
            lastTriggeredAtByIntent[command.intent.intentType] = nowMs
            Log.d(
                TAG,
                "Emitted response request from keyword intent. intent=${command.intent.intentType.name}, " +
                    "category=${command.responseCategory}, keywordId=${command.intent.keywordId}"
            )
        } else {
            Log.w(
                TAG,
                "Failed to emit response request from keyword intent. " +
                    "intent=${command.intent.intentType.name}, keywordId=${command.intent.keywordId}"
            )
        }
    }

    private suspend fun applyTargetStateIfNeeded(
        command: KeywordCommand,
        eventTimestampMs: Long
    ) {
        val targetState = command.targetState ?: return
        val currentState = brainStateStore.currentSnapshot().currentState
        if (currentState == targetState) {
            Log.d(
                TAG,
                "Intent target state already active. state=${targetState.name}, " +
                    "intent=${command.intent.intentType.name}"
            )
            return
        }

        val changed = brainStateStore.setState(
            targetState = targetState,
            timestampMs = eventTimestampMs
        )
        if (!changed) {
            Log.d(
                TAG,
                "Intent target state transition skipped. state=${targetState.name}, " +
                    "intent=${command.intent.intentType.name}"
            )
            return
        }

        try {
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.BRAIN_STATE_CHANGED,
                    payloadJson = BrainStateChangedEventPayload(
                        fromState = currentState,
                        toState = targetState,
                        reason = "KEYWORD_INTENT_${command.intent.intentType.name}",
                        changedAtMs = eventTimestampMs
                    ).toJson(),
                    timestampMs = eventTimestampMs
                )
            )
            Log.d(
                TAG,
                "Applied keyword intent state transition. from=${currentState.name}, " +
                    "to=${targetState.name}, intent=${command.intent.intentType.name}"
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to publish ${EventType.BRAIN_STATE_CHANGED.name} for keyword intent " +
                    "${command.intent.intentType.name}.",
                error
            )
        }
    }

    private fun formatConfidence(confidence: Float): String {
        return String.format(java.util.Locale.US, "%.3f", confidence)
    }

    private val lastTriggeredAtByIntent = mutableMapOf<KeywordIntentType, Long>()

    companion object {
        private const val TAG = "KeywordIntentRule"
        private const val DEFAULT_INTENT_COOLDOWN_MS = 2_000L
    }
}
