package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import org.junit.Assert.assertEquals
import org.junit.Test

class PetActivityAvatarReactionMapperTest {
    @Test
    fun `feed maps to happy avatar reaction`() {
        assertEquals(
            AvatarEmotion.HAPPY,
            PetActivityAvatarReactionMapper.toAvatarEmotion(PetActivityType.FEED)
        )
    }

    @Test
    fun `play maps to surprised avatar reaction`() {
        assertEquals(
            AvatarEmotion.SURPRISED,
            PetActivityAvatarReactionMapper.toAvatarEmotion(PetActivityType.PLAY)
        )
    }

    @Test
    fun `rest maps to sleepy avatar reaction`() {
        assertEquals(
            AvatarEmotion.SLEEPY,
            PetActivityAvatarReactionMapper.toAvatarEmotion(PetActivityType.REST)
        )
    }
}
