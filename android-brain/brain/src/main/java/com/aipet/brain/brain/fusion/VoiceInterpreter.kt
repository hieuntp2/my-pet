package com.aipet.brain.brain.fusion

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interprets VAD + ASR signals into a semantic [VoiceContext].
 *
 * Voice commands are treated as social signals, not just triggers.
 * Confidence and freshness are tracked for downstream behavior decisions.
 */
class VoiceInterpreter(
    private val voiceHangoverMs: Long = VOICE_HANGOVER_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    @Volatile private var voiceActive: Boolean = false
    @Volatile private var lastCommandType: VoiceCommandType? = null
    @Volatile private var lastCommandConfidence: Float = 0f
    @Volatile private var lastCommandAddressedToPet: Boolean = false
    @Volatile private var lastRawText: String? = null
    @Volatile private var recentSpeechMs: Long = 0L

    suspend fun processVoiceActivityStarted(nowMs: Long = nowProvider()) {
        mutex.withLock {
            voiceActive = true
            recentSpeechMs = nowMs
        }
    }

    suspend fun processVoiceActivityEnded(nowMs: Long = nowProvider()) {
        mutex.withLock {
            voiceActive = false
        }
    }

    suspend fun processCommand(
        rawText: String,
        commandType: VoiceCommandType,
        confidence: Float,
        addressedToPet: Boolean,
        nowMs: Long = nowProvider()
    ): VoiceContext {
        return mutex.withLock {
            lastCommandType = commandType
            lastCommandConfidence = confidence
            lastCommandAddressedToPet = addressedToPet
            lastRawText = rawText
            recentSpeechMs = nowMs

            buildContext(nowMs)
        }
    }

    fun buildCurrentContext(nowMs: Long = nowProvider()): VoiceContext {
        val sinceLastSpeech = if (recentSpeechMs > 0L) nowMs - recentSpeechMs else Long.MAX_VALUE
        val commandStillFresh = sinceLastSpeech < voiceHangoverMs

        return VoiceContext(
            voiceActivity = voiceActive,
            commandType = if (commandStillFresh) lastCommandType else null,
            commandConfidence = if (commandStillFresh) lastCommandConfidence else 0f,
            commandAddressedToPet = commandStillFresh && lastCommandAddressedToPet,
            lastRawText = if (commandStillFresh) lastRawText else null,
            recentSpeechMs = if (recentSpeechMs > 0L) nowMs - recentSpeechMs else 0L,
            updatedAtMs = nowMs
        )
    }

    private fun buildContext(nowMs: Long): VoiceContext = buildCurrentContext(nowMs)

    companion object {
        /** How long after last speech the context remains "fresh." */
        private const val VOICE_HANGOVER_MS = 5_000L
    }
}
