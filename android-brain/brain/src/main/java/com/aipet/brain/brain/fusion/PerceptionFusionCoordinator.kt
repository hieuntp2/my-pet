package com.aipet.brain.brain.fusion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory implementation of [PerceptionFusionRepository].
 * Updated by [PerceptionFusionCoordinator].
 */
class InMemoryPerceptionFusionRepository : PerceptionFusionRepository {
    private val _snapshot = MutableStateFlow(PerceptionFusionSnapshot.DEFAULT)

    override fun observeFusionSnapshot(): StateFlow<PerceptionFusionSnapshot> =
        _snapshot.asStateFlow()

    override fun getCurrentFusionSnapshot(): PerceptionFusionSnapshot =
        _snapshot.value

    fun update(snapshot: PerceptionFusionSnapshot) {
        _snapshot.value = snapshot
    }
}

/**
 * Coordinates all perception interpreters and merges them into a unified [PerceptionFusionSnapshot].
 *
 * This is the single entry point for feeding raw signals into the fusion layer.
 * External callers (app module, audio rules, camera pipeline) call the `on*` methods.
 */
class PerceptionFusionCoordinator(
    val repository: InMemoryPerceptionFusionRepository,
    private val presenceInterpreter: PresenceInterpreter,
    private val audioInterpreter: AudioInterpreter,
    private val voiceInterpreter: VoiceInterpreter,
    private val touchInterpreter: TouchInterpreter,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    // ─── Camera / Presence signals ───────────────────────────────────────────

    suspend fun onCameraFrame(
        faceCount: Int,
        recognizedPersonId: String?,
        faceConfidence: Float
    ) {
        val now = nowProvider()
        val presence = presenceInterpreter.processFrame(faceCount, recognizedPersonId, faceConfidence, now)
        updateSnapshot(now) { it.copy(presence = presence) }
    }

    suspend fun onPersonRecognized(personId: String) {
        presenceInterpreter.setRecognizedPerson(personId, nowProvider())
    }

    // ─── Audio signals ────────────────────────────────────────────────────────

    suspend fun onAudioEnergyUpdate(ambientLevel: Float, peakLevel: Float) {
        val now = nowProvider()
        val audio = audioInterpreter.processAudioEnergy(ambientLevel, peakLevel, now)
        updateSnapshot(now) { it.copy(audioContext = audio) }
    }

    suspend fun onSelfPlaybackStarted() {
        audioInterpreter.setSelfPlaybackActive(true, nowProvider())
    }

    suspend fun onSelfPlaybackStopped() {
        audioInterpreter.setSelfPlaybackActive(false, nowProvider())
    }

    // ─── Voice signals ────────────────────────────────────────────────────────

    suspend fun onVoiceActivityStarted() {
        val now = nowProvider()
        voiceInterpreter.processVoiceActivityStarted(now)
        updateSnapshot(now) { it.copy(voiceContext = voiceInterpreter.buildCurrentContext(now)) }
    }

    suspend fun onVoiceActivityEnded() {
        val now = nowProvider()
        voiceInterpreter.processVoiceActivityEnded(now)
        updateSnapshot(now) { it.copy(voiceContext = voiceInterpreter.buildCurrentContext(now)) }
    }

    suspend fun onCommandParsed(
        rawText: String,
        commandType: VoiceCommandType,
        confidence: Float,
        addressedToPet: Boolean
    ) {
        val now = nowProvider()
        val voice = voiceInterpreter.processCommand(rawText, commandType, confidence, addressedToPet, now)
        updateSnapshot(now) { snap ->
            snap.copy(
                voiceContext = voice,
                recentPerceptionSummary = snap.recentPerceptionSummary.copy(
                    voiceCommandAtMs = now
                )
            )
        }
    }

    // ─── Touch signals ────────────────────────────────────────────────────────

    suspend fun onTap() {
        val now = nowProvider()
        val touch = touchInterpreter.processTap(now)
        updateSnapshot(now) { snap ->
            snap.copy(
                touchContext = touch,
                recentPerceptionSummary = snap.recentPerceptionSummary.copy(tapAtMs = now)
            )
        }
    }

    suspend fun onLongPress() {
        val now = nowProvider()
        val touch = touchInterpreter.processLongPress(now)
        updateSnapshot(now) { snap ->
            snap.copy(
                touchContext = touch,
                recentPerceptionSummary = snap.recentPerceptionSummary.copy(longPressEndedAtMs = now)
            )
        }
    }

    // ─── Environment computation ──────────────────────────────────────────────

    private suspend fun updateSnapshot(nowMs: Long, transform: (PerceptionFusionSnapshot) -> PerceptionFusionSnapshot) {
        mutex.withLock {
            val current = repository.getCurrentFusionSnapshot()
            val updated = transform(current)
            val withEnvironment = updated.copy(
                environmentContext = computeEnvironment(updated),
                socialContext = computeSocialContext(updated),
                attentionContext = computeAttentionContext(updated),
                updatedAtMs = nowMs
            )
            repository.update(withEnvironment)
        }
    }

    private fun computeEnvironment(snap: PerceptionFusionSnapshot): EnvironmentContext {
        val visualQuiet = !snap.presence.userPresent && snap.presence.faceCount == 0
        val audioQuiet = snap.audioContext.ambientLevel < 0.1f && !snap.audioContext.loudEventActive
        val pressure = (
            (if (snap.presence.userPresent) 0.4f else 0f) +
            (if (snap.voiceContext.voiceActivity) 0.3f else 0f) +
            (if (snap.touchContext.recentTap || snap.touchContext.recentLongPress) 0.3f else 0f)
        ).coerceIn(0f, 1f)
        return EnvironmentContext(visualQuiet = visualQuiet, audioQuiet = audioQuiet, interactionPressure = pressure)
    }

    private fun computeSocialContext(snap: PerceptionFusionSnapshot): SocialContext {
        val userPresent = snap.presence.userPresent
        val stableMs = snap.presence.stablePresenceMs
        val eyeContact = if (userPresent && stableMs > 1_000L) 0.6f else 0f
        val warmth = snap.touchContext.affectionLikelihood * 0.5f +
            (if (snap.voiceContext.commandAddressedToPet) 0.3f else 0f)
        return SocialContext(
            userWatchingPet = userPresent && eyeContact > 0.4f,
            eyeContactLikelihood = eyeContact,
            interactionAvailability = if (userPresent) 0.7f else 0f,
            socialWarmthSignal = warmth.coerceIn(0f, 1f)
        )
    }

    private fun computeAttentionContext(snap: PerceptionFusionSnapshot): AttentionContext {
        val urgency = when {
            snap.audioContext.loudEventActive -> 0.9f
            snap.presence.entryEventRecently -> 0.7f
            snap.voiceContext.voiceActivity -> 0.5f
            snap.attentionContext.noveltySignal > 0f -> snap.attentionContext.noveltySignal
            else -> 0f
        }
        val novelty = when {
            snap.presence.entryEventRecently -> 0.8f
            snap.audioContext.loudsoundRecently() -> 0.6f
            else -> 0f
        }
        return AttentionContext(
            focusDirection = FocusDirection.CENTER,
            noveltySignal = novelty,
            orientingUrgency = urgency
        )
    }
}

private fun AudioContext.loudsoundRecently(): Boolean =
    loudEventActive || loudSoundStartMs > 0L
