package com.aipet.brain.app.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.app.ui.audio.model.AudioCategory

class AudioPlaybackEngine(
    private val context: Context,
    private val cooldownMs: Long = DEFAULT_COOLDOWN_MS,
    private val fallbackClipDurationMs: Long = DEFAULT_FALLBACK_CLIP_DURATION_MS
) {
    fun playRandomClip(category: AudioCategory): Boolean {
        val clipResId = AudioAssetRegistry.getRandomClip(category)
        if (clipResId == null) {
            Log.e(TAG, "Playback skipped: no clip registered for category=${category.label}")
            return false
        }

        synchronized(lock) {
            val nowMs = SystemClock.elapsedRealtime()
            val remainingPlaybackMs = activePlaybackUntilElapsedRealtimeMs - nowMs
            if (remainingPlaybackMs > 0L) {
                Log.d(
                    TAG,
                    "Playback skipped: overlap protection active. " +
                        "category=${category.label}, clipResId=$clipResId, remainingMs=$remainingPlaybackMs"
                )
                return false
            }

            val elapsedSinceLastPlaybackMs = nowMs - lastPlaybackStartedElapsedRealtimeMs
            if (elapsedSinceLastPlaybackMs in 0 until cooldownMs) {
                Log.d(
                    TAG,
                    "Playback skipped: cooldown active. " +
                        "category=${category.label}, clipResId=$clipResId, cooldownMs=$cooldownMs, " +
                        "elapsedMs=$elapsedSinceLastPlaybackMs"
                )
                return false
            }

            val soundId = soundIdByClipResId[clipResId]
            if (soundId == null) {
                Log.e(
                    TAG,
                    "Playback skipped: clip is not preloaded. " +
                        "category=${category.label}, clipResId=$clipResId"
                )
                return false
            }
            if (soundId !in loadedSoundIds) {
                Log.d(
                    TAG,
                    "Playback skipped: clip still loading. " +
                        "category=${category.label}, clipResId=$clipResId, soundId=$soundId"
                )
                return false
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
                Log.e(
                    TAG,
                    "Playback skipped: SoundPool returned streamId=0. " +
                        "category=${category.label}, clipResId=$clipResId, soundId=$soundId"
                )
                return false
            }

            val clipDurationMs = clipDurationByResId[clipResId] ?: fallbackClipDurationMs
            lastPlaybackStartedElapsedRealtimeMs = nowMs
            activePlaybackUntilElapsedRealtimeMs = nowMs + clipDurationMs
            Log.d(
                TAG,
                "Playback started. category=${category.label}, clipResId=$clipResId, " +
                    "soundId=$soundId, streamId=$streamId, durationMs=$clipDurationMs"
            )
            return true
        }
    }

    fun release() {
        synchronized(lock) {
            soundPool.release()
            soundIdByClipResId.clear()
            loadedSoundIds.clear()
            clipDurationByResId.clear()
            activePlaybackUntilElapsedRealtimeMs = 0L
            lastPlaybackStartedElapsedRealtimeMs = 0L
            Log.d(TAG, "AudioPlaybackEngine released.")
        }
    }

    private fun preloadKnownClips() {
        AudioCategory.entries.forEach { category ->
            AudioAssetRegistry.getClips(category).forEach { clipResId ->
                preloadClip(category = category, clipResId = clipResId)
            }
        }
    }

    private fun preloadClip(
        category: AudioCategory,
        clipResId: Int
    ) {
        try {
            val soundId = soundPool.load(context, clipResId, STREAM_PRIORITY)
            synchronized(lock) {
                soundIdByClipResId[clipResId] = soundId
                clipDurationByResId[clipResId] = resolveClipDurationMs(clipResId)
            }
            Log.d(
                TAG,
                "Queued clip preload. category=${category.label}, clipResId=$clipResId, soundId=$soundId"
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to queue clip preload. category=${category.label}, clipResId=$clipResId",
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

    private val lock = Any()
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
    private val loadedSoundIds: MutableSet<Int> = mutableSetOf()
    private val clipDurationByResId: MutableMap<Int, Long> = mutableMapOf()
    private var activePlaybackUntilElapsedRealtimeMs: Long = 0L
    private var lastPlaybackStartedElapsedRealtimeMs: Long = 0L

    init {
        soundPool.setOnLoadCompleteListener { _, soundId, status ->
            synchronized(lock) {
                if (status == 0) {
                    loadedSoundIds.add(soundId)
                    Log.d(TAG, "Clip preloaded. soundId=$soundId")
                } else {
                    Log.e(TAG, "Clip preload failed. soundId=$soundId, status=$status")
                }
            }
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
