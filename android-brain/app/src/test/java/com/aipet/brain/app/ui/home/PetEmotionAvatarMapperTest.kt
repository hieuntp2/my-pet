package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import org.junit.Assert.assertEquals
import org.junit.Test

class PetEmotionAvatarMapperTest {
    @Test
    fun `maps sleepy emotion to sleepy avatar`() {
        assertEquals(
            AvatarEmotion.SLEEPY,
            PetEmotionAvatarMapper.toAvatarEmotion(PetEmotion.SLEEPY)
        )
    }

    @Test
    fun `maps excited emotion to surprised avatar`() {
        assertEquals(
            AvatarEmotion.SURPRISED,
            PetEmotionAvatarMapper.toAvatarEmotion(PetEmotion.EXCITED)
        )
    }
}
