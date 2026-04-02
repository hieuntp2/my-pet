package com.aipet.brain.app.behavior.experience

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetIntentionExecutorTest {

    @Test
    fun `global audio interval suppresses rapid cross-key playback`() {
        var nowMs = 10_000L
        val executor = PetIntentionExecutor(
            globalAudioMinIntervalMs = 1_200L,
            nowProvider = { nowMs }
        )

        val first = executor.execute(
            bundle(
                id = "plan_first",
                intention = PetIntention.RESPOND_TO_USER,
                cooldownKey = "audio_key_a",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, first.debugState.audioResult.decision)

        nowMs += 400L
        val second = executor.execute(
            bundle(
                id = "plan_second",
                intention = PetIntention.STARTLE_RECOVER,
                cooldownKey = "audio_key_b",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.SUPPRESSED, second.debugState.audioResult.decision)
        assertTrue(
            second.debugState.audioResult.reason?.startsWith("global_interval_active_") == true
        )

        nowMs += 900L
        val third = executor.execute(
            bundle(
                id = "plan_third",
                intention = PetIntention.LISTEN,
                category = AudioCategory.CURIOUS,
                cooldownKey = "audio_key_c",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, third.debugState.audioResult.decision)
    }

    @Test
    fun `audio cooldown key remains effective when global interval is disabled`() {
        var nowMs = 2_000L
        val executor = PetIntentionExecutor(
            globalAudioMinIntervalMs = 0L,
            sameCategoryAudioMinIntervalMs = 0L,
            nowProvider = { nowMs }
        )

        val first = executor.execute(
            bundle(
                id = "cooldown_first",
                intention = PetIntention.RESPOND_TO_USER,
                cooldownKey = "shared_key",
                minIntervalMs = 2_000L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, first.debugState.audioResult.decision)

        nowMs += 500L
        val second = executor.execute(
            bundle(
                id = "cooldown_second",
                intention = PetIntention.RESPOND_TO_USER,
                cooldownKey = "shared_key",
                minIntervalMs = 2_000L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.SUPPRESSED, second.debugState.audioResult.decision)
        assertTrue(
            second.debugState.audioResult.reason?.startsWith("cooldown_active_") == true
        )
    }

    @Test
    fun `reset runtime state clears audio interval guard`() {
        var nowMs = 10_000L
        val executor = PetIntentionExecutor(
            globalAudioMinIntervalMs = 1_200L,
            nowProvider = { nowMs }
        )

        val first = executor.execute(
            bundle(
                id = "reset_first",
                intention = PetIntention.RESPOND_TO_USER,
                cooldownKey = "reset_key_a",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, first.debugState.audioResult.decision)

        nowMs += 300L
        executor.resetRuntimeState()

        val second = executor.execute(
            bundle(
                id = "reset_second",
                intention = PetIntention.STARTLE_RECOVER,
                cooldownKey = "reset_key_b",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, second.debugState.audioResult.decision)
    }

    @Test
    fun `same category interval suppresses rapid replay even with different cooldown keys`() {
        var nowMs = 20_000L
        val executor = PetIntentionExecutor(
            globalAudioMinIntervalMs = 0L,
            sameCategoryAudioMinIntervalMs = 2_200L,
            nowProvider = { nowMs }
        )

        val first = executor.execute(
            bundle(
                id = "same_category_first",
                intention = PetIntention.RESPOND_TO_USER,
                category = AudioCategory.CURIOUS,
                cooldownKey = "key_a",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.EXECUTED, first.debugState.audioResult.decision)

        nowMs += 600L
        val second = executor.execute(
            bundle(
                id = "same_category_second",
                intention = PetIntention.LISTEN,
                category = AudioCategory.CURIOUS,
                cooldownKey = "key_b",
                minIntervalMs = 0L
            ),
            nowMs = nowMs
        )
        assertEquals(ChannelDecision.SUPPRESSED, second.debugState.audioResult.decision)
        assertTrue(
            second.debugState.audioResult.reason?.startsWith("same_category_interval_active_") == true
        )
    }

    private fun bundle(
        id: String,
        intention: PetIntention,
        category: AudioCategory = AudioCategory.ACKNOWLEDGMENT,
        cooldownKey: String,
        minIntervalMs: Long
    ): PetExperienceBundle {
        return PetExperienceBundle(
            sourcePlanId = id,
            sourceIntention = intention,
            debugLabel = id,
            visualDirective = VisualDirective(
                intent = PixelPetAvatarIntent.NEUTRAL,
                holdMs = 0L,
                settleMs = 0L
            ),
            audioDirective = AudioDirective(
                category = category,
                cooldownKey = cooldownKey,
                minIntervalMs = minIntervalMs
            ),
            talkDirective = null,
            priority = ExperienceExecutionPriority.NORMAL,
            interruptPolicy = ExperienceInterruptPolicy.FREE,
            antiRepeatKey = id,
            minimumRepeatIntervalMs = 0L,
            cooldownMs = 0L
        )
    }
}
