package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetMood

/**
 * An in-progress episode being accumulated during the current session.
 * It becomes a finalized MemoryEpisode when the session ends.
 */
data class EpisodeCandidate(
    val sessionId: String,
    val startTimeMs: Long,
    var lastActivityMs: Long,
    var eventCount: Int = 0,
    var interactionCount: Int = 0,
    val interactionTypes: MutableList<String> = mutableListOf(),
    val emotionCounts: MutableMap<String, Int> = mutableMapOf(),
    val moodCounts: MutableMap<String, Int> = mutableMapOf(),
    var careScoreDelta: Int = 0,
    var bondDelta: Int = 0,
    var hadNeglectSignal: Boolean = false,
    var reunionType: String = "NONE",
    var userBehaviorTag: String = "GENERAL"
) {
    fun dominantEmotion(): String =
        emotionCounts.maxByOrNull { it.value }?.key ?: PetEmotion.IDLE.name

    fun dominantMood(): String =
        moodCounts.maxByOrNull { it.value }?.key ?: PetMood.NEUTRAL.name
}
