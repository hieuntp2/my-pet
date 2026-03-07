package com.aipet.brain.app.ui.debug

import java.io.File

internal class EventJsonExportValidator {
    fun validateFile(file: File): ExportValidationResult {
        if (!file.exists()) {
            return ExportValidationResult(
                source = file.absolutePath,
                messages = listOf(
                    ExportValidationMessage.error("Export file does not exist.")
                )
            )
        }

        val content = runCatching {
            file.readText(Charsets.UTF_8)
        }.getOrElse { error ->
            return ExportValidationResult(
                source = file.absolutePath,
                messages = listOf(
                    ExportValidationMessage.error(
                        "Failed to read export file: ${error.message ?: "Unknown read error"}"
                    )
                )
            )
        }

        return validateJsonString(
            source = file.absolutePath,
            jsonContent = content
        )
    }

    internal fun validateJsonString(
        source: String,
        jsonContent: String
    ): ExportValidationResult {
        val messages = mutableListOf<ExportValidationMessage>()
        val parsedRoot = EventPayloadParser.parse(jsonContent)
        if (!parsedRoot.valid) {
            messages.add(ExportValidationMessage.error("Malformed JSON: unable to parse export file."))
            return ExportValidationResult(
                source = source,
                messages = messages
            )
        }

        val rootObject = parsedRoot.value as? JsonObjectLiteral
        if (rootObject == null) {
            messages.add(ExportValidationMessage.error("Root JSON value must be an object."))
            return ExportValidationResult(
                source = source,
                messages = messages
            )
        }

        val manifest = rootObject.valueFor(EXPORT_ROOT_MANIFEST_FIELD)
        val events = rootObject.valueFor(EXPORT_ROOT_EVENTS_FIELD)
        val manifestObject = manifest as? JsonObjectLiteral
        val eventsArray = events as? JsonArrayLiteral

        if (manifestObject == null) {
            messages.add(ExportValidationMessage.error("Missing or invalid '$EXPORT_ROOT_MANIFEST_FIELD' object."))
        }
        if (eventsArray == null) {
            messages.add(ExportValidationMessage.error("Missing or invalid '$EXPORT_ROOT_EVENTS_FIELD' array."))
        }

        val manifestEventCount = manifestObject?.let { validateManifest(it, messages) }
        val actualEventCount = eventsArray?.items?.size
        if (manifestEventCount != null && actualEventCount != null && manifestEventCount != actualEventCount) {
            messages.add(
                ExportValidationMessage.error(
                    "Manifest eventCount ($manifestEventCount) does not match actual events ($actualEventCount)."
                )
            )
        }

        if (eventsArray != null) {
            validateEvents(eventsArray, messages)
        }

        return ExportValidationResult(
            source = source,
            messages = messages
        )
    }

    private fun validateManifest(
        manifest: JsonObjectLiteral,
        messages: MutableList<ExportValidationMessage>
    ): Int? {
        val exportVersion = manifest.valueFor("exportVersion")
        if (exportVersion !is String || exportVersion.isBlank()) {
            messages.add(ExportValidationMessage.error("Manifest field 'exportVersion' is missing or blank."))
        }

        val exportedAt = manifest.valueFor("exportedAt")
        if (exportedAt !is String || exportedAt.isBlank()) {
            messages.add(ExportValidationMessage.error("Manifest field 'exportedAt' is missing or blank."))
        }

        val eventCountValue = manifest.valueFor("eventCount")
        val eventCount = eventCountValue.asNonNegativeInt()
        if (eventCount == null) {
            messages.add(
                ExportValidationMessage.error(
                    "Manifest field 'eventCount' is missing or not a non-negative integer."
                )
            )
        }

        val strategySummary = manifest.valueFor("strategySummary")
        if (strategySummary !is String || strategySummary.isBlank()) {
            messages.add(
                ExportValidationMessage.warning(
                    "Manifest field 'strategySummary' is missing or blank."
                )
            )
        }

        val knownLimitations = manifest.valueFor("knownLimitations")
        if (knownLimitations !is JsonArrayLiteral || knownLimitations.items.isEmpty()) {
            messages.add(
                ExportValidationMessage.warning(
                    "Manifest field 'knownLimitations' is missing or empty."
                )
            )
        }

        return eventCount
    }

    private fun validateEvents(
        events: JsonArrayLiteral,
        messages: MutableList<ExportValidationMessage>
    ) {
        events.items.forEachIndexed { index, item ->
            val event = item as? JsonObjectLiteral
            if (event == null) {
                messages.add(ExportValidationMessage.error("Event[$index] is not an object."))
                return@forEachIndexed
            }

            val eventId = event.valueFor("eventId")
            if (eventId !is String || eventId.isBlank()) {
                messages.add(ExportValidationMessage.error("Event[$index] field 'eventId' is missing or blank."))
            }

            val type = event.valueFor("type")
            if (type !is String || type.isBlank()) {
                messages.add(ExportValidationMessage.error("Event[$index] field 'type' is missing or blank."))
            }

            val timestamp = event.valueFor("timestampMs")
            if (timestamp.asLongOrNull() == null) {
                messages.add(
                    ExportValidationMessage.error(
                        "Event[$index] field 'timestampMs' is missing or not numeric."
                    )
                )
            }

            val hasPayload = event.hasKey("payload")
            val hasPayloadRaw = event.hasKey("payloadRaw")
            if (!hasPayload && !hasPayloadRaw) {
                messages.add(
                    ExportValidationMessage.error(
                        "Event[$index] must include 'payload' or 'payloadRaw'."
                    )
                )
            }

            if (hasPayload && event.valueFor("payload") == null && !hasPayloadRaw) {
                messages.add(
                    ExportValidationMessage.warning(
                        "Event[$index] has null payload without payloadRaw fallback."
                    )
                )
            }
        }
    }
}

internal data class ExportValidationResult(
    val source: String,
    val messages: List<ExportValidationMessage>
) {
    val success: Boolean
        get() = errorCount == 0

    val errorCount: Int
        get() = messages.count { it.severity == ValidationSeverity.ERROR }

    val warningCount: Int
        get() = messages.count { it.severity == ValidationSeverity.WARNING }

    fun toDisplayText(): String {
        val header = if (success) {
            "Validation passed for $source"
        } else {
            "Validation failed for $source"
        }

        val detailLines = buildList {
            add("Errors: $errorCount")
            add("Warnings: $warningCount")
            messages.forEach { message ->
                add("[${message.severity.name}] ${message.message}")
            }
        }
        return (listOf(header) + detailLines).joinToString(separator = "\n")
    }
}

internal data class ExportValidationMessage(
    val severity: ValidationSeverity,
    val message: String
) {
    companion object {
        fun error(message: String): ExportValidationMessage {
            return ExportValidationMessage(
                severity = ValidationSeverity.ERROR,
                message = message
            )
        }

        fun warning(message: String): ExportValidationMessage {
            return ExportValidationMessage(
                severity = ValidationSeverity.WARNING,
                message = message
            )
        }
    }
}

internal enum class ValidationSeverity {
    ERROR,
    WARNING
}

private fun JsonObjectLiteral.valueFor(key: String): Any? {
    return entries.firstOrNull { (entryKey, _) -> entryKey == key }?.second
}

private fun JsonObjectLiteral.hasKey(key: String): Boolean {
    return entries.any { (entryKey, _) -> entryKey == key }
}

private fun Any?.asNonNegativeInt(): Int? {
    val longValue = asLongOrNull() ?: return null
    if (longValue < 0L || longValue > Int.MAX_VALUE.toLong()) {
        return null
    }
    return longValue.toInt()
}

private fun Any?.asLongOrNull(): Long? {
    return when (this) {
        is JsonNumberLiteral -> toLongOrNullStrict(raw)
        is Number -> toLong()
        else -> null
    }
}

private fun toLongOrNullStrict(raw: String): Long? {
    if (raw.contains('.') || raw.contains('e') || raw.contains('E')) {
        return null
    }
    return raw.toLongOrNull()
}
