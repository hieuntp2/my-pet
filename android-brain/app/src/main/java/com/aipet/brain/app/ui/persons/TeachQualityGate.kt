package com.aipet.brain.app.ui.persons

internal enum class TeachQualityGateFailureReason {
    MINIMUM_SAMPLE_COUNT_NOT_MET,
    MINIMUM_QUALIFIED_SAMPLE_COUNT_NOT_MET
}

internal data class TeachQualityGateFailure(
    val reason: TeachQualityGateFailureReason,
    val message: String
)

internal data class TeachQualityGateResult(
    val minimumSampleCount: Int,
    val requiredQualifiedSampleCount: Int,
    val totalSampleCount: Int,
    val qualifiedSampleCount: Int,
    val failures: List<TeachQualityGateFailure>,
    val failingSampleObservationIds: List<String>
) {
    val canSaveTeachPerson: Boolean
        get() = failures.isEmpty()

    val hasEnoughQualifiedSamples: Boolean
        get() = qualifiedSampleCount >= requiredQualifiedSampleCount

    val saveBlockedReason: String?
        get() = failures.firstOrNull()?.message
}

internal fun evaluateTeachQualityGate(
    capturedSamples: List<TeachPersonCapturedSample>,
    minimumSampleCount: Int = 3,
    minimumQualifiedSampleCount: Int = 1
): TeachQualityGateResult {
    val failures = mutableListOf<TeachQualityGateFailure>()
    val normalizedMinimumSampleCount = minimumSampleCount.coerceAtLeast(1)
    val normalizedMinimumQualifiedSampleCount = minimumQualifiedSampleCount.coerceAtLeast(1)

    if (capturedSamples.size < normalizedMinimumSampleCount) {
        failures += TeachQualityGateFailure(
            reason = TeachQualityGateFailureReason.MINIMUM_SAMPLE_COUNT_NOT_MET,
            message = "Capture at least 3 samples before saving."
        )
    }

    val qualifiedSamples = capturedSamples.filter { sample -> sample.isQualifiedForHardGate() }
    if (capturedSamples.size >= normalizedMinimumSampleCount &&
        qualifiedSamples.size < normalizedMinimumQualifiedSampleCount
    ) {
        failures += TeachQualityGateFailure(
            reason = TeachQualityGateFailureReason.MINIMUM_QUALIFIED_SAMPLE_COUNT_NOT_MET,
            message = "Capture at least one sample with a face crop and MEDIUM/HIGH quality level before saving."
        )
    }

    val failingSampleObservationIds = if (
        failures.any { failure ->
            failure.reason == TeachQualityGateFailureReason.MINIMUM_QUALIFIED_SAMPLE_COUNT_NOT_MET
        }
    ) {
        capturedSamples
            .asSequence()
            .filterNot { sample -> sample.isQualifiedForHardGate() }
            .map { sample -> sample.observationId }
            .sorted()
            .toList()
    } else {
        emptyList()
    }

    return TeachQualityGateResult(
        minimumSampleCount = normalizedMinimumSampleCount,
        requiredQualifiedSampleCount = normalizedMinimumQualifiedSampleCount,
        totalSampleCount = capturedSamples.size,
        qualifiedSampleCount = qualifiedSamples.size,
        failures = failures,
        failingSampleObservationIds = failingSampleObservationIds
    )
}

internal fun TeachPersonCapturedSample.isQualifiedForHardGate(): Boolean {
    return !faceCropUri.isNullOrBlank() && scoredQuality.level != SampleQualityLevel.LOW
}
