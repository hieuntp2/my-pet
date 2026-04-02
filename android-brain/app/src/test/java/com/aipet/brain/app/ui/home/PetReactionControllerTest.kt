package com.aipet.brain.app.ui.home

import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PetReactionControllerTest {

    @Test
    fun `keyword reaction is suppressed right after pet audio output`() {
        val controller = PetReactionController()

        controller.recordAudioOutput(System.currentTimeMillis())
        controller.triggerKeyword()

        assertNull(controller.activeReaction)
    }

    @Test
    fun `keyword reaction is allowed when self-audio guard window has passed`() {
        val controller = PetReactionController()

        controller.recordAudioOutput(nowMs = 0L)
        controller.triggerKeyword()

        assertEquals(PixelPetAvatarIntent.ATTENTIVE, controller.activeReaction?.intent)
    }

    @Test
    fun `loud sound reaction is suppressed right after pet audio output`() {
        val controller = PetReactionController()

        controller.recordAudioOutput(System.currentTimeMillis())
        controller.triggerLoudSound()

        assertNull(controller.activeReaction)
    }
}
