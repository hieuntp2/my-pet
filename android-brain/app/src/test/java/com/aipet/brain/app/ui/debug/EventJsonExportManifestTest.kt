package com.aipet.brain.app.ui.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventJsonExportManifestTest {
    @Test
    fun buildExportManifest_containsRequiredFieldsAndEventCount() {
        val exportedAtMs = 1_700_000_000_000L
        val eventCount = 7

        val manifest = buildExportManifest(
            exportedAtMs = exportedAtMs,
            eventCount = eventCount
        )

        assertEquals(EXPORT_VERSION, manifest.exportVersion)
        assertEquals("2023-11-14T22:13:20Z", manifest.exportedAt)
        assertEquals(exportedAtMs, manifest.exportedAtMs)
        assertEquals(eventCount, manifest.eventCount)
    }

    @Test
    fun buildExportManifest_describesCurrentStrategyAndLimitations() {
        val manifest = buildExportManifest(
            exportedAtMs = 1_700_000_000_000L,
            eventCount = 1
        )

        assertTrue(manifest.strategySummary.contains("chunked", ignoreCase = true))
        assertTrue(manifest.strategySummary.contains("rowid", ignoreCase = true))
        assertTrue(
            manifest.knownLimitations.any {
                it.contains("cursor streaming", ignoreCase = true)
            }
        )
        assertTrue(
            manifest.knownLimitations.any {
                it.contains("rowid", ignoreCase = true)
            }
        )
    }

    @Test
    fun exportRootFieldNames_includeManifestAndEvents() {
        assertEquals("manifest", EXPORT_ROOT_MANIFEST_FIELD)
        assertEquals("events", EXPORT_ROOT_EVENTS_FIELD)
    }
}
