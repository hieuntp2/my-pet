package com.aipet.brain.brain.b2.domain

/**
 * The type of animation family the behavior engine requests.
 */
enum class AnimationFamily {
    IDLE_CALM,
    IDLE_CURIOUS,
    IDLE_SLEEPY,
    IDLE_LONELY,
    SOCIAL_WARM,
    SOCIAL_PLAYFUL,
    SOCIAL_EXCITED,
    REACT_HAPPY,
    REACT_STARTLED,
    REACT_IRRITATED,
    REACT_COMFORT,
    PLAY_ACTIVE,
    PLAY_INVITATION,
    WITHDRAW_INWARD,
    SELF_SOOTHE,
    NONE
}

/**
 * Policy for how a talk bubble should be handled in this plan.
 */
enum class BubblePolicy {
    NONE,
    SHOW_IF_NOT_ON_COOLDOWN,
    FORCE_SHOW,
    CLEAR
}

/**
 * Policy controlling interruption eligibility during execution.
 */
enum class Interruptibility {
    /** Cannot be interrupted. */
    LOCKED,
    /** Can be interrupted only by high-priority events. */
    HIGH_PRIORITY_ONLY,
    /** Can be interrupted by any meaningful event. */
    NORMAL,
    /** Always interruptible. */
    FREE
}

/**
 * What happens after this behavior plan completes.
 */
data class BehaviorAfterEffect(
    val type: AfterEffectType,
    val value: Float = 0f,
    val description: String = ""
)

enum class AfterEffectType {
    APPLY_JOY_DELTA,
    APPLY_IRRITATION_DELTA,
    APPLY_DROWSINESS_DELTA,
    APPLY_NEEDINESS_DELTA,
    APPLY_WARMTH_DELTA,
    RECORD_INVITATION_OUTCOME,
    RECORD_TOUCH_COOLDOWN,
    RECORD_LONG_PRESS_COOLDOWN,
    RECORD_BUBBLE_COOLDOWN,
    RECORD_SEEK_ATTENTION_COOLDOWN,
    INCREMENT_INVITATION_IGNORED,
    RESET_INVITATION_IGNORED
}

/**
 * A full behavior plan produced by the BehaviorPlanner.
 * The execution layer reads this to drive animation, bubbles, audio, and games.
 */
data class BehaviorPlan(
    val id: String,
    val intention: PetIntention,
    val animationFamily: AnimationFamily,
    val bubblePolicy: BubblePolicy,
    val interruptibility: Interruptibility,
    /** Expected duration of the plan in ms (0 = indefinite). */
    val expectedDurationMs: Long,
    /** Higher = harder to interrupt with a competing plan. */
    val interruptPriority: Int,
    val afterEffects: List<BehaviorAfterEffect>,
    val debugLabel: String
) {
    companion object {
        /** Fallback plan when nothing better is selected. */
        val IDLE_DEFAULT = BehaviorPlan(
            id = "idle_default",
            intention = PetIntention.REST,
            animationFamily = AnimationFamily.IDLE_CALM,
            bubblePolicy = BubblePolicy.NONE,
            interruptibility = Interruptibility.FREE,
            expectedDurationMs = 0L,
            interruptPriority = 0,
            afterEffects = emptyList(),
            debugLabel = "idle/default"
        )
    }
}
