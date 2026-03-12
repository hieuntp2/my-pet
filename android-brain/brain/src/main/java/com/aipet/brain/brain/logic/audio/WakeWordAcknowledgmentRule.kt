package com.aipet.brain.brain.logic.audio

import android.util.Log
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.brain.state.BrainStateStore
import java.util.Locale
import kotlinx.coroutines.flow.collect

class WakeWordAcknowledgmentRule(
    private val audioStimulusObserver: AudioStimulusObserver,
    private val audioResponseRequestEmitter: AudioResponseRequestEmitter,
    private val brainStateStore: BrainStateStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val minWakeWordConfidence: Float = DEFAULT_MIN_WAKE_WORD_CONFIDENCE,
    private val minKeywordConfidence: Float = DEFAULT_MIN_KEYWORD_CONFIDENCE,
    supportedKeywordIds: Set<String> = DEFAULT_SUPPORTED_KEYWORD_IDS,
    private val ruleCooldownMs: Long = DEFAULT_RULE_COOLDOWN_MS
) {
    init {
        require(minWakeWordConfidence in 0f..1f) { "minWakeWordConfidence must be in [0, 1]" }
        require(minKeywordConfidence in 0f..1f) { "minKeywordConfidence must be in [0, 1]" }
        require(ruleCooldownMs >= 0L) { "ruleCooldownMs must be >= 0" }
    }

    private val normalizedSupportedKeywordIds: Set<String> = supportedKeywordIds
        .map { it.trim().lowercase(Locale.US) }
        .filter { it.isNotEmpty() }
        .toSet()

    suspend fun observeStimuliAndReact() {
        audioStimulusObserver.observeLatestStimulus().collect { stimulus ->
            val keywordStimulus = stimulus as? KeywordStimulus ?: return@collect
            handleKeywordStimulus(keywordStimulus)
        }
    }

    private suspend fun handleKeywordStimulus(stimulus: KeywordStimulus) {
        Log.d(
            TAG,
            "Received keyword stimulus. kind=${stimulus.kind.name}, keywordId=${stimulus.keywordId}, " +
                "keywordText=${stimulus.keywordText ?: "-"}, confidence=${formatConfidence(stimulus.confidence)}, " +
                "engine=${stimulus.engine}, source=${stimulus.sourceEventType.name}, ts=${stimulus.timestampMs}"
        )

        if (!stimulus.confidence.isFinite() || stimulus.confidence !in 0f..1f) {
            Log.w(
                TAG,
                "Ignored keyword stimulus: invalid confidence=${stimulus.confidence}, keywordId=${stimulus.keywordId}"
            )
            return
        }

        val minConfidence = when (stimulus.kind) {
            KeywordStimulusKind.WAKE_WORD -> minWakeWordConfidence
            KeywordStimulusKind.KEYWORD -> minKeywordConfidence
        }
        if (stimulus.confidence < minConfidence) {
            Log.d(
                TAG,
                "Ignored weak keyword stimulus. kind=${stimulus.kind.name}, keywordId=${stimulus.keywordId}, " +
                    "confidence=${formatConfidence(stimulus.confidence)}, threshold=${formatConfidence(minConfidence)}"
            )
            return
        }

        if (stimulus.kind == KeywordStimulusKind.KEYWORD && !isSupportedKeyword(stimulus.keywordId)) {
            Log.d(
                TAG,
                "Ignored unsupported keyword stimulus. keywordId=${stimulus.keywordId}, " +
                    "supportedIds=$normalizedSupportedKeywordIds"
            )
            return
        }

        val nowMs = nowProvider()
        val elapsedSinceLastTriggerMs = nowMs - lastTriggeredAtMs
        if (lastTriggeredAtMs > 0L && elapsedSinceLastTriggerMs in 0 until ruleCooldownMs) {
            Log.d(
                TAG,
                "Ignored keyword stimulus due to internal cooldown. elapsedMs=$elapsedSinceLastTriggerMs " +
                    "cooldownMs=$ruleCooldownMs, keywordId=${stimulus.keywordId}"
            )
            return
        }

        val eventTimestampMs = if (stimulus.timestampMs > 0L) stimulus.timestampMs else nowMs
        applyAttentiveState(eventTimestampMs, stimulus)

        val emitted = audioResponseRequestEmitter.emitFromStimulus(
            AudioResponseRequestInput(
                stimulus = stimulus,
                category = ACKNOWLEDGMENT_CATEGORY,
                cooldownKey = RULE_COOLDOWN_KEY
            )
        )
        if (emitted) {
            lastTriggeredAtMs = nowMs
            Log.d(
                TAG,
                "Acknowledgment request emitted. category=$ACKNOWLEDGMENT_CATEGORY, " +
                    "keywordId=${stimulus.keywordId}, eventType=${stimulus.sourceEventType.name}"
            )
        } else {
            Log.w(
                TAG,
                "Acknowledgment request was not emitted. category=$ACKNOWLEDGMENT_CATEGORY, " +
                    "keywordId=${stimulus.keywordId}"
            )
        }
    }

    private suspend fun applyAttentiveState(
        eventTimestampMs: Long,
        stimulus: KeywordStimulus
    ) {
        val currentState = brainStateStore.currentSnapshot().currentState
        if (currentState == BrainState.CURIOUS) {
            Log.d(
                TAG,
                "Attentive state already active (CURIOUS). keywordId=${stimulus.keywordId}"
            )
            return
        }

        val changed = brainStateStore.setState(
            targetState = BrainState.CURIOUS,
            timestampMs = eventTimestampMs,
            reason = KEYWORD_ATTENTIVE_REASON
        )
        if (!changed) {
            Log.d(
                TAG,
                "Attentive state transition skipped (no state change). keywordId=${stimulus.keywordId}"
            )
            return
        }

        Log.d(
            TAG,
            "Applied attentive state. from=${currentState.name}, to=${BrainState.CURIOUS.name}, " +
                "keywordId=${stimulus.keywordId}, source=${stimulus.sourceEventType.name}"
        )
    }

    private fun isSupportedKeyword(keywordId: String): Boolean {
        val normalizedKeywordId = keywordId.trim().lowercase(Locale.US)
        return normalizedKeywordId.isNotEmpty() && normalizedKeywordId in normalizedSupportedKeywordIds
    }

    private fun formatConfidence(value: Float): String {
        return String.format(Locale.US, "%.3f", value)
    }

    private var lastTriggeredAtMs: Long = 0L

    companion object {
        private const val TAG = "WakeWordAckRule"
        private const val ACKNOWLEDGMENT_CATEGORY = "ACKNOWLEDGMENT"
        private const val RULE_COOLDOWN_KEY = "wake_word_acknowledgment"
        private const val KEYWORD_ATTENTIVE_REASON = "KEYWORD_ATTENTIVE_ACKNOWLEDGMENT"
        private const val DEFAULT_MIN_WAKE_WORD_CONFIDENCE = 0.03f
        private const val DEFAULT_MIN_KEYWORD_CONFIDENCE = 0.20f
        private val DEFAULT_SUPPORTED_KEYWORD_IDS = setOf("hey_pet")
        private const val DEFAULT_RULE_COOLDOWN_MS = 1_500L
    }
}
