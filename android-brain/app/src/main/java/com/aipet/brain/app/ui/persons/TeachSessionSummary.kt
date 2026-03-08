package com.aipet.brain.app.ui.persons

internal data class TeachSessionSummary(
    val totalSampleCount: Int,
    val qualifiedSampleCount: Int,
    val requiredQualifiedSampleCount: Int,
    val warningSampleCount: Int,
    val totalWarningCount: Int,
    val canSave: Boolean,
    val blockedReason: String?,
    val preferredSampleId: String?
) {
    val hasWarnings: Boolean
        get() = totalWarningCount > 0
}

internal fun deriveTeachSessionSummary(
    capturedSamples: List<TeachPersonCapturedSample>,
    qualityGateResult: TeachQualityGateResult,
    bestSampleSelection: BestSampleSelection
): TeachSessionSummary {
    val warningSampleCount = capturedSamples.count { sample -> sample.hasSoftWarning }
    val totalWarningCount = capturedSamples.sumOf { sample -> sample.softWarnings.size }
    return TeachSessionSummary(
        totalSampleCount = capturedSamples.size,
        qualifiedSampleCount = qualityGateResult.qualifiedSampleCount,
        requiredQualifiedSampleCount = qualityGateResult.requiredQualifiedSampleCount,
        warningSampleCount = warningSampleCount,
        totalWarningCount = totalWarningCount,
        canSave = qualityGateResult.canSaveTeachPerson,
        blockedReason = qualityGateResult.saveBlockedReason,
        preferredSampleId = bestSampleSelection.bestSampleId
    )
}
