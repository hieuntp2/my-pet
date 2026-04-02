package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import java.util.UUID

/**
 * Accumulates raw interaction signals into episode candidates and emits
 * finalized MemoryEpisode objects when a session boundary is reached.
 *
 * Thread-safety: all mutation must happen from a single coroutine context.
 */
class EpisodeGroupingEngine(
    private val summarizer: EpisodeSummarizer,
    private val importanceScorer: ImportanceScorer,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private var openCandidate: EpisodeCandidate? = null

    /** Call when the app opens to start or continue a session. */
    fun onSessionStarted(reunionType: String) {
        val now = nowProvider()
        val existing = openCandidate
        if (existing != null && EpisodeBoundaryResolver.shouldMerge(existing, now)) {
            // Still within the merge window — keep the open episode
            existing.reunionType = reunionType
            return
        }
        // Close the previous episode if one was open
        // (finalization happens via closeCurrentEpisode call from outside)
        openCandidate = EpisodeCandidate(
            sessionId = UUID.randomUUID().toString(),
            startTimeMs = now,
            lastActivityMs = now,
            reunionType = reunionType
        )
    }

    /** Record an event that occurred during the session. */
    fun recordEvent(emotionName: String, moodName: String, isInteraction: Boolean, interactionType: String? = null) {
        val candidate = openCandidate ?: return
        val now = nowProvider()
        candidate.lastActivityMs = now
        candidate.eventCount++
        candidate.emotionCounts[emotionName] = (candidate.emotionCounts[emotionName] ?: 0) + 1
        candidate.moodCounts[moodName] = (candidate.moodCounts[moodName] ?: 0) + 1
        if (isInteraction) {
            candidate.interactionCount++
            interactionType?.let { candidate.interactionTypes.add(it) }
        }
    }

    /** Record a care action delta (positive or negative). */
    fun recordCareChange(careScoreDelta: Int, bondDelta: Int) {
        val candidate = openCandidate ?: return
        candidate.careScoreDelta += careScoreDelta
        candidate.bondDelta += bondDelta
    }

    /** Mark that a neglect signal occurred in this session. */
    fun recordNeglectSignal() {
        openCandidate?.hadNeglectSignal = true
    }

    /** Tag user behavior based on dominant pattern. */
    fun tagUserBehavior(tag: String) {
        openCandidate?.userBehaviorTag = tag
    }

    /**
     * Finalize the current open episode.
     * Call on app background or inactivity timeout.
     * Returns the finalized episode, or null if not worth saving.
     */
    fun closeCurrentEpisode(): MemoryEpisode? {
        val candidate = openCandidate ?: return null
        val now = nowProvider()
        if (!EpisodeBoundaryResolver.isWorthSaving(candidate, now)) {
            openCandidate = null
            return null
        }
        openCandidate = null
        val durationMs = now - candidate.startTimeMs
        val interactionTypesJson = candidate.interactionTypes
            .distinct()
            .joinToString(separator = ",", prefix = "[", postfix = "]") { "\"$it\"" }
        val episode = MemoryEpisode(
            id = UUID.randomUUID().toString(),
            startTimeMs = candidate.startTimeMs,
            endTimeMs = now,
            durationMs = durationMs,
            eventCount = candidate.eventCount,
            interactionCount = candidate.interactionCount,
            interactionTypesJson = interactionTypesJson,
            dominantPetEmotion = candidate.dominantEmotion(),
            dominantPetMood = candidate.dominantMood(),
            careScoreDelta = candidate.careScoreDelta,
            bondDelta = candidate.bondDelta,
            neglectSignal = candidate.hadNeglectSignal,
            reunionType = candidate.reunionType,
            userBehaviorTag = candidate.userBehaviorTag,
            importanceScore = importanceScorer.score(candidate, durationMs),
            summaryText = summarizer.summarize(candidate, durationMs),
            createdAtMs = now
        )
        return episode
    }

    /**
     * Finalize the open episode only when the inactivity boundary is reached.
     * Returns null when there is no open episode or inactivity has not elapsed yet.
     */
    fun closeCurrentEpisodeIfInactive(): MemoryEpisode? {
        val candidate = openCandidate ?: return null
        val now = nowProvider()
        val inactiveMs = now - candidate.lastActivityMs
        if (inactiveMs < EpisodeBoundaryResolver.INACTIVITY_TIMEOUT_MS) {
            return null
        }
        return closeCurrentEpisode()
    }

    fun hasOpenEpisode(): Boolean = openCandidate != null
}
