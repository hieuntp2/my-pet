package com.aipet.brain.brain.events

internal fun String.toJsonEscaped(): String {
    return buildString(length + 8) {
        for (char in this@toJsonEscaped) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
