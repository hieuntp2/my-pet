package com.aipet.brain.app.ui.audio

import com.aipet.brain.app.R
import com.aipet.brain.app.ui.audio.model.AudioCategory
import kotlin.random.Random

object AudioAssetRegistry {
    fun getClips(category: AudioCategory): List<Int> {
        return clipsByCategory[category].orEmpty()
    }

    fun hasClips(category: AudioCategory): Boolean {
        return getClips(category).isNotEmpty()
    }

    fun getRandomClip(
        category: AudioCategory,
        random: Random = Random.Default
    ): Int? {
        val clips = getClips(category)
        if (clips.isEmpty()) {
            return null
        }
        return clips[random.nextInt(clips.size)]
    }

    private val clipsByCategory: Map<AudioCategory, List<Int>> = mapOf(
        AudioCategory.ACKNOWLEDGMENT to listOf(
            R.raw.acknowledgment_1,
            R.raw.acknowledgment_2,
            R.raw.acknowledgment_3,
            R.raw.acknowledgment_4
        ),
        AudioCategory.CURIOUS to listOf(
            R.raw.curious_1,
            R.raw.curious_2,
            R.raw.curious_3,
            R.raw.curious_4
        ),
        AudioCategory.GREETING to listOf(
            R.raw.greeting_1,
            R.raw.greeting_2,
            R.raw.greeting_3,
            R.raw.greeting_4
        ),
        AudioCategory.HAPPY to listOf(
            R.raw.happy_1,
            R.raw.happy_2,
            R.raw.happy_3,
            R.raw.happy_4
        ),
        AudioCategory.SLEEPY to listOf(
            R.raw.sleepy_1,
            R.raw.sleepy_2,
            R.raw.sleepy_3,
            R.raw.sleepy_4
        ),
        AudioCategory.SURPRISED to listOf(
            R.raw.surprised_1,
            R.raw.surprised_2
        ),
        AudioCategory.WARNING_NO to listOf(
            R.raw.warning_no_1,
            R.raw.warning_no_2
        )
    )
}
