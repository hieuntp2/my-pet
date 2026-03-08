package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.teachsamples.SampleQualityFlag
import com.aipet.brain.memory.teachsamples.SampleQualityMetadata
import com.aipet.brain.memory.teachsamples.SampleQualityStatus

internal enum class SampleQualityLevel {
    HIGH,
    MEDIUM,
    LOW
}

internal enum class SampleQualityScoreDeduction {
    MISSING_FACE_CROP,
    LIMITED_SOURCE_STATUS,
    DEBUG_GENERATED_IMAGE,
    NOT_CAMERA_FRAME
}

internal data class SampleQualityScore(
    val score: Int,
    val level: SampleQualityLevel,
    val deductions: Set<SampleQualityScoreDeduction>
)

internal fun scoreSampleQuality(
    faceCropUri: String?,
    qualityMetadata: SampleQualityMetadata
): SampleQualityScore {
    var scoreValue = 50
    val deductions = linkedSetOf<SampleQualityScoreDeduction>()

    if (faceCropUri.isNullOrBlank()) {
        scoreValue -= 35
        deductions += SampleQualityScoreDeduction.MISSING_FACE_CROP
    } else {
        scoreValue += 25
    }

    if (qualityMetadata.qualityStatus == SampleQualityStatus.LIMITED_SOURCE) {
        scoreValue -= 15
        deductions += SampleQualityScoreDeduction.LIMITED_SOURCE_STATUS
    }

    if (qualityMetadata.qualityFlags.contains(SampleQualityFlag.DEBUG_GENERATED_IMAGE)) {
        scoreValue -= 10
        deductions += SampleQualityScoreDeduction.DEBUG_GENERATED_IMAGE
    }

    if (qualityMetadata.qualityFlags.contains(SampleQualityFlag.NOT_CAMERA_FRAME)) {
        scoreValue -= 10
        deductions += SampleQualityScoreDeduction.NOT_CAMERA_FRAME
    }

    val normalizedScore = scoreValue.coerceIn(0, 100)
    return SampleQualityScore(
        score = normalizedScore,
        level = deriveQualityLevel(normalizedScore),
        deductions = deductions
    )
}

internal fun deriveQualityLevel(score: Int): SampleQualityLevel {
    val normalizedScore = score.coerceIn(0, 100)
    return when {
        normalizedScore >= 75 -> SampleQualityLevel.HIGH
        normalizedScore >= 45 -> SampleQualityLevel.MEDIUM
        else -> SampleQualityLevel.LOW
    }
}
