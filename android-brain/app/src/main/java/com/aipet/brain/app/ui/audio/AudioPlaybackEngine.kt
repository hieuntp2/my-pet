package com.aipet.brain.app.ui.audio

import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.app.ui.audio.model.AudioClipMetadata
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponsePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioPlaybackEngine(
    private val context: Context,
    private val eventBus: EventBus? = null,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val fallbackClipDurationMs: Long = DEFAULT_FALLBACK_CLIP_DURATION_MS
) {
    fun playRandomClip(category: AudioCategory): Boolean {
        return playRandomClipWithDetails(category).started
    }

    fun playRandomClipWithDetails(category: AudioCategory): AudioPlaybackResult {
        Log.d(TAG, "Playback requested. category=${category.label}")
        val clipMetadata = AudioAssetRegistry.getRandomClipMetadata(category)
        if (clipMetadata == null) {
            return skipPlayback(
                category = category,
                clipMetadata = null,
                skipReason = AudioPlaybackSkipReason.CATEGORY_EMPTY,
                reasonDetail = "no manifest clip for category"
            )
        }

        val clipResId = clipMetadata.resourceId
        val clipResourceName = resolveRawResourceName(clipResId)
        synchronized(lock) {
            if (playbackDebugState.value.readinessState != AudioPlaybackReadinessState.READY) {
                return skipPlaybackLocked(
                    category = category,
                    clipMetadata = clipMetadata,
                    clipResourceName = clipResourceName,
                    skipReason = AudioPlaybackSkipReason.NOT_READY,
                    reasonDetail = "soundpool is preloading"
                )
            }

            val nowMs = SystemClock.elapsedRealtime()
            val remainingPlaybackMs = activePlaybackUntilElapsedRealtimeMs - nowMs
            if (remainingPlaybackMs > 0L) {
                return skipPlaybackLocked(
                    category = category,
                    clipMetadata = clipMetadata,
                    clipResourceName = clipResourceName,
                    skipReason = AudioPlaybackSkipReason.OVERLAP_GUARD,
                    reasonDetail = "overlap guard active: remainingMs=$remainingPlaybackMs"
                )
            }

            val elapsedSinceLastPlaybackMs = nowMs - lastPlaybackStartedElapsedRealtimeMs
            if (elapsedSinceLastPlaybackMs in 0 until cooldownMs) {
                return skipPlaybackLocked(
                    category = category,
                    clipMetadata = clipMetadata,
                    clipResourceName = clipResourceName,
                    skipReason = AudioPlaybackSkipReason.COOLDOWN,
                    reasonDetail = "cooldown active: elapsedMs=$elapsedSinceLastPlaybackMs"
                )
            }

            val soundId = soundIdByClipResId[clipResId]
            if (soundId == null || soundId !in loadedSoundIds) {
                return skipPlaybackLocked(
                    category = category,
                    clipMetadata = clipMetadata,
                    clipResourceName = clipResourceName,
                    skipReason = AudioPlaybackSkipReason.NOT_READY,
                    reasonDetail = "manifest clip is not ready"
                )
            }

            val streamId = soundPool.play(
                soundId,
                DEFAULT_VOLUME,
                DEFAULT_VOLUME,
                STREAM_PRIORITY,
                NO_LOOP,
                PLAYBACK_RATE_NORMAL
            )
            if (streamId == 0) {
                return skipPlaybackLocked(
                    category = category,
                    clipMetadata = clipMetadata,
                    clipResourceName = clipResourceName,
                    skipReason = AudioPlaybackSkipReason.PLAYBACK_ERROR,
                    reasonDetail = "soundpool returned streamId=0"
                )
            }

            val clipDurationMs = clipDurationByResId[clipResId] ?: fallbackClipDurationMs
            lastPlaybackStartedElapsedRealtimeMs = nowMs
            activePlaybackUntilElapsedRealtimeMs = nowMs + clipDurationMs
            val startedAtMs = System.currentTimeMillis()
            updateStartedDebugStateLocked(
                category = category,
                clipMetadata = clipMetadata,
                timestampMs = startedAtMs
            )
            publishStartedEvent(
                category = category,
                clipMetadata = clipMetadata,
                durationMs = clipDurationMs,
                timestampMs = startedAtMs
            )
            scheduleCompletionEvent(
                category = category,
                clipMetadata = clipMetadata,
                durationMs = clipDurationMs
            )
            Log.d(
                TAG,
                "Playback started. category=${category.label}, clip=${clipMetadata.logicalClipName}, " +
                    "clipResId=$clipResId, clipResource=R.raw.$clipResourceName, soundId=$soundId, " +
                    "streamId=$streamId, durationMs=$clipDurationMs"
            )
            return AudioPlaybackResult(
                category = category,
                clipLogicalName = clipMetadata.logicalClipName,
                clipResId = clipResId,
                clipResourceName = clipResourceName,
                started = true,
                reason = PlaybackStatus.STARTED.name,
                skipReason = null
            )
        }
    }

    fun currentDebugState(): AudioPlaybackDebugState = playbackDebugState.value

    fun observeDebugState(): StateFlow<AudioPlaybackDebugState> = playbackDebugState

    fun release() {
        synchronized(lock) {
            completionJob?.cancel()
            completionJob = null
            soundPool.release()
            soundIdByClipResId.clear()
            loadedSoundIds.clear()
            clipDurationByResId.clear()
            activePlaybackUntilElapsedRealtimeMs = 0L
            lastPlaybackStartedElapsedRealtimeMs = 0L
            updateReadinessStateLocked()
            Log.d(TAG, "AudioPlaybackEngine released.")
        }
        eventScope.cancel()
    }

    private fun preloadKnownClips() {
        AudioCategory.entries.forEach { category ->
            AudioAssetRegistry.getClipMetadata(category).forEach { clipMetadata ->
                preloadClip(clipMetadata)
            }
        }
    }

    private fun preloadClip(clipMetadata: AudioClipMetadata) {
        try {
            val soundId = soundPool.load(context, clipMetadata.resourceId, STREAM_PRIORITY)
            synchronized(lock) {
                soundIdByClipResId[clipMetadata.resourceId] = soundId
                clipDurationByResId[clipMetadata.resourceId] = resolveClipDurationMs(clipMetadata.resourceId)
                clipMetadataBySoundId[soundId] = clipMetadata
            }
            Log.d(
                TAG,
                "Queued clip preload. category=${clipMetadata.category.label}, clip=${clipMetadata.logicalClipName}, " +
                    "clipResId=${clipMetadata.resourceId}, soundId=$soundId"
            )
        } catch (error: Throwable) {
            synchronized(lock) {
                failedClipCount += 1
                updateReadinessStateLocked()
            }
            Log.e(
                TAG,
                "Failed to queue clip preload. category=${clipMetadata.category.label}, " +
                    "clip=${clipMetadata.logicalClipName}, clipResId=${clipMetadata.resourceId}",
                error
            )
        }
    }

    private fun resolveClipDurationMs(clipResId: Int): Long {
        return try {
            context.resources.openRawResourceFd(clipResId).use { fileDescriptor ->
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(
                        fileDescriptor.fileDescriptor,
                        fileDescriptor.startOffset,
                        fileDescriptor.length
                    )
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                        ?.coerceAtLeast(MIN_CLIP_DURATION_MS)
                        ?: fallbackClipDurationMs
                } finally {
                    retriever.release()
                }
            }
        } catch (error: Throwable) {
            Log.w(
                TAG,
                "Could not resolve clip duration for clipResId=$clipResId. " +
                    "Using fallback=${fallbackClipDurationMs}ms.",
                error
            )
            fallbackClipDurationMs
        }
    }

    private fun resolveRawResourceName(clipResId: Int): String {
        return try {
            context.resources.getResourceEntryName(clipResId)
        } catch (_: Resources.NotFoundException) {
            "unresolved_$clipResId"
        }
    }

    private fun skipPlayback(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata?,
        skipReason: AudioPlaybackSkipReason,
        reasonDetail: String
    ): AudioPlaybackResult {
        val clipResourceName = clipMetadata?.resourceId?.let { resolveRawResourceName(it) }
        synchronized(lock) {
            return skipPlaybackLocked(
                category = category,
                clipMetadata = clipMetadata,
                clipResourceName = clipResourceName,
                skipReason = skipReason,
                reasonDetail = reasonDetail
            )
        }
    }

    private fun skipPlaybackLocked(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata?,
        clipResourceName: String?,
        skipReason: AudioPlaybackSkipReason,
        reasonDetail: String
    ): AudioPlaybackResult {
        val clipResId = clipMetadata?.resourceId
        val durationMs = clipResId?.let { clipDurationByResId[it] } ?: 0L
        val timestampMs = System.currentTimeMillis()
        updateSkippedDebugStateLocked(
            skipReason = skipReason,
            timestampMs = timestampMs
        )
        publishSkippedEvent(
            category = category,
            clipMetadata = clipMetadata,
            durationMs = durationMs,
            skipReason = skipReason,
            timestampMs = timestampMs
        )
        Log.d(
            TAG,
            "Playback skipped. category=${category.label}, clip=${clipMetadata?.logicalClipName ?: "-"}, " +
                "clipResId=${clipResId ?: -1}, clipResource=${clipResourceName?.let { "R.raw.$it" } ?: "-"}, " +
                "skipReason=${skipReason.name}, detail=$reasonDetail"
        )
        return AudioPlaybackResult(
            category = category,
            clipLogicalName = clipMetadata?.logicalClipName,
            clipResId = clipResId,
            clipResourceName = clipResourceName,
            started = false,
            reason = skipReason.name,
            skipReason = skipReason
        )
    }

    private fun publishStartedEvent(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata,
        durationMs: Long,
        timestampMs: Long
    ) {
        publishAudioResponseEvent(
            eventType = EventType.AUDIO_RESPONSE_STARTED,
            payload = AudioResponsePayload(
                category = category.label,
                clipId = clipMetadata.logicalClipName,
                durationMs = durationMs,
                priority = STREAM_PRIORITY,
                timestamp = timestampMs
            ),
            timestampMs = timestampMs,
            clipLabel = clipMetadata.logicalClipName,
            reason = null
        )
    }

    private fun publishCompletedEvent(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata,
        durationMs: Long,
        timestampMs: Long
    ) {
        publishAudioResponseEvent(
            eventType = EventType.AUDIO_RESPONSE_COMPLETED,
            payload = AudioResponsePayload(
                category = category.label,
                clipId = clipMetadata.logicalClipName,
                durationMs = durationMs,
                priority = STREAM_PRIORITY,
                timestamp = timestampMs
            ),
            timestampMs = timestampMs,
            clipLabel = clipMetadata.logicalClipName,
            reason = null
        )
    }

    private fun publishSkippedEvent(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata?,
        durationMs: Long,
        skipReason: AudioPlaybackSkipReason,
        timestampMs: Long
    ) {
        publishAudioResponseEvent(
            eventType = EventType.AUDIO_RESPONSE_SKIPPED,
            payload = AudioResponsePayload(
                category = category.label,
                clipId = clipMetadata?.logicalClipName,
                durationMs = durationMs,
                priority = STREAM_PRIORITY,
                timestamp = timestampMs,
                reason = skipReason.name
            ),
            timestampMs = timestampMs,
            clipLabel = clipMetadata?.logicalClipName,
            reason = skipReason.name
        )
    }

    private fun publishAudioResponseEvent(
        eventType: EventType,
        payload: AudioResponsePayload,
        timestampMs: Long,
        clipLabel: String?,
        reason: String?
    ) {
        val bus = eventBus ?: run {
            Log.d(
                TAG,
                "Skipped ${eventType.name} publish: EventBus unavailable. " +
                    "category=${payload.category}, clip=${clipLabel ?: "-"}"
            )
            return
        }

        eventScope.launch {
            try {
                bus.publish(
                    EventEnvelope.create(
                        type = eventType,
                        timestampMs = timestampMs,
                        payloadJson = payload.toJson()
                    )
                )
                Log.d(
                    TAG,
                    "Published ${eventType.name}. category=${payload.category}, " +
                        "clip=${clipLabel ?: "-"}, reason=${reason ?: "-"}, durationMs=${payload.durationMs}"
                )
            } catch (error: Throwable) {
                Log.e(
                    TAG,
                    "Failed to publish ${eventType.name}. category=${payload.category}, " +
                        "clip=${clipLabel ?: "-"}",
                    error
                )
            }
        }
    }

    private fun scheduleCompletionEvent(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata,
        durationMs: Long
    ) {
        completionJob?.cancel()
        completionJob = eventScope.launch {
            delay(durationMs)
            publishCompletedEvent(
                category = category,
                clipMetadata = clipMetadata,
                durationMs = durationMs,
                timestampMs = System.currentTimeMillis()
            )
        }
    }

    private fun updateStartedDebugStateLocked(
        category: AudioCategory,
        clipMetadata: AudioClipMetadata,
        timestampMs: Long
    ) {
        playbackDebugState.value = playbackDebugState.value.copy(
            lastPlayedCategory = category.label,
            lastPlayedClipName = clipMetadata.logicalClipName,
            lastPlayedAtMs = timestampMs
        )
    }

    private fun updateSkippedDebugStateLocked(
        skipReason: AudioPlaybackSkipReason,
        timestampMs: Long
    ) {
        playbackDebugState.value = playbackDebugState.value.copy(
            lastSkippedReason = skipReason,
            lastSkippedAtMs = timestampMs
        )
    }

    private fun updateReadinessStateLocked() {
        val readinessState = when {
            totalManifestClipCount <= 0 -> AudioPlaybackReadinessState.NOT_READY
            failedClipCount > 0 -> AudioPlaybackReadinessState.NOT_READY
            loadedSoundIds.size >= totalManifestClipCount -> AudioPlaybackReadinessState.READY
            else -> AudioPlaybackReadinessState.PRELOADING
        }
        playbackDebugState.value = playbackDebugState.value.copy(
            readinessState = readinessState,
            loadedClipCount = loadedSoundIds.size,
            totalClipCount = totalManifestClipCount,
            failedClipCount = failedClipCount
        )
    }

    private val lock = Any()
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()
    private val soundIdByClipResId: MutableMap<Int, Int> = mutableMapOf()
    private val clipMetadataBySoundId: MutableMap<Int, AudioClipMetadata> = mutableMapOf()
    private val loadedSoundIds: MutableSet<Int> = mutableSetOf()
    private val clipDurationByResId: MutableMap<Int, Long> = mutableMapOf()
    private val totalManifestClipCount: Int = AudioAssetRegistry.allClipMetadata().size
    private val playbackDebugState = MutableStateFlow(
        AudioPlaybackDebugState(
            readinessState = AudioPlaybackReadinessState.NOT_READY,
            loadedClipCount = 0,
            totalClipCount = totalManifestClipCount,
            failedClipCount = 0
        )
    )
    private var failedClipCount: Int = 0
    private var activePlaybackUntilElapsedRealtimeMs: Long = 0L
    private var lastPlaybackStartedElapsedRealtimeMs: Long = 0L
    private var completionJob: Job? = null

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            synchronized(lock) {
                val clipMetadata = clipMetadataBySoundId[soundId]
                if (status == 0) {
                    loadedSoundIds.add(soundId)
                    Log.d(
                        TAG,
                        "Clip preloaded. soundId=$soundId, clip=${clipMetadata?.logicalClipName ?: "-"}"
                    )
                } else {
                    failedClipCount += 1
                    Log.e(
                        TAG,
                        "Clip preload failed. soundId=$soundId, status=$status, " +
                            "clip=${clipMetadata?.logicalClipName ?: "-"}"
                    )
                }
                updateReadinessStateLocked()
            }
        }
        synchronized(lock) {
            updateReadinessStateLocked()
        }
        preloadKnownClips()
    }

    companion object {
        private const val TAG = "AudioPlaybackEngine"
        private const val MAX_STREAMS = 1
        private const val STREAM_PRIORITY = 1
        private const val NO_LOOP = 0
        private const val DEFAULT_VOLUME = 1.0f
        private const val PLAYBACK_RATE_NORMAL = 1.0f
        private const val DEFAULT_COOLDOWN_MS = 300L
        private const val DEFAULT_FALLBACK_CLIP_DURATION_MS = 800L
        private const val MIN_CLIP_DURATION_MS = 100L
    }
}

enum class AudioPlaybackReadinessState {
    NOT_READY,
    PRELOADING,
    READY
}

enum class AudioPlaybackSkipReason {
    CATEGORY_EMPTY,
    NOT_READY,
    COOLDOWN,
    OVERLAP_GUARD,
    PLAYBACK_ERROR
}

data class AudioPlaybackDebugState(
    val readinessState: AudioPlaybackReadinessState,
    val loadedClipCount: Int,
    val totalClipCount: Int,
    val failedClipCount: Int,
    val lastPlayedCategory: String? = null,
    val lastPlayedClipName: String? = null,
    val lastPlayedAtMs: Long? = null,
    val lastSkippedReason: AudioPlaybackSkipReason? = null,
    val lastSkippedAtMs: Long? = null
)

data class AudioPlaybackResult(
    val category: AudioCategory,
    val clipLogicalName: String?,
    val clipResId: Int?,
    val clipResourceName: String?,
    val started: Boolean,
    val reason: String,
    val skipReason: AudioPlaybackSkipReason?
)

private enum class PlaybackStatus {
    STARTED
}
