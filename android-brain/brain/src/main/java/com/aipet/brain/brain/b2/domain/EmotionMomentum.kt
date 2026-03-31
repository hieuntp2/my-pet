package com.aipet.brain.brain.b2.domain

/**
 * Emotional carry-over model with three layers:
 * - Reaction affect: fast-changing (seconds), driven by current event
 * - Mood field: slow-moving (minutes/session), biases behavior selection
 * - The underlying traits are captured as trait bias elsewhere
 */
data class EmotionMomentum(
    // --- Reaction affect (seconds) ---
    val joy: Float = 0f,
    val comfort: Float = 0f,
    val curiosity: Float = 0f,
    val drowsiness: Float = 0f,
    val neediness: Float = 0f,
    val irritation: Float = 0f,
    val caution: Float = 0f,
    val startledLevel: Float = 0f,
    // --- Mood field (minutes/session) ---
    val moodPlayful: Float = 0f,
    val moodWithdrawn: Float = 0f,
    val moodWarm: Float = 0f,
    val moodDrowsy: Float = 0f,
    val moodNeedy: Float = 0f,
    val updatedAtMs: Long = 0L
) {
    /** Returns all field values clamped to [0, 1]. */
    fun clamped(): EmotionMomentum = copy(
        joy = joy.coerceIn(0f, 1f),
        comfort = comfort.coerceIn(0f, 1f),
        curiosity = curiosity.coerceIn(0f, 1f),
        drowsiness = drowsiness.coerceIn(0f, 1f),
        neediness = neediness.coerceIn(0f, 1f),
        irritation = irritation.coerceIn(0f, 1f),
        caution = caution.coerceIn(0f, 1f),
        startledLevel = startledLevel.coerceIn(0f, 1f),
        moodPlayful = moodPlayful.coerceIn(0f, 1f),
        moodWithdrawn = moodWithdrawn.coerceIn(0f, 1f),
        moodWarm = moodWarm.coerceIn(0f, 1f),
        moodDrowsy = moodDrowsy.coerceIn(0f, 1f),
        moodNeedy = moodNeedy.coerceIn(0f, 1f)
    )

    /** Summarize the dominant mood field for debug. */
    fun dominantMoodLabel(): String {
        val moods = mapOf(
            "playful" to moodPlayful,
            "withdrawn" to moodWithdrawn,
            "warm" to moodWarm,
            "drowsy" to moodDrowsy,
            "needy" to moodNeedy
        )
        val dominant = moods.maxByOrNull { it.value }
        return if (dominant != null && dominant.value > 0.2f) dominant.key else "neutral"
    }

    companion object {
        val DEFAULT = EmotionMomentum(updatedAtMs = 0L)
    }
}
