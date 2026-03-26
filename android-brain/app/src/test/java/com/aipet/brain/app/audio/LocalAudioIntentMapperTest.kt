package com.aipet.brain.app.audio

import com.aipet.brain.brain.events.audio.AudioIntent
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalAudioIntentMapperTest {
    private val mapper = LocalAudioIntentMapper()

    @Test
    fun `normalize handles supported variants`() {
        assertEquals("wakeup", mapper.normalize(" wake up "))
        assertEquals("wakeup", mapper.normalize("Wakeup"))
        assertEquals("learn person", mapper.normalize("learn a person"))
        assertEquals("learn object", mapper.normalize("learn object"))
        assertEquals("play random", mapper.normalize("play random"))
    }

    @Test
    fun `mapToIntent maps supported commands and unknown safely`() {
        assertEquals(AudioIntent.WAKE_UP, mapper.mapToIntent("wakeup"))
        assertEquals(AudioIntent.LEARN_PERSON, mapper.mapToIntent("learn person"))
        assertEquals(AudioIntent.LEARN_OBJECT, mapper.mapToIntent("learn object"))
        assertEquals(AudioIntent.PLAY_RANDOM, mapper.mapToIntent("play random"))
        assertEquals(AudioIntent.UNKNOWN, mapper.mapToIntent("play"))
        assertEquals(AudioIntent.UNKNOWN, mapper.mapToIntent(""))
    }
}
