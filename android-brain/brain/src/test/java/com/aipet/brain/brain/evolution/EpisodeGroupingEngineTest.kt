package com.aipet.brain.brain.evolution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeGroupingEngineTest {

    @Test
    fun `closeCurrentEpisodeIfInactive finalizes only after inactivity timeout`() {
        var now = 1_000_000L
        val engine = EpisodeGroupingEngine(
            summarizer = EpisodeSummarizer(),
            importanceScorer = ImportanceScorer(),
            nowProvider = { now }
        )

        engine.onSessionStarted(reunionType = "ROUTINE_RETURN")
        engine.recordEvent(
            emotionName = "HAPPY",
            moodName = "HAPPY",
            isInteraction = true,
            interactionType = "PLAY"
        )

        now += EpisodeBoundaryResolver.INACTIVITY_TIMEOUT_MS - 1
        assertNull(engine.closeCurrentEpisodeIfInactive())

        now += EpisodeBoundaryResolver.MIN_EPISODE_DURATION_MS + 2
        val episode = engine.closeCurrentEpisodeIfInactive()

        assertNotNull(episode)
        assertEquals("ROUTINE_RETURN", episode?.reunionType)
        assertEquals(1, episode?.eventCount)
        assertEquals(1, episode?.interactionCount)
        assertTrue(!engine.hasOpenEpisode())
    }

    @Test
    fun `closeCurrentEpisode skips very short sessions`() {
        var now = 2_000_000L
        val engine = EpisodeGroupingEngine(
            summarizer = EpisodeSummarizer(),
            importanceScorer = ImportanceScorer(),
            nowProvider = { now }
        )

        engine.onSessionStarted(reunionType = "QUICK_RETURN")
        engine.recordEvent(
            emotionName = "IDLE",
            moodName = "NEUTRAL",
            isInteraction = false
        )

        now += EpisodeBoundaryResolver.MIN_EPISODE_DURATION_MS - 1
        assertNull(engine.closeCurrentEpisode())
        assertTrue(!engine.hasOpenEpisode())
    }
}
