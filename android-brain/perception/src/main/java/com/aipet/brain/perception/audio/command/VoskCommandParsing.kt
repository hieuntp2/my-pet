package com.aipet.brain.perception.audio.command

import java.util.Locale

object VoskCommandParsing {
    val supportedCommands: Set<String> = linkedSetOf(
        "wakeup",
        "learn person",
        "learn object",
        "play random"
    )

    private val partialPattern = Regex("\"partial\"\\s*:\\s*\"([^\"]*)\"")
    private val textPattern = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"")

    fun buildRestrictedGrammarJson(): String {
        return buildString {
            append("[")
            append(supportedCommands.joinToString(",") { phrase ->
                "\"$phrase\""
            })
            append(",\"[unk]\"]")
        }
    }

    fun parsePartialText(hypothesisJson: String): String? {
        return partialPattern.find(hypothesisJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.ifBlank { null }
    }

    fun parseFinalText(hypothesisJson: String): String? {
        return textPattern.find(hypothesisJson)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.ifBlank { null }
    }

    fun normalizeCommand(rawText: String): String? {
        val normalized = rawText
            .lowercase(Locale.US)
            .trim()
            .replace(Regex("\\s+"), " ")
        return normalized.takeIf { candidate -> candidate in supportedCommands }
    }
}

