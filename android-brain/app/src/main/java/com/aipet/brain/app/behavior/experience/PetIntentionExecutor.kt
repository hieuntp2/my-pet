package com.aipet.brain.app.behavior.experience

/**
 * Executes behavior experience bundles with interruption, cooldown, and anti-repeat policies.
 *
 * The executor is intentionally lightweight: it does not plan behavior, it only governs
 * runtime execution feel and suppression decisions.
 */
class PetIntentionExecutor(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private data class ActiveExecution(
        val bundle: PetExperienceBundle,
        val startedAtMs: Long,
        val reactionEndsAtMs: Long,
        val settleEndsAtMs: Long,
        val cooldownEndsAtMs: Long
    )

    private val audioCooldownUntilByKey = mutableMapOf<String, Long>()
    private val talkCooldownUntilByKey = mutableMapOf<String, Long>()
    private val antiRepeatUntilByKey = mutableMapOf<String, Long>()

    private var activeExecution: ActiveExecution? = null
    private var lastDebugState: BehaviorExperienceDebugState = BehaviorExperienceDebugState.EMPTY

    fun execute(bundle: PetExperienceBundle, nowMs: Long = nowProvider()): BehaviorExperienceExecution {
        cleanupExpired(nowMs)

        val currentActive = activeExecution
        var accepted = false
        var rejectionReason: ExperienceRejectionReason? = null

        val duplicateActive = currentActive != null &&
            currentActive.bundle.antiRepeatKey == bundle.antiRepeatKey &&
            nowMs < currentActive.reactionEndsAtMs
        if (duplicateActive) {
            rejectionReason = ExperienceRejectionReason.DUPLICATE_ACTIVE_REACTION
        } else {
            val antiRepeatUntil = antiRepeatUntilByKey[bundle.antiRepeatKey] ?: 0L
            if (nowMs < antiRepeatUntil) {
                rejectionReason = ExperienceRejectionReason.ANTI_REPEAT_WINDOW
            } else if (!canInterrupt(currentActive, bundle)) {
                rejectionReason = rejectionReasonForInterrupt(currentActive, bundle)
            } else {
                accepted = true
            }
        }

        if (accepted) {
            activeExecution = ActiveExecution(
                bundle = bundle,
                startedAtMs = nowMs,
                reactionEndsAtMs = nowMs + bundle.visualDirective.holdMs,
                settleEndsAtMs = nowMs + bundle.visualDirective.holdMs + bundle.visualDirective.settleMs,
                cooldownEndsAtMs = nowMs + bundle.visualDirective.holdMs + bundle.visualDirective.settleMs + bundle.cooldownMs
            )
            antiRepeatUntilByKey[bundle.antiRepeatKey] = nowMs + bundle.minimumRepeatIntervalMs
        }

        val running = activeExecution ?: currentActive
        val phase = when {
            accepted -> ExperienceExecutionPhase.PREPARING_REACTION
            running == null -> ExperienceExecutionPhase.IDLE_BASE
            nowMs < running.reactionEndsAtMs -> ExperienceExecutionPhase.PRIMARY_REACTION
            nowMs < running.settleEndsAtMs -> ExperienceExecutionPhase.SETTLING
            else -> ExperienceExecutionPhase.COOLDOWN
        }

        val activeVisualIntent = (running ?: activeExecution)?.bundle?.visualDirective?.intent
            ?: bundle.visualDirective.intent

        val visualResult = if (accepted) {
            ChannelExecutionResult(
                decision = ChannelDecision.EXECUTED,
                reason = "bundle_accepted"
            )
        } else {
            ChannelExecutionResult(
                decision = ChannelDecision.SUPPRESSED,
                reason = rejectionReason?.name?.lowercase() ?: "suppressed"
            )
        }

        val (audioDirective, audioResult) = resolveAudio(
            accepted = accepted,
            bundle = bundle,
            nowMs = nowMs,
            rejectionReason = rejectionReason
        )
        val (talkDirective, talkResult) = resolveTalk(
            accepted = accepted,
            bundle = bundle,
            nowMs = nowMs,
            rejectionReason = rejectionReason
        )

        val activeCooldownKeys = buildList {
            audioCooldownUntilByKey
                .filterValues { it > nowMs }
                .keys
                .sorted()
                .forEach { add("audio:$it") }
            talkCooldownUntilByKey
                .filterValues { it > nowMs }
                .keys
                .sorted()
                .forEach { add("talk:$it") }
        }
        val activeAntiRepeatKeys = antiRepeatUntilByKey
            .filterValues { it > nowMs }
            .keys
            .sorted()

        val resolvedPhase = if (accepted) {
            ExperienceExecutionPhase.PRIMARY_REACTION
        } else {
            phase
        }

        val debugState = BehaviorExperienceDebugState(
            sourcePlanId = bundle.sourcePlanId,
            sourceIntention = bundle.sourceIntention,
            mappedVisualIntent = bundle.visualDirective.intent,
            mappedAudioCategory = bundle.audioDirective?.category,
            mappedTalkDedupeKey = bundle.talkDirective?.dedupeKey,
            phase = resolvedPhase,
            accepted = accepted,
            rejectionReason = rejectionReason,
            visualResult = visualResult,
            audioResult = audioResult,
            talkResult = talkResult,
            activeCooldownKeys = activeCooldownKeys,
            activeAntiRepeatKeys = activeAntiRepeatKeys,
            updatedAtMs = nowMs,
            debugLabel = bundle.debugLabel
        )
        lastDebugState = debugState

        return BehaviorExperienceExecution(
            activeVisualIntent = activeVisualIntent,
            talkDirective = talkDirective,
            audioDirective = audioDirective,
            debugState = debugState
        )
    }

    fun currentDebugState(): BehaviorExperienceDebugState = lastDebugState

    private fun canInterrupt(
        currentActive: ActiveExecution?,
        candidateBundle: PetExperienceBundle
    ): Boolean {
        currentActive ?: return true
        if (currentActive.bundle.priority.rank == ExperienceExecutionPriority.IDLE.rank) {
            return true
        }
        return when (currentActive.bundle.interruptPolicy) {
            ExperienceInterruptPolicy.FREE -> true
            ExperienceInterruptPolicy.NORMAL -> {
                candidateBundle.priority.rank >= currentActive.bundle.priority.rank
            }
            ExperienceInterruptPolicy.HIGH_PRIORITY_ONLY -> {
                candidateBundle.priority.rank > currentActive.bundle.priority.rank
            }
            ExperienceInterruptPolicy.LOCKED -> false
        }
    }

    private fun rejectionReasonForInterrupt(
        currentActive: ActiveExecution?,
        candidateBundle: PetExperienceBundle
    ): ExperienceRejectionReason {
        currentActive ?: return ExperienceRejectionReason.LOWER_PRIORITY_SUPPRESSED
        return when (currentActive.bundle.interruptPolicy) {
            ExperienceInterruptPolicy.LOCKED -> ExperienceRejectionReason.ACTIVE_LOCKED
            ExperienceInterruptPolicy.HIGH_PRIORITY_ONLY -> ExperienceRejectionReason.ACTIVE_HIGH_PRIORITY_ONLY
            ExperienceInterruptPolicy.NORMAL -> {
                if (candidateBundle.priority.rank < currentActive.bundle.priority.rank) {
                    ExperienceRejectionReason.LOWER_PRIORITY_SUPPRESSED
                } else {
                    ExperienceRejectionReason.ACTIVE_HIGH_PRIORITY_ONLY
                }
            }
            ExperienceInterruptPolicy.FREE -> ExperienceRejectionReason.LOWER_PRIORITY_SUPPRESSED
        }
    }

    private fun resolveAudio(
        accepted: Boolean,
        bundle: PetExperienceBundle,
        nowMs: Long,
        rejectionReason: ExperienceRejectionReason?
    ): Pair<AudioDirective?, ChannelExecutionResult> {
        val directive = bundle.audioDirective
        if (directive == null) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SKIPPED,
                reason = "no_audio_directive"
            )
        }
        if (!accepted) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SUPPRESSED,
                reason = rejectionReason?.name?.lowercase() ?: "bundle_rejected"
            )
        }

        val cooldownUntil = audioCooldownUntilByKey[directive.cooldownKey] ?: 0L
        if (nowMs < cooldownUntil) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SUPPRESSED,
                reason = "cooldown_active_${cooldownUntil - nowMs}ms"
            )
        }

        audioCooldownUntilByKey[directive.cooldownKey] = nowMs + directive.minIntervalMs
        return directive to ChannelExecutionResult(
            decision = ChannelDecision.EXECUTED,
            reason = "audio_dispatched"
        )
    }

    private fun resolveTalk(
        accepted: Boolean,
        bundle: PetExperienceBundle,
        nowMs: Long,
        rejectionReason: ExperienceRejectionReason?
    ): Pair<TalkDirective?, ChannelExecutionResult> {
        val directive = bundle.talkDirective
        if (directive == null) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SKIPPED,
                reason = "no_talk_directive"
            )
        }
        if (!accepted) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SUPPRESSED,
                reason = rejectionReason?.name?.lowercase() ?: "bundle_rejected"
            )
        }

        val cooldownUntil = talkCooldownUntilByKey[directive.dedupeKey] ?: 0L
        if (nowMs < cooldownUntil) {
            return null to ChannelExecutionResult(
                decision = ChannelDecision.SUPPRESSED,
                reason = "dedupe_active_${cooldownUntil - nowMs}ms"
            )
        }

        talkCooldownUntilByKey[directive.dedupeKey] = nowMs + directive.minIntervalMs
        return directive to ChannelExecutionResult(
            decision = ChannelDecision.EXECUTED,
            reason = "talk_dispatched"
        )
    }

    private fun cleanupExpired(nowMs: Long) {
        val active = activeExecution
        if (active != null && nowMs >= active.cooldownEndsAtMs) {
            activeExecution = null
        }
        audioCooldownUntilByKey.entries.removeAll { it.value <= nowMs }
        talkCooldownUntilByKey.entries.removeAll { it.value <= nowMs }
        antiRepeatUntilByKey.entries.removeAll { it.value <= nowMs }
    }
}
