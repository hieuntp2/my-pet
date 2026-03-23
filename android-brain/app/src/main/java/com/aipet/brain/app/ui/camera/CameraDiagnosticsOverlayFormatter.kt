package com.aipet.brain.app.ui.camera

import java.util.Locale

internal fun formatPromptCooldownText(
    promptSuppression: CameraObjectPromptSuppressionState
): String {
    return if (promptSuppression.canonicalLabel.isNullOrBlank()) {
        "Prompt cooldown: n/a"
    } else if (promptSuppression.isSuppressed) {
        "Prompt cooldown: ${promptSuppression.canonicalLabel} (${promptSuppression.remainingMs} ms left)"
    } else {
        "Prompt cooldown: ${promptSuppression.canonicalLabel} not suppressed"
    }
}

internal fun formatDetectionSummaryLine(
    index: Int,
    detection: CameraObjectDebugItem,
    nowMs: Long
): String {
    val bboxSummary = formatBoundingBoxSummary(detection)
    val ageMs = (nowMs - detection.observedAtMs).coerceAtLeast(0L)
    return buildString {
        append("  ${index + 1}. ")
        append(detection.displayLabel)
        append(" [")
        append(
            when (detection.knownState) {
                CameraObjectKnownState.KNOWN -> "known"
                CameraObjectKnownState.UNKNOWN -> "unknown"
                CameraObjectKnownState.UNRESOLVED -> "unresolved"
            }
        )
        append("] ")
        append(
            detection.confidence?.let {
                String.format(Locale.US, "%.3f", it)
            } ?: "n/a"
        )
        append(", bbox=")
        append(bboxSummary)
        append(", age=")
        append(ageMs)
        append(" ms")
    }
}

private fun formatBoundingBoxSummary(detection: CameraObjectDebugItem): String {
    if (detection.boundingBoxWidthPx == null || detection.boundingBoxHeightPx == null) {
        return "n/a"
    }
    return buildString {
        append("${detection.boundingBoxWidthPx}x${detection.boundingBoxHeightPx}px")
        detection.boundingBoxAreaPercent?.let { areaPercent ->
            append(" ")
            append(String.format(Locale.US, "%.1f%%", areaPercent))
        }
    }
}
