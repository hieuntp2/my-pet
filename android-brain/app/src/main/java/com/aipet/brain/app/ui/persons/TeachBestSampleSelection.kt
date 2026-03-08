package com.aipet.brain.app.ui.persons

internal data class BestSampleSelection(
    val bestSampleId: String?,
    val preferredSampleIds: Set<String>,
    val rankedSampleObservationIds: List<String>
) {
    val hasPreferredSample: Boolean
        get() = bestSampleId != null
}

internal fun selectBestSamples(
    capturedSamples: List<TeachPersonCapturedSample>
): BestSampleSelection {
    if (capturedSamples.isEmpty()) {
        return BestSampleSelection(
            bestSampleId = null,
            preferredSampleIds = emptySet(),
            rankedSampleObservationIds = emptyList()
        )
    }

    val rankedSamples = capturedSamples.sortedWith(
        compareByDescending<TeachPersonCapturedSample> { sample -> sample.isQualifiedForHardGate() }
            .thenByDescending { sample -> sample.scoredQuality.score }
            .thenByDescending { sample -> qualityLevelRank(sample.scoredQuality.level) }
            .thenByDescending { sample -> !sample.faceCropUri.isNullOrBlank() }
            .thenByDescending { sample -> sample.observedAtMs }
            .thenBy { sample -> sample.observationId }
    )

    val bestSampleId = rankedSamples.first().observationId
    return BestSampleSelection(
        bestSampleId = bestSampleId,
        preferredSampleIds = setOf(bestSampleId),
        rankedSampleObservationIds = rankedSamples.map { sample -> sample.observationId }
    )
}

private fun qualityLevelRank(level: SampleQualityLevel): Int {
    return when (level) {
        SampleQualityLevel.HIGH -> 2
        SampleQualityLevel.MEDIUM -> 1
        SampleQualityLevel.LOW -> 0
    }
}
