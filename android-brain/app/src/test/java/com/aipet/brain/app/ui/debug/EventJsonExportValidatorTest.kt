package com.aipet.brain.app.ui.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventJsonExportValidatorTest {
    private val validator = EventJsonExportValidator()

    @Test
    fun validateJsonString_validExport_passes() {
        val json = """
            {
              "manifest": {
                "exportVersion": "phase1-export-v1",
                "exportedAt": "2026-03-07T00:00:00Z",
                "eventCount": 2
              },
              "events": [
                {
                  "eventId": "event-1",
                  "type": "TEST_EVENT",
                  "timestampMs": 1000,
                  "payload": { "value": 1 }
                },
                {
                  "eventId": "event-2",
                  "type": "TEST_EVENT",
                  "timestampMs": 2000,
                  "payload": null,
                  "payloadRaw": "{invalid}"
                }
              ]
            }
        """.trimIndent()

        val result = validator.validateJsonString(
            source = "in-memory",
            jsonContent = json
        )

        assertTrue(result.success)
        assertEquals(0, result.errorCount)
    }

    @Test
    fun validateJsonString_manifestEventCountMismatch_detected() {
        val json = """
            {
              "manifest": {
                "exportVersion": "phase1-export-v1",
                "exportedAt": "2026-03-07T00:00:00Z",
                "eventCount": 1
              },
              "events": [
                {
                  "eventId": "event-1",
                  "type": "TEST_EVENT",
                  "timestampMs": 1000,
                  "payload": {}
                },
                {
                  "eventId": "event-2",
                  "type": "TEST_EVENT",
                  "timestampMs": 2000,
                  "payload": {}
                }
              ]
            }
        """.trimIndent()

        val result = validator.validateJsonString(
            source = "in-memory",
            jsonContent = json
        )

        assertFalse(result.success)
        assertTrue(
            result.messages.any { message ->
                message.severity == ValidationSeverity.ERROR &&
                    message.message.contains("eventCount", ignoreCase = true)
            }
        )
    }

    @Test
    fun validateJsonString_malformedJson_detectedSafely() {
        val malformed = """{"manifest": {"exportVersion":"v1"}, "events":[}"""

        val result = validator.validateJsonString(
            source = "in-memory",
            jsonContent = malformed
        )

        assertFalse(result.success)
        assertTrue(
            result.messages.any { message ->
                message.severity == ValidationSeverity.ERROR &&
                    message.message.contains("Malformed JSON", ignoreCase = true)
            }
        )
    }

    @Test
    fun validateJsonString_missingManifestRequiredField_detected() {
        val json = """
            {
              "manifest": {
                "exportedAt": "2026-03-07T00:00:00Z",
                "eventCount": 0
              },
              "events": []
            }
        """.trimIndent()

        val result = validator.validateJsonString(
            source = "in-memory",
            jsonContent = json
        )

        assertFalse(result.success)
        assertTrue(
            result.messages.any { message ->
                message.severity == ValidationSeverity.ERROR &&
                    message.message.contains("exportVersion", ignoreCase = true)
            }
        )
    }

    @Test
    fun validateJsonString_wrongStructure_detectedSafely() {
        val json = """
            {
              "manifest": {
                "exportVersion": "phase1-export-v1",
                "exportedAt": "2026-03-07T00:00:00Z",
                "eventCount": 1
              },
              "items": []
            }
        """.trimIndent()

        val result = validator.validateJsonString(
            source = "content://picked/events.json",
            jsonContent = json
        )

        assertFalse(result.success)
        assertTrue(
            result.messages.any { message ->
                message.severity == ValidationSeverity.ERROR &&
                    message.message.contains("'events'", ignoreCase = true)
            }
        )
    }

    @Test
    fun validateJsonString_preservesSourceForPickedFileValidation() {
        val json = """
            {
              "manifest": {
                "exportVersion": "phase1-export-v1",
                "exportedAt": "2026-03-07T00:00:00Z",
                "eventCount": 0
              },
              "events": []
            }
        """.trimIndent()

        val result = validator.validateJsonString(
            source = "content://picked/export-123.json",
            jsonContent = json
        )

        assertEquals("content://picked/export-123.json", result.source)
        assertTrue(result.success)
    }
}
