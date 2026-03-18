package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import org.junit.Assert.assertEquals
import org.junit.Test

class PetInteractionAvatarReactionMapperTest {
    @Test
    fun `tap maps to happy avatar reaction`() {
        assertEquals(
            AvatarEmotion.HAPPY,
            PetInteractionAvatarReactionMapper.toAvatarEmotion(PetInteractionType.TAP)
        )
    }

    @Test
    fun `long press maps to surprised avatar reaction`() {
        assertEquals(
            AvatarEmotion.SURPRISED,
            PetInteractionAvatarReactionMapper.toAvatarEmotion(PetInteractionType.LONG_PRESS)
        )
    }
}
