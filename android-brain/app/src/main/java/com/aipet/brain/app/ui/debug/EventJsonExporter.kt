package com.aipet.brain.app.ui.debug

import android.content.Context
import android.util.JsonWriter
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.events.ExportCursor
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class EventJsonExporter(
    private val appContext: Context
) {
    suspend fun export(eventStore: EventStore): EventExportResult {
        return withContext(Dispatchers.IO) {
            val exportDirectory = File(appContext.filesDir, EXPORT_DIRECTORY_NAME).apply {
                mkdirs()
            }
            val timestamp = createFileNameTimestamp()
            val fileName = "events_$timestamp.json"
            val outputFile = File(exportDirectory, fileName)
            var exportedEventCount = 0
            val exportedAtMs = System.currentTimeMillis()

            outputFile.outputStream().buffered().writer(Charsets.UTF_8).use { fileWriter ->
                JsonWriter(fileWriter).use { jsonWriter ->
                    jsonWriter.setIndent("  ")
                    jsonWriter.beginObject()
                    jsonWriter.name(EXPORT_ROOT_EVENTS_FIELD).beginArray()

                    val snapshotCursor = eventStore.latestExportCursor()
                    if (snapshotCursor != null) {
                        var afterCursorExclusive: ExportCursor? = null
                        while (true) {
                            val page = eventStore.listForExportPage(
                                limit = EXPORT_PAGE_SIZE,
                                snapshotCursor = snapshotCursor,
                                afterCursorExclusive = afterCursorExclusive
                            )
                            if (page.isEmpty()) {
                                break
                            }

                            page.forEach { record ->
                                writeEvent(writer = jsonWriter, event = record.event)
                                exportedEventCount++
                            }

                            afterCursorExclusive = page.last().cursor
                        }
                    }

                    jsonWriter.endArray()
                    jsonWriter.writeManifest(
                        buildExportManifest(
                            exportedAtMs = exportedAtMs,
                            eventCount = exportedEventCount
                        )
                    )
                    jsonWriter.endObject()
                }
            }


            EventExportResult(
                fileName = fileName,
                filePath = outputFile.absolutePath,
                eventCount = exportedEventCount,
                fileSizeBytes = outputFile.length()
            )
        }
    }

    private fun writeEvent(
        writer: JsonWriter,
        event: EventEnvelope
    ) {
        writer.beginObject()
        writer.name("eventId").value(event.eventId)
        writer.name("type").value(event.type.name)
        writer.name("timestampMs").value(event.timestampMs)
        writer.name("schemaVersion").value(event.schemaVersion.toLong())

        val parsedPayload = EventPayloadParser.parse(payloadRaw = event.payloadJson)
        writer.name("payload")
        if (parsedPayload.valid) {
            writer.writeJsonValue(parsedPayload.value)
        } else {
            writer.nullValue()
            writer.name("payloadRaw").value(event.payloadJson)
        }

        writer.endObject()
    }

    private fun createFileNameTimestamp(): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return formatter.format(Date())
    }

    companion object {
        private const val EXPORT_DIRECTORY_NAME = "event-exports"
        private const val EXPORT_PAGE_SIZE = 250
    }
}

internal data class EventExportResult(
    val fileName: String,
    val filePath: String,
    val eventCount: Int,
    val fileSizeBytes: Long
)

internal data class ExportManifest(
    val exportVersion: String,
    val exportedAt: String,
    val exportedAtMs: Long,
    val eventCount: Int,
    val strategySummary: String,
    val knownLimitations: List<String>
)

internal const val EXPORT_ROOT_MANIFEST_FIELD = "manifest"
internal const val EXPORT_ROOT_EVENTS_FIELD = "events"
internal const val EXPORT_VERSION = "phase1-export-v1"

private const val EXPORT_STRATEGY_SUMMARY =
    "Chunked/paged snapshot export using descending SQLite rowid traversal."

private val EXPORT_KNOWN_LIMITATIONS = listOf(
    "Not a full low-level DB cursor streaming export; events are written in fixed-size pages.",
    "Traversal depends on SQLite rowid snapshot semantics."
)

internal fun buildExportManifest(
    exportedAtMs: Long,
    eventCount: Int
): ExportManifest {
    return ExportManifest(
        exportVersion = EXPORT_VERSION,
        exportedAt = Instant.ofEpochMilli(exportedAtMs).toString(),
        exportedAtMs = exportedAtMs,
        eventCount = eventCount,
        strategySummary = EXPORT_STRATEGY_SUMMARY,
        knownLimitations = EXPORT_KNOWN_LIMITATIONS
    )
}

private fun JsonWriter.writeJsonValue(value: Any?) {
    when (value) {
        null -> nullValue()
        is JsonObjectLiteral -> {
            beginObject()
            value.entries.forEach { (key, entryValue) ->
                name(key)
                writeJsonValue(entryValue)
            }
            endObject()
        }

        is JsonArrayLiteral -> {
            beginArray()
            value.items.forEach { item ->
                writeJsonValue(item)
            }
            endArray()
        }

        is Boolean -> value(value)
        is Int -> value(value.toLong())
        is Long -> value(value)
        is Float -> value(value.toDouble())
        is Double -> value(value)
        is Number -> value(value)
        else -> value(value.toString())
    }
}

private fun JsonWriter.writeManifest(manifest: ExportManifest) {
    name(EXPORT_ROOT_MANIFEST_FIELD)
    beginObject()
    name("exportVersion").value(manifest.exportVersion)
    name("exportedAt").value(manifest.exportedAt)
    name("exportedAtMs").value(manifest.exportedAtMs)
    name("eventCount").value(manifest.eventCount.toLong())
    name("strategySummary").value(manifest.strategySummary)
    name("knownLimitations").beginArray()
    manifest.knownLimitations.forEach { limitation ->
        value(limitation)
    }
    endArray()
    endObject()
}
