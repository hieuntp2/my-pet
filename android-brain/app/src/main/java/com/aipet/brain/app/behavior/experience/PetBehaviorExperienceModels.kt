package com.aipet.brain.app.behavior.experience

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

enum class ExperienceExecutionPriority(
    val rank: Int
) {
    CRITICAL(4),
    HIGH(3),
    NORMAL(2),
    LOW(1),
    IDLE(0)
}

enum class ExperienceInterruptPolicy {
    LOCKED,
    HIGH_PRIORITY_ONLY,
    NORMAL,
    FREE
}

enum class ExperienceExecutionPhase {
    IDLE_BASE,
    PREPARING_REACTION,
    PRIMARY_REACTION,
    SETTLING,
    COOLDOWN
}

enum class ExperienceRejectionReason {
    ACTIVE_LOCKED,
    ACTIVE_HIGH_PRIORITY_ONLY,
    LOWER_PRIORITY_SUPPRESSED,
    ANTI_REPEAT_WINDOW,
    DUPLICATE_ACTIVE_REACTION
}

enum class ChannelDecision {
    EXECUTED,
    SUPPRESSED,
    SKIPPED
}

data class VisualDirective(
    val intent: PixelPetAvatarIntent,
    val holdMs: Long,
    val settleMs: Long = 420L
)

data class AudioDirective(
    val category: AudioCategory,
    val cooldownKey: String,
    val minIntervalMs: Long = 1_600L
)

data class TalkDirective(
    val message: String,
    val dedupeKey: String,
    val maxDisplayMs: Long = 3_200L,
    val minIntervalMs: Long = 6_000L,
    val issuedAtMs: Long
)

data class PetExperienceBundle(
    val sourcePlanId: String,
    val sourceIntention: PetIntention,
    val debugLabel: String,
    val visualDirective: VisualDirective,
    val audioDirective: AudioDirective?,
    val talkDirective: TalkDirective?,
    val priority: ExperienceExecutionPriority,
    val interruptPolicy: ExperienceInterruptPolicy,
    val antiRepeatKey: String,
    val minimumRepeatIntervalMs: Long,
    val cooldownMs: Long
)

data class ChannelExecutionResult(
    val decision: ChannelDecision,
    val reason: String? = null
)

data class BehaviorExperienceDebugState(
    val sourcePlanId: String,
    val sourceIntention: PetIntention,
    val mappedVisualIntent: PixelPetAvatarIntent,
    val mappedAudioCategory: AudioCategory?,
    val mappedTalkDedupeKey: String?,
    val phase: ExperienceExecutionPhase,
    val accepted: Boolean,
    val rejectionReason: ExperienceRejectionReason?,
    val visualResult: ChannelExecutionResult,
    val audioResult: ChannelExecutionResult,
    val talkResult: ChannelExecutionResult,
    val activeCooldownKeys: List<String>,
    val activeAntiRepeatKeys: List<String>,
    val updatedAtMs: Long,
    val debugLabel: String
) {
    companion object {
        val EMPTY = BehaviorExperienceDebugState(
            sourcePlanId = "none",
            sourceIntention = PetIntention.REST,
            mappedVisualIntent = PixelPetAvatarIntent.NEUTRAL,
            mappedAudioCategory = null,
            mappedTalkDedupeKey = null,
            phase = ExperienceExecutionPhase.IDLE_BASE,
            accepted = false,
            rejectionReason = null,
            visualResult = ChannelExecutionResult(decision = ChannelDecision.SKIPPED, reason = "idle"),
            audioResult = ChannelExecutionResult(decision = ChannelDecision.SKIPPED, reason = "idle"),
            talkResult = ChannelExecutionResult(decision = ChannelDecision.SKIPPED, reason = "idle"),
            activeCooldownKeys = emptyList(),
            activeAntiRepeatKeys = emptyList(),
            updatedAtMs = 0L,
            debugLabel = "idle"
        )
    }
}

data class BehaviorExperienceExecution(
    val activeVisualIntent: PixelPetAvatarIntent?,
    val talkDirective: TalkDirective?,
    val audioDirective: AudioDirective?,
    val debugState: BehaviorExperienceDebugState
)
