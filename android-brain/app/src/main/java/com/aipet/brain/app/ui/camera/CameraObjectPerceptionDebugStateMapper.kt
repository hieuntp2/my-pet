package com.aipet.brain.app.ui.camera

import com.aipet.brain.app.perception.UnknownObjectPromptSuppressionDebugState
import com.aipet.brain.perception.vision.objectdetection.model.DetectedObject
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import java.util.Locale

internal data class ResolvedObjectLabel(
    val label: String,
    val fallbackReason: String? = null
)

internal fun resolveObjectLabelForDisplay(rawLabel: String?): ResolvedObjectLabel {
    val sanitized = rawLabel?.trim().orEmpty()
    if (sanitized.isBlank()) {
        return ResolvedObjectLabel(
            label = UNKNOWN_OBJECT_LABEL,
            fallbackReason = "missing_label"
        )
    }
    if (sanitized == OBJECT_UNKNOWN_TOKEN || sanitized.startsWith(OBJECT_CLASS_FALLBACK_PREFIX)) {
        return ResolvedObjectLabel(
            label = UNKNOWN_OBJECT_LABEL,
            fallbackReason = "class_id_fallback:$sanitized"
        )
    }
    return ResolvedObjectLabel(label = sanitized)
}

internal fun buildCameraObjectPerceptionDebugState(
    detectionResult: ObjectDetectionResult,
    knownDisplayNamesByCanonical: Map<String, String?>,
    promptSuppression: UnknownObjectPromptSuppressionDebugState
): CameraObjectPerceptionDebugState {
    return CameraObjectPerceptionDebugState(
        modelName = detectionResult.modelName,
        inferenceDurationMs = detectionResult.inferenceDurationMs,
        updatedAtMs = detectionResult.timestampMs,
        promptSuppression = CameraObjectPromptSuppressionState(
            canonicalLabel = promptSuppression.canonicalLabel,
            isSuppressed = promptSuppression.isSuppressed,
            suppressedUntilMs = promptSuppression.suppressedUntilMs,
            remainingMs = promptSuppression.remainingMs
        ),
        detections = detectionResult.detections.map { detection ->
            detection.toCameraObjectDebugItem(
                detectionResult = detectionResult,
                knownDisplayName = knownDisplayNamesByCanonical[resolveObjectLabelForDisplay(detection.label).label]
            )
        }
    )
}

internal fun preferredObjectDisplayLabel(
    rawLabel: String,
    knownDisplayNamesByCanonical: Map<String, String?>
): String {
    val resolvedLabel = resolveObjectLabelForDisplay(rawLabel)
    if (resolvedLabel.fallbackReason != null) {
        return resolvedLabel.label
    }
    return knownDisplayNamesByCanonical[resolvedLabel.label] ?: resolvedLabel.label
}

internal fun formatConfidence(confidence: Float?): String {
    return confidence?.let { value ->
        String.format(Locale.US, "%.3f", value)
    } ?: "n/a"
}

private fun DetectedObject.toCameraObjectDebugItem(
    detectionResult: ObjectDetectionResult,
    knownDisplayName: String?
): CameraObjectDebugItem {
    val mappedLabel = resolveObjectLabelForDisplay(label)
    val mappedCanonicalLabel = mappedLabel.label
    val bbox = boundingBox
    val bboxWidth = bbox?.let { (it.right - it.left).coerceAtLeast(0) }
    val bboxHeight = bbox?.let { (it.bottom - it.top).coerceAtLeast(0) }
    val bboxAreaPercent = if (
        bboxWidth != null &&
        bboxHeight != null &&
        detectionResult.sourceFrameWidth > 0 &&
        detectionResult.sourceFrameHeight > 0
    ) {
        val frameArea = detectionResult.sourceFrameWidth * detectionResult.sourceFrameHeight
        ((bboxWidth * bboxHeight).toFloat() / frameArea.toFloat()) * 100f
    } else {
        null
    }
    return CameraObjectDebugItem(
        canonicalLabel = mappedCanonicalLabel,
        displayLabel = knownDisplayName ?: mappedCanonicalLabel,
        knownState = when {
            mappedLabel.fallbackReason != null -> CameraObjectKnownState.UNRESOLVED
            knownDisplayName != null -> CameraObjectKnownState.KNOWN
            else -> CameraObjectKnownState.UNKNOWN
        },
        confidence = confidence
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, 1f),
        boundingBoxWidthPx = bboxWidth,
        boundingBoxHeightPx = bboxHeight,
        boundingBoxAreaPercent = bboxAreaPercent,
        observedAtMs = detectionResult.timestampMs
    )
}

internal const val UNKNOWN_OBJECT_LABEL = "Unknown object"
internal const val OBJECT_UNKNOWN_TOKEN = "???"
internal const val OBJECT_CLASS_FALLBACK_PREFIX = "class_"
