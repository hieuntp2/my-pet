package com.aipet.brain.app.ui.persons

internal enum class TeachSessionCompletionStatus {
    BLOCKED,
    READY_TO_COMPLETE,
    COMPLETED
}

internal data class TeachSessionCompletionState(
    val status: TeachSessionCompletionStatus,
    val isReadyToComplete: Boolean,
    val isCompleted: Boolean,
    val completionBlockedReason: String?,
    val completedAtMs: Long?
)

internal sealed interface TeachSessionCompletionConfirmationResult {
    data class Confirmed(
        val completionState: TeachSessionCompletionState
    ) : TeachSessionCompletionConfirmationResult

    data class Blocked(
        val completionState: TeachSessionCompletionState
    ) : TeachSessionCompletionConfirmationResult

    data class AlreadyCompleted(
        val completionState: TeachSessionCompletionState
    ) : TeachSessionCompletionConfirmationResult
}

internal fun canCompleteTeachSession(
    qualityGateResult: TeachQualityGateResult,
    sessionSummary: TeachSessionSummary
): Boolean {
    return qualityGateResult.canSaveTeachPerson && sessionSummary.canSave
}

internal fun evaluateTeachSessionCompletionState(
    qualityGateResult: TeachQualityGateResult,
    sessionSummary: TeachSessionSummary,
    completionConfirmedAtMs: Long?
): TeachSessionCompletionState {
    if (completionConfirmedAtMs != null) {
        return TeachSessionCompletionState(
            status = TeachSessionCompletionStatus.COMPLETED,
            isReadyToComplete = true,
            isCompleted = true,
            completionBlockedReason = null,
            completedAtMs = completionConfirmedAtMs
        )
    }

    val isReadyToComplete = canCompleteTeachSession(
        qualityGateResult = qualityGateResult,
        sessionSummary = sessionSummary
    )
    if (isReadyToComplete) {
        return TeachSessionCompletionState(
            status = TeachSessionCompletionStatus.READY_TO_COMPLETE,
            isReadyToComplete = true,
            isCompleted = false,
            completionBlockedReason = null,
            completedAtMs = null
        )
    }

    return TeachSessionCompletionState(
        status = TeachSessionCompletionStatus.BLOCKED,
        isReadyToComplete = false,
        isCompleted = false,
        completionBlockedReason = sessionSummary.blockedReason
            ?: qualityGateResult.saveBlockedReason
            ?: "Teach session is not ready to complete.",
        completedAtMs = null
    )
}

internal fun confirmTeachSessionCompletion(
    qualityGateResult: TeachQualityGateResult,
    sessionSummary: TeachSessionSummary,
    completionConfirmedAtMs: Long?,
    nowMs: Long
): TeachSessionCompletionConfirmationResult {
    val currentState = evaluateTeachSessionCompletionState(
        qualityGateResult = qualityGateResult,
        sessionSummary = sessionSummary,
        completionConfirmedAtMs = completionConfirmedAtMs
    )
    if (currentState.isCompleted) {
        return TeachSessionCompletionConfirmationResult.AlreadyCompleted(currentState)
    }
    if (!currentState.isReadyToComplete) {
        return TeachSessionCompletionConfirmationResult.Blocked(currentState)
    }

    return TeachSessionCompletionConfirmationResult.Confirmed(
        TeachSessionCompletionState(
            status = TeachSessionCompletionStatus.COMPLETED,
            isReadyToComplete = true,
            isCompleted = true,
            completionBlockedReason = null,
            completedAtMs = nowMs
        )
    )
}
