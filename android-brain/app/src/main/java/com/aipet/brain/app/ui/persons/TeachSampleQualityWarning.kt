package com.aipet.brain.app.ui.persons

internal enum class SampleQualityWarningType {
    LOW_SCORE_LEVEL,
    MISSING_FACE_CROP,
    LIMITED_SOURCE_QUALITY,
    DEBUG_GENERATED_SOURCE,
    NOT_CAMERA_FRAME_SOURCE
}

internal data class SampleQualityWarning(
    val type: SampleQualityWarningType,
    val warningMessage: String
)

internal fun deriveSampleWarnings(
    scoredQuality: SampleQualityScore
): List<SampleQualityWarning> {
    val warnings = mutableListOf<SampleQualityWarning>()

    if (scoredQuality.level == SampleQualityLevel.LOW) {
        warnings += SampleQualityWarning(
            type = SampleQualityWarningType.LOW_SCORE_LEVEL,
            warningMessage = "Quality score is low (${scoredQuality.score}/100); review this sample before training."
        )
    }

    if (scoredQuality.deductions.contains(SampleQualityScoreDeduction.MISSING_FACE_CROP)) {
        warnings += SampleQualityWarning(
            type = SampleQualityWarningType.MISSING_FACE_CROP,
            warningMessage = "Face crop is missing; review this sample before training."
        )
    }

    if (scoredQuality.deductions.contains(SampleQualityScoreDeduction.LIMITED_SOURCE_STATUS)) {
        warnings += SampleQualityWarning(
            type = SampleQualityWarningType.LIMITED_SOURCE_QUALITY,
            warningMessage = "Sample quality is limited by the current capture source."
        )
    }

    if (scoredQuality.deductions.contains(SampleQualityScoreDeduction.DEBUG_GENERATED_IMAGE)) {
        warnings += SampleQualityWarning(
            type = SampleQualityWarningType.DEBUG_GENERATED_SOURCE,
            warningMessage = "Sample was generated from debug image data."
        )
    }

    if (scoredQuality.deductions.contains(SampleQualityScoreDeduction.NOT_CAMERA_FRAME)) {
        warnings += SampleQualityWarning(
            type = SampleQualityWarningType.NOT_CAMERA_FRAME_SOURCE,
            warningMessage = "Sample is not a true camera frame capture."
        )
    }

    return warnings
}
