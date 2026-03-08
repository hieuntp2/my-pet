package com.aipet.brain.app.ui.persons

internal enum class SampleRetentionHint {
    KEEP,
    RECAPTURE_SUGGESTED,
    REMOVE_SUGGESTED
}

internal enum class SamplePruningReason {
    PREFERRED_SAMPLE,
    MISSING_FACE_CROP,
    LOW_QUALITY_LEVEL,
    HAS_SOFT_WARNING,
    NOT_QUALIFIED_FOR_HARD_GATE,
    WEAKER_THAN_PREFERRED_SAMPLE
}

internal data class SamplePruningSuggestion(
    val observationId: String,
    val retentionHint: SampleRetentionHint,
    val reasons: Set<SamplePruningReason>
) {
    val recaptureSuggested: Boolean
        get() = retentionHint == SampleRetentionHint.RECAPTURE_SUGGESTED

    val removeSuggested: Boolean
        get() = retentionHint == SampleRetentionHint.REMOVE_SUGGESTED

    val isPruningCandidate: Boolean
        get() = recaptureSuggested || removeSuggested
}

internal data class TeachSamplePruningSuggestions(
    val suggestionsByObservationId: Map<String, SamplePruningSuggestion>,
    val keepSampleIds: List<String>,
    val recaptureSuggestedIds: List<String>,
    val removeSuggestedIds: List<String>,
    val pruningCandidateIds: List<String>
) {
    val keepSampleCount: Int
        get() = keepSampleIds.size

    val pruningCandidateCount: Int
        get() = pruningCandidateIds.size

    val hasPruningCandidates: Boolean
        get() = pruningCandidateIds.isNotEmpty()
}

internal fun derivePruningSuggestions(
    capturedSamples: List<TeachPersonCapturedSample>,
    bestSampleSelection: BestSampleSelection,
    qualityGateResult: TeachQualityGateResult
): TeachSamplePruningSuggestions {
    if (capturedSamples.isEmpty()) {
        return TeachSamplePruningSuggestions(
            suggestionsByObservationId = emptyMap(),
            keepSampleIds = emptyList(),
            recaptureSuggestedIds = emptyList(),
            removeSuggestedIds = emptyList(),
            pruningCandidateIds = emptyList()
        )
    }

    val rankedOrder = bestSampleSelection.rankedSampleObservationIds
        .withIndex()
        .associate { indexed -> indexed.value to indexed.index }
    val preferredBestScore = capturedSamples
        .firstOrNull { sample -> sample.observationId == bestSampleSelection.bestSampleId }
        ?.scoredQuality
        ?.score

    val orderedSamples = capturedSamples.sortedWith(
        compareBy<TeachPersonCapturedSample> { sample -> rankedOrder[sample.observationId] ?: Int.MAX_VALUE }
            .thenBy { sample -> sample.observationId }
    )
    val suggestions = orderedSamples.map { sample ->
        sample.toPruningSuggestion(
            bestSampleSelection = bestSampleSelection,
            preferredBestScore = preferredBestScore,
            qualityGateResult = qualityGateResult
        )
    }

    val suggestionsById = suggestions.associateBy { suggestion -> suggestion.observationId }
    val keepSampleIds = suggestions
        .asSequence()
        .filter { suggestion -> suggestion.retentionHint == SampleRetentionHint.KEEP }
        .map { suggestion -> suggestion.observationId }
        .toList()
    val recaptureSuggestedIds = suggestions
        .asSequence()
        .filter { suggestion -> suggestion.recaptureSuggested }
        .map { suggestion -> suggestion.observationId }
        .toList()
    val removeSuggestedIds = suggestions
        .asSequence()
        .filter { suggestion -> suggestion.removeSuggested }
        .map { suggestion -> suggestion.observationId }
        .toList()
    val pruningCandidateIds = suggestions
        .asSequence()
        .filter { suggestion -> suggestion.isPruningCandidate }
        .map { suggestion -> suggestion.observationId }
        .toList()

    return TeachSamplePruningSuggestions(
        suggestionsByObservationId = suggestionsById,
        keepSampleIds = keepSampleIds,
        recaptureSuggestedIds = recaptureSuggestedIds,
        removeSuggestedIds = removeSuggestedIds,
        pruningCandidateIds = pruningCandidateIds
    )
}

private fun TeachPersonCapturedSample.toPruningSuggestion(
    bestSampleSelection: BestSampleSelection,
    preferredBestScore: Int?,
    qualityGateResult: TeachQualityGateResult
): SamplePruningSuggestion {
    val isPreferredSample = bestSampleSelection.preferredSampleIds.contains(observationId)
    val isMissingFaceCrop = faceCropUri.isNullOrBlank()
    val isLowQualityLevel = scoredQuality.level == SampleQualityLevel.LOW
    val isQualifiedForGate = isQualifiedForHardGate()
    val hasQualifiedCoverage = qualityGateResult.qualifiedSampleCount >=
        qualityGateResult.requiredQualifiedSampleCount
    val isWeakerThanPreferred = !isPreferredSample &&
        preferredBestScore != null &&
        scoredQuality.score <= (preferredBestScore - 15)

    val reasons = linkedSetOf<SamplePruningReason>()
    if (isPreferredSample) {
        reasons += SamplePruningReason.PREFERRED_SAMPLE
    }
    if (isMissingFaceCrop) {
        reasons += SamplePruningReason.MISSING_FACE_CROP
    }
    if (isLowQualityLevel) {
        reasons += SamplePruningReason.LOW_QUALITY_LEVEL
    }
    if (hasSoftWarning) {
        reasons += SamplePruningReason.HAS_SOFT_WARNING
    }
    if (!isQualifiedForGate) {
        reasons += SamplePruningReason.NOT_QUALIFIED_FOR_HARD_GATE
    }
    if (hasQualifiedCoverage && isWeakerThanPreferred) {
        reasons += SamplePruningReason.WEAKER_THAN_PREFERRED_SAMPLE
    }

    val retentionHint = when {
        isPreferredSample -> SampleRetentionHint.KEEP
        isMissingFaceCrop || isLowQualityLevel -> SampleRetentionHint.REMOVE_SUGGESTED
        !isQualifiedForGate || (hasQualifiedCoverage && isWeakerThanPreferred) ->
            SampleRetentionHint.RECAPTURE_SUGGESTED
        hasSoftWarning -> SampleRetentionHint.RECAPTURE_SUGGESTED
        else -> SampleRetentionHint.KEEP
    }

    return SamplePruningSuggestion(
        observationId = observationId,
        retentionHint = retentionHint,
        reasons = reasons
    )
}
