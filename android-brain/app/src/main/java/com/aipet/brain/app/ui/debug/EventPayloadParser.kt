package com.aipet.brain.app.ui.debug

import java.math.BigDecimal

internal object EventPayloadParser {
    fun parse(payloadRaw: String): ParsedPayload {
        return try {
            val parser = JsonValueParser(payloadRaw)
            ParsedPayload(
                valid = true,
                value = parser.parseSingleValue()
            )
        } catch (_: IllegalArgumentException) {
            ParsedPayload(
                valid = false,
                value = null
            )
        }
    }
}

internal data class ParsedPayload(
    val valid: Boolean,
    val value: Any?
)

internal data class JsonObjectLiteral(
    val entries: List<Pair<String, Any?>>
)

internal data class JsonArrayLiteral(
    val items: List<Any?>
)

internal data class JsonNumberLiteral(
    val raw: String
) : Number() {
    private fun asBigDecimal(): BigDecimal = BigDecimal(raw)

    override fun toByte(): Byte = asBigDecimal().toByte()

    override fun toDouble(): Double = asBigDecimal().toDouble()

    override fun toFloat(): Float = asBigDecimal().toFloat()

    override fun toInt(): Int = asBigDecimal().toInt()

    override fun toLong(): Long = asBigDecimal().toLong()

    override fun toShort(): Short = asBigDecimal().toShort()

    override fun toString(): String = raw
}

private class JsonValueParser(
    private val source: String
) {
    private var index = 0

    fun parseSingleValue(): Any? {
        skipWhitespace()
        if (index >= source.length) {
            throw IllegalArgumentException("Payload is empty")
        }

        val value = parseValue()
        skipWhitespace()
        if (index != source.length) {
            throw IllegalArgumentException("Trailing non-whitespace content")
        }
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        val current = peek()
        return when (current) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> parseLiteral("true", true)
            'f' -> parseLiteral("false", false)
            'n' -> parseLiteral("null", null)
            '-', in '0'..'9' -> parseNumber()
            else -> throw IllegalArgumentException("Unexpected token: $current")
        }
    }

    private fun parseObject(): JsonObjectLiteral {
        expect('{')
        skipWhitespace()
        if (consumeIf('}')) {
            return JsonObjectLiteral(emptyList())
        }

        val entries = mutableListOf<Pair<String, Any?>>()
        while (true) {
            skipWhitespace()
            if (peek() != '"') {
                throw IllegalArgumentException("Object key must be a string")
            }
            val key = parseString()
            skipWhitespace()
            expect(':')
            val value = parseValue()
            entries.add(key to value)
            skipWhitespace()
            when {
                consumeIf(',') -> continue
                consumeIf('}') -> return JsonObjectLiteral(entries)
                else -> throw IllegalArgumentException("Expected ',' or '}' in object")
            }
        }
    }

    private fun parseArray(): JsonArrayLiteral {
        expect('[')
        skipWhitespace()
        if (consumeIf(']')) {
            return JsonArrayLiteral(emptyList())
        }

        val items = mutableListOf<Any?>()
        while (true) {
            items.add(parseValue())
            skipWhitespace()
            when {
                consumeIf(',') -> continue
                consumeIf(']') -> return JsonArrayLiteral(items)
                else -> throw IllegalArgumentException("Expected ',' or ']' in array")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val output = StringBuilder()
        while (index < source.length) {
            val char = source[index++]
            when (char) {
                '"' -> return output.toString()
                '\\' -> output.append(parseEscapeSequence())
                else -> {
                    if (char.code in 0x00..0x1F) {
                        throw IllegalArgumentException("Unescaped control character in string")
                    }
                    output.append(char)
                }
            }
        }
        throw IllegalArgumentException("Unterminated string")
    }

    private fun parseEscapeSequence(): Char {
        if (index >= source.length) {
            throw IllegalArgumentException("Invalid escape sequence")
        }

        return when (val escaped = source[index++]) {
            '"', '\\', '/' -> escaped
            'b' -> '\b'
            'f' -> '\u000C'
            'n' -> '\n'
            'r' -> '\r'
            't' -> '\t'
            'u' -> parseUnicodeEscape()
            else -> throw IllegalArgumentException("Invalid escape sequence: \\$escaped")
        }
    }

    private fun parseUnicodeEscape(): Char {
        if (index + 4 > source.length) {
            throw IllegalArgumentException("Invalid unicode escape")
        }
        val hex = source.substring(index, index + 4)
        val codePoint = hex.toIntOrNull(16)
            ?: throw IllegalArgumentException("Invalid unicode escape")
        index += 4
        return codePoint.toChar()
    }

    private fun parseLiteral(
        literal: String,
        value: Any?
    ): Any? {
        if (!source.startsWith(literal, index)) {
            throw IllegalArgumentException("Invalid literal")
        }
        index += literal.length
        return value
    }

    private fun parseNumber(): JsonNumberLiteral {
        val start = index
        consumeIf('-')
        parseIntegerPart()
        parseFractionPartIfPresent()
        parseExponentPartIfPresent()

        val rawNumber = source.substring(start, index)
        try {
            BigDecimal(rawNumber)
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("Invalid number")
        }
        return JsonNumberLiteral(rawNumber)
    }

    private fun parseIntegerPart() {
        if (index >= source.length) {
            throw IllegalArgumentException("Invalid number")
        }

        when (source[index]) {
            '0' -> index++
            in '1'..'9' -> {
                index++
                while (index < source.length && source[index].isDigit()) {
                    index++
                }
            }

            else -> throw IllegalArgumentException("Invalid number")
        }
    }

    private fun parseFractionPartIfPresent() {
        if (!consumeIf('.')) {
            return
        }

        if (index >= source.length || !source[index].isDigit()) {
            throw IllegalArgumentException("Invalid number fraction")
        }
        while (index < source.length && source[index].isDigit()) {
            index++
        }
    }

    private fun parseExponentPartIfPresent() {
        if (index >= source.length || (source[index] != 'e' && source[index] != 'E')) {
            return
        }

        index++
        if (index < source.length && (source[index] == '+' || source[index] == '-')) {
            index++
        }
        if (index >= source.length || !source[index].isDigit()) {
            throw IllegalArgumentException("Invalid number exponent")
        }
        while (index < source.length && source[index].isDigit()) {
            index++
        }
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].isJsonWhitespace()) {
            index++
        }
    }

    private fun peek(): Char {
        if (index >= source.length) {
            throw IllegalArgumentException("Unexpected end of payload")
        }
        return source[index]
    }

    private fun consumeIf(expected: Char): Boolean {
        if (index < source.length && source[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun expect(expected: Char) {
        if (!consumeIf(expected)) {
            throw IllegalArgumentException("Expected '$expected'")
        }
    }
}

private fun Char.isJsonWhitespace(): Boolean {
    return this == ' ' || this == '\n' || this == '\r' || this == '\t'
}
