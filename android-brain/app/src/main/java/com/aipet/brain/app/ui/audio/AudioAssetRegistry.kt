package com.aipet.brain.app.ui.audio

import com.aipet.brain.app.R
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.app.ui.audio.model.AudioClipMetadata
import kotlin.random.Random

/**
 * Runtime source of truth for pet response clips.
 *
 * The canonical runtime assets are flattened `res/raw` resources referenced via `R.raw.*`
 * (for example `R.raw.greeting_1`). Nested duplicate folders under `res/raw/<category>/` are not
 * consumed by this registry.
 */
object AudioAssetRegistry {
    fun getClipMetadata(category: AudioCategory): List<AudioClipMetadata> {
        return clipManifestByCategory[category].orEmpty()
    }

    fun getRandomClipMetadata(
        category: AudioCategory,
        random: Random = Random.Default
    ): AudioClipMetadata? {
        val clips = getClipMetadata(category)
        if (clips.isEmpty()) {
            return null
        }
        return clips[random.nextInt(clips.size)]
    }

    fun allClipMetadata(): List<AudioClipMetadata> {
        return clipManifestByCategory.values.flatten()
    }

    fun categoryClipCounts(): Map<AudioCategory, Int> {
        return clipManifestByCategory.mapValues { (_, clips) -> clips.size }
    }

    fun getClips(category: AudioCategory): List<Int> {
        return getClipMetadata(category).map { metadata -> metadata.resourceId }
    }

    fun hasClips(category: AudioCategory): Boolean {
        return getClipMetadata(category).isNotEmpty()
    }

    fun getRandomClip(
        category: AudioCategory,
        random: Random = Random.Default
    ): Int? {
        return getRandomClipMetadata(category, random)?.resourceId
    }

    private val clipManifestByCategory: Map<AudioCategory, List<AudioClipMetadata>> = mapOf(
        AudioCategory.ACKNOWLEDGMENT to listOf(
            clip(AudioCategory.ACKNOWLEDGMENT, R.raw.acknowledgment_1, "acknowledgment_1"),
            clip(AudioCategory.ACKNOWLEDGMENT, R.raw.acknowledgment_2, "acknowledgment_2"),
            clip(AudioCategory.ACKNOWLEDGMENT, R.raw.acknowledgment_3, "acknowledgment_3"),
            clip(AudioCategory.ACKNOWLEDGMENT, R.raw.acknowledgment_4, "acknowledgment_4")
        ),
        AudioCategory.CURIOUS to listOf(
            clip(AudioCategory.CURIOUS, R.raw.curious_1, "curious_1"),
            clip(AudioCategory.CURIOUS, R.raw.curious_2, "curious_2"),
            clip(AudioCategory.CURIOUS, R.raw.curious_3, "curious_3"),
            clip(AudioCategory.CURIOUS, R.raw.curious_4, "curious_4")
        ),
        AudioCategory.GREETING to listOf(
            clip(AudioCategory.GREETING, R.raw.greeting_1, "greeting_1"),
            clip(AudioCategory.GREETING, R.raw.greeting_2, "greeting_2"),
            clip(AudioCategory.GREETING, R.raw.greeting_3, "greeting_3"),
            clip(AudioCategory.GREETING, R.raw.greeting_4, "greeting_4")
        ),
        AudioCategory.HAPPY to listOf(
            clip(AudioCategory.HAPPY, R.raw.happy_1, "happy_1"),
            clip(AudioCategory.HAPPY, R.raw.happy_2, "happy_2"),
            clip(AudioCategory.HAPPY, R.raw.happy_3, "happy_3"),
            clip(AudioCategory.HAPPY, R.raw.happy_4, "happy_4")
        ),
        AudioCategory.SLEEPY to listOf(
            clip(AudioCategory.SLEEPY, R.raw.sleepy_1, "sleepy_1"),
            clip(AudioCategory.SLEEPY, R.raw.sleepy_2, "sleepy_2"),
            clip(AudioCategory.SLEEPY, R.raw.sleepy_3, "sleepy_3"),
            clip(AudioCategory.SLEEPY, R.raw.sleepy_4, "sleepy_4")
        ),
        AudioCategory.SURPRISED to listOf(
            clip(AudioCategory.SURPRISED, R.raw.surprised_1, "surprised_1"),
            clip(AudioCategory.SURPRISED, R.raw.surprised_2, "surprised_2")
        ),
        AudioCategory.WARNING_NO to listOf(
            clip(AudioCategory.WARNING_NO, R.raw.warning_no_1, "warning_no_1"),
            clip(AudioCategory.WARNING_NO, R.raw.warning_no_2, "warning_no_2")
        )
    )

    private fun clip(
        category: AudioCategory,
        resourceId: Int,
        logicalClipName: String,
        durationMs: Long? = null
    ): AudioClipMetadata {
        return AudioClipMetadata(
            category = category,
            resourceId = resourceId,
            logicalClipName = logicalClipName,
            durationMs = durationMs
        )
    }
}
