package com.aipet.brain.app.ui.camera

internal data class CameraObjectPerceptionDebugState(
    val modelName: String? = null,
    val inferenceDurationMs: Long? = null,
    val detections: List<CameraObjectDebugItem> = emptyList(),
    val updatedAtMs: Long? = null,
    val promptSuppression: CameraObjectPromptSuppressionState = CameraObjectPromptSuppressionState()
) {
    companion object {
        fun empty(): CameraObjectPerceptionDebugState = CameraObjectPerceptionDebugState()
    }
}

internal data class CameraObjectDebugItem(
    val canonicalLabel: String,
    val displayLabel: String,
    val knownState: CameraObjectKnownState,
    val confidence: Float?,
    val boundingBoxWidthPx: Int?,
    val boundingBoxHeightPx: Int?,
    val boundingBoxAreaPercent: Float?,
    val observedAtMs: Long
)

internal enum class CameraObjectKnownState {
    KNOWN,
    UNKNOWN,
    UNRESOLVED
}

internal data class CameraObjectPromptSuppressionState(
    val canonicalLabel: String? = null,
    val isSuppressed: Boolean = false,
    val suppressedUntilMs: Long? = null,
    val remainingMs: Long = 0L
)
