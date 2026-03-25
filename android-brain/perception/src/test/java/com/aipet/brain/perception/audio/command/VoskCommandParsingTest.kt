package com.aipet.brain.perception.audio.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoskCommandParsingTest {
    @Test
    fun `normalizeCommand accepts supported command text`() {
        assertEquals("wakeup", VoskCommandParsing.normalizeCommand("  Wakeup "))
        assertEquals("learn person", VoskCommandParsing.normalizeCommand("learn   person"))
        assertEquals("learn object", VoskCommandParsing.normalizeCommand("LEARN OBJECT"))
        assertEquals("play random", VoskCommandParsing.normalizeCommand("play random"))
    }

    @Test
    fun `normalizeCommand rejects unsupported phrases`() {
        assertNull(VoskCommandParsing.normalizeCommand(""))
        assertNull(VoskCommandParsing.normalizeCommand("wake up"))
        assertNull(VoskCommandParsing.normalizeCommand("play"))
        assertNull(VoskCommandParsing.normalizeCommand("learn"))
    }

    @Test
    fun `buildRestrictedGrammarJson contains only supported commands and unk`() {
        val grammarJson = VoskCommandParsing.buildRestrictedGrammarJson()
        assertEquals(
            "[\"wakeup\",\"learn person\",\"learn object\",\"play random\",\"[unk]\"]",
            grammarJson
        )
    }

    @Test
    fun `parsePartialText and parseFinalText extract hypothesis safely`() {
        assertEquals("learn", VoskCommandParsing.parsePartialText("{\"partial\":\"learn\"}"))
        assertEquals(
            "learn person",
            VoskCommandParsing.parseFinalText("{\"result\":[],\"text\":\"learn person\"}")
        )
        assertTrue(VoskCommandParsing.parsePartialText("{\"other\":\"value\"}") == null)
        assertTrue(VoskCommandParsing.parseFinalText("{\"other\":\"value\"}") == null)
    }
}

