package com.aipet.brain.app.perception

internal fun inspectUnknownObjectPromptSuppression(
    canonicalLabel: String?,
    suppressedUntilByLabel: Map<String, Long>,
    nowMs: Long = System.currentTimeMillis()
): UnknownObjectPromptSuppressionDebugState {
    val normalizedLabel = canonicalLabel?.trim().orEmpty()
    if (normalizedLabel.isBlank()) {
        return UnknownObjectPromptSuppressionDebugState()
    }
    val suppressedUntilMs = suppressedUntilByLabel[normalizedLabel]
    val remainingMs = (suppressedUntilMs ?: 0L) - nowMs
    val isSuppressed = remainingMs > 0L
    return UnknownObjectPromptSuppressionDebugState(
        canonicalLabel = normalizedLabel,
        isSuppressed = isSuppressed,
        suppressedUntilMs = suppressedUntilMs?.takeIf { it > 0L },
        remainingMs = remainingMs.coerceAtLeast(0L)
    )
}
