package com.aipet.brain.app.ui.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventPayloadParserTest {
    @Test
    fun parse_acceptsFullyValidJson() {
        val result = EventPayloadParser.parse("""{"width":640,"height":480}""")

        assertTrue(result.valid)
        assertTrue(result.value is JsonObjectLiteral)
    }

    @Test
    fun parse_acceptsArrayPayload() {
        val result = EventPayloadParser.parse("""[1,2,3]""")

        assertTrue(result.valid)
        assertTrue(result.value is JsonArrayLiteral)
    }

    @Test
    fun parse_acceptsPrimitiveString() {
        val result = EventPayloadParser.parse(""""hello"""")

        assertTrue(result.valid)
        assertTrue(result.value is String)
        assertEquals("hello", result.value)
    }

    @Test
    fun parse_acceptsPrimitiveNumberBooleanAndNull() {
        val numberResult = EventPayloadParser.parse("42")
        val booleanResult = EventPayloadParser.parse("true")
        val nullResult = EventPayloadParser.parse("null")

        assertTrue(numberResult.valid)
        assertEquals("42", (numberResult.value as JsonNumberLiteral).raw)
        assertTrue(booleanResult.valid)
        assertTrue(booleanResult.value is Boolean)
        assertTrue(nullResult.valid)
        assertNull(nullResult.value)
    }

    @Test
    fun parse_preservesLargeIntegerLexeme() {
        val largeInteger = "1234567890123456789012345678901234567890"
        val result = EventPayloadParser.parse(largeInteger)

        assertTrue(result.valid)
        assertEquals(largeInteger, (result.value as JsonNumberLiteral).raw)
    }

    @Test
    fun parse_preservesHighPrecisionDecimalLexeme() {
        val highPrecisionDecimal = "3.1415926535897932384626433832795028841971"
        val result = EventPayloadParser.parse(highPrecisionDecimal)

        assertTrue(result.valid)
        assertEquals(highPrecisionDecimal, (result.value as JsonNumberLiteral).raw)
    }

    @Test
    fun parse_preservesNestedLargeIntegerLexeme() {
        val payload = """{"value":123456789012345678901234567890}"""
        val result = EventPayloadParser.parse(payload)

        assertTrue(result.valid)
        val root = result.value as JsonObjectLiteral
        val value = root.valueFor("value") as JsonNumberLiteral
        assertEquals("123456789012345678901234567890", value.raw)
    }

    @Test
    fun parse_preservesNestedHighPrecisionDecimalLexeme() {
        val payload = """{"price":0.12345678901234567890}"""
        val result = EventPayloadParser.parse(payload)

        assertTrue(result.valid)
        val root = result.value as JsonObjectLiteral
        val price = root.valueFor("price") as JsonNumberLiteral
        assertEquals("0.12345678901234567890", price.raw)
    }

    @Test
    fun parse_preservesMixedNestedNumberLexemes() {
        val payload = """[1,2.0000000000000000003,{"x":99999999999999999999}]"""
        val result = EventPayloadParser.parse(payload)

        assertTrue(result.valid)
        val root = result.value as JsonArrayLiteral
        assertEquals("1", (root.items[0] as JsonNumberLiteral).raw)
        assertEquals("2.0000000000000000003", (root.items[1] as JsonNumberLiteral).raw)
        val nestedObject = root.items[2] as JsonObjectLiteral
        assertEquals("99999999999999999999", (nestedObject.valueFor("x") as JsonNumberLiteral).raw)
    }

    @Test
    fun parse_rejectsTrailingNonWhitespaceContent() {
        val result = EventPayloadParser.parse("""{"width":640} trailing""")

        assertFalse(result.valid)
        assertTrue(result.value == null)
    }

    @Test
    fun parse_rejectsMalformedPayload() {
        val result = EventPayloadParser.parse("""{"width":640""")

        assertFalse(result.valid)
        assertTrue(result.value == null)
    }

    @Test
    fun parse_rejectsMalformedNumericPayload() {
        val result = EventPayloadParser.parse("42oops")

        assertFalse(result.valid)
        assertNull(result.value)
    }

    @Test
    fun parse_rejectsMalformedNestedPayload() {
        val result = EventPayloadParser.parse("""{"metrics":[1,2,}""")

        assertFalse(result.valid)
        assertNull(result.value)
    }
}

private fun JsonObjectLiteral.valueFor(key: String): Any? {
    return entries.firstOrNull { it.first == key }?.second
}
