package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.AfterEffectType
import com.aipet.brain.brain.b2.domain.AnimationFamily
import com.aipet.brain.brain.b2.domain.BehaviorAfterEffect
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.domain.BubblePolicy
import com.aipet.brain.brain.b2.domain.Interruptibility
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.pet.PetCondition
import java.util.UUID

/**
 * Maps a winning [PetIntention] to a concrete [BehaviorPlan].
 *
 * Considers the current [WorkingContext] when selecting animation family,
 * duration, bubble policy, and after-effects.
 */
class BehaviorPlanner {

    fun plan(intention: PetIntention, ctx: WorkingContext): BehaviorPlan {
        val id = "${intention.name.lowercase()}_${ctx.snapshotAtMs}"
        return when (intention) {
            PetIntention.REST -> planRest(id, ctx)
            PetIntention.DOZE -> planDoze(id, ctx)
            PetIntention.SEEK_ATTENTION -> planSeekAttention(id, ctx)
            PetIntention.SEEK_COMFORT -> planSeekComfort(id, ctx)
            PetIntention.INVITE_PLAY -> planInvitePlay(id, ctx)
            PetIntention.PLAY -> planPlay(id, ctx)
            PetIntention.RESPOND_TO_USER -> planRespondToUser(id, ctx)
            PetIntention.OBSERVE -> planObserve(id, ctx)
            PetIntention.INVESTIGATE -> planInvestigate(id, ctx)
            PetIntention.REQUEST_FOOD -> planRequestFood(id, ctx)
            PetIntention.SELF_SOOTHE -> planSelfSoothe(id, ctx)
            PetIntention.STAY_NEAR -> planStayNear(id, ctx)
            PetIntention.WITHDRAW -> planWithdraw(id, ctx)
            PetIntention.LISTEN -> planListen(id, ctx)
            PetIntention.STARTLE_RECOVER -> planStartleRecover(id, ctx)
            PetIntention.CELEBRATE -> planCelebrate(id, ctx)
            PetIntention.RECOVER -> planRecover(id, ctx)
        }
    }

    // ─── Individual plan builders ────────────────────────────────────────────

    private fun planRest(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.REST,
        animationFamily = AnimationFamily.IDLE_CALM,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.FREE,
        expectedDurationMs = 0L,
        interruptPriority = 0,
        afterEffects = emptyList(),
        debugLabel = "rest/calm_idle"
    )

    private fun planDoze(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.DOZE,
        animationFamily = AnimationFamily.IDLE_SLEEPY,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.HIGH_PRIORITY_ONLY,
        expectedDurationMs = 0L,
        interruptPriority = 0,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_DROWSINESS_DELTA, 0.05f, "doze_deepens_drowsiness")
        ),
        debugLabel = "doze/sleepy_idle"
    )

    private fun planSeekAttention(id: String, ctx: WorkingContext): BehaviorPlan {
        // Appearance varies with emotional state
        val family = when {
            ctx.emotionMomentum.moodNeedy > 0.5f -> AnimationFamily.IDLE_LONELY
            ctx.emotionMomentum.moodWarm > 0.4f -> AnimationFamily.SOCIAL_WARM
            else -> AnimationFamily.IDLE_CURIOUS
        }
        return BehaviorPlan(
            id = id,
            intention = PetIntention.SEEK_ATTENTION,
            animationFamily = family,
            bubblePolicy = BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN,
            interruptibility = Interruptibility.NORMAL,
            expectedDurationMs = 4_000L,
            interruptPriority = 2,
            afterEffects = listOf(
                BehaviorAfterEffect(AfterEffectType.RECORD_SEEK_ATTENTION_COOLDOWN, 0f, "seek_attention_cd"),
                BehaviorAfterEffect(AfterEffectType.APPLY_NEEDINESS_DELTA, -0.1f, "need_expressed")
            ),
            debugLabel = "seek_attention/${family.name.lowercase()}"
        )
    }

    private fun planSeekComfort(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.SEEK_COMFORT,
        animationFamily = AnimationFamily.IDLE_LONELY,
        bubblePolicy = BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 5_000L,
        interruptPriority = 2,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_NEEDINESS_DELTA, -0.05f, "comfort_seeking"),
            BehaviorAfterEffect(AfterEffectType.RECORD_SEEK_ATTENTION_COOLDOWN, 0f, "comfort_cd")
        ),
        debugLabel = "seek_comfort/lonely"
    )

    private fun planInvitePlay(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.INVITE_PLAY,
        animationFamily = AnimationFamily.PLAY_INVITATION,
        bubblePolicy = BubblePolicy.FORCE_SHOW,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 6_000L,
        interruptPriority = 3,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.RECORD_INVITATION_OUTCOME, 0f, "invitation_tracked"),
            BehaviorAfterEffect(AfterEffectType.RECORD_BUBBLE_COOLDOWN, 0f, "bubble_cd")
        ),
        debugLabel = "invite_play/invitation"
    )

    private fun planPlay(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.PLAY,
        animationFamily = AnimationFamily.PLAY_ACTIVE,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.HIGH_PRIORITY_ONLY,
        expectedDurationMs = 0L,
        interruptPriority = 4,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.2f, "play_joy"),
            BehaviorAfterEffect(AfterEffectType.APPLY_DROWSINESS_DELTA, 0.1f, "play_tires")
        ),
        debugLabel = "play/active"
    )

    private fun planRespondToUser(id: String, ctx: WorkingContext): BehaviorPlan {
        val isLongPress = ctx.perception.touchContext.recentLongPress
        val isSpam = ctx.perception.touchContext.spamLikelihood > 0.6f
        val family = when {
            isLongPress && !isSpam -> AnimationFamily.REACT_COMFORT
            isSpam -> AnimationFamily.REACT_IRRITATED
            ctx.emotionMomentum.joy > 0.5f -> AnimationFamily.REACT_HAPPY
            ctx.emotionMomentum.moodPlayful > 0.4f -> AnimationFamily.SOCIAL_PLAYFUL
            else -> AnimationFamily.REACT_HAPPY
        }
        val afterEffects = mutableListOf(
            BehaviorAfterEffect(AfterEffectType.RECORD_TOUCH_COOLDOWN, 0f, "touch_cd")
        )
        if (isLongPress) {
            afterEffects += BehaviorAfterEffect(AfterEffectType.RECORD_LONG_PRESS_COOLDOWN, 0f, "long_press_cd")
            afterEffects += BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.15f, "cuddle_warmth")
        } else if (isSpam) {
            afterEffects += BehaviorAfterEffect(AfterEffectType.APPLY_IRRITATION_DELTA, 0.12f, "tap_spam_irritation")
        } else {
            afterEffects += BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.08f, "tap_joy")
        }
        return BehaviorPlan(
            id = id,
            intention = PetIntention.RESPOND_TO_USER,
            animationFamily = family,
            bubblePolicy = BubblePolicy.NONE,
            interruptibility = Interruptibility.HIGH_PRIORITY_ONLY,
            expectedDurationMs = if (isLongPress) 2_000L else 800L,
            interruptPriority = 5,
            afterEffects = afterEffects,
            debugLabel = "respond_user/${family.name.lowercase()}"
        )
    }

    private fun planObserve(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.OBSERVE,
        animationFamily = AnimationFamily.IDLE_CURIOUS,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.FREE,
        expectedDurationMs = 3_000L,
        interruptPriority = 1,
        afterEffects = emptyList(),
        debugLabel = "observe/curious_idle"
    )

    private fun planInvestigate(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.INVESTIGATE,
        animationFamily = AnimationFamily.IDLE_CURIOUS,
        bubblePolicy = BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 3_000L,
        interruptPriority = 2,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.05f, "investigation_novelty")
        ),
        debugLabel = "investigate/novelty"
    )

    private fun planRequestFood(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.REQUEST_FOOD,
        animationFamily = AnimationFamily.IDLE_LONELY,
        bubblePolicy = BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 4_000L,
        interruptPriority = 3,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.RECORD_SEEK_ATTENTION_COOLDOWN, 0f, "food_request_cd")
        ),
        debugLabel = "request_food/hungry"
    )

    private fun planSelfSoothe(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.SELF_SOOTHE,
        animationFamily = AnimationFamily.SELF_SOOTHE,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 4_000L,
        interruptPriority = 2,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_IRRITATION_DELTA, -0.15f, "self_soothe_calms"),
            BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.05f, "self_soothe_comfort")
        ),
        debugLabel = "self_soothe/calm"
    )

    private fun planStayNear(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.STAY_NEAR,
        animationFamily = AnimationFamily.SOCIAL_WARM,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.FREE,
        expectedDurationMs = 0L,
        interruptPriority = 1,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_WARMTH_DELTA, 0.02f, "passive_warmth")
        ),
        debugLabel = "stay_near/companion"
    )

    private fun planWithdraw(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.WITHDRAW,
        animationFamily = AnimationFamily.WITHDRAW_INWARD,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.HIGH_PRIORITY_ONLY,
        expectedDurationMs = 5_000L,
        interruptPriority = 2,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_IRRITATION_DELTA, -0.05f, "withdrawal_cools"),
            BehaviorAfterEffect(AfterEffectType.APPLY_DROWSINESS_DELTA, 0.05f, "withdrawal_drains")
        ),
        debugLabel = "withdraw/inward"
    )

    private fun planListen(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.LISTEN,
        animationFamily = AnimationFamily.IDLE_CURIOUS,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 2_500L,
        interruptPriority = 3,
        afterEffects = emptyList(),
        debugLabel = "listen/orient"
    )

    private fun planStartleRecover(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.STARTLE_RECOVER,
        animationFamily = AnimationFamily.REACT_STARTLED,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.LOCKED,
        expectedDurationMs = 1_500L,
        interruptPriority = 7,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_IRRITATION_DELTA, 0.08f, "startle_stress")
        ),
        debugLabel = "startle_recover/startled"
    )

    private fun planCelebrate(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.CELEBRATE,
        animationFamily = AnimationFamily.REACT_HAPPY,
        bubblePolicy = BubblePolicy.SHOW_IF_NOT_ON_COOLDOWN,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 2_000L,
        interruptPriority = 4,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.2f, "celebration_joy"),
            BehaviorAfterEffect(AfterEffectType.RECORD_BUBBLE_COOLDOWN, 0f, "bubble_cd")
        ),
        debugLabel = "celebrate/happy"
    )

    private fun planRecover(id: String, ctx: WorkingContext) = BehaviorPlan(
        id = id,
        intention = PetIntention.RECOVER,
        animationFamily = AnimationFamily.IDLE_CALM,
        bubblePolicy = BubblePolicy.NONE,
        interruptibility = Interruptibility.NORMAL,
        expectedDurationMs = 3_000L,
        interruptPriority = 2,
        afterEffects = listOf(
            BehaviorAfterEffect(AfterEffectType.APPLY_IRRITATION_DELTA, -0.1f, "recovery_calms"),
            BehaviorAfterEffect(AfterEffectType.APPLY_JOY_DELTA, 0.05f, "recovery_lift")
        ),
        debugLabel = "recover/calm"
    )
}
