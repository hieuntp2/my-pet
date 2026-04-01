package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.CooldownDomain
import com.aipet.brain.brain.b2.domain.IntentionCandidate
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.attention.AttentionMode
import com.aipet.brain.brain.attention.FocusTargetType
import com.aipet.brain.brain.evolution.DayPhase
import com.aipet.brain.brain.evolution.EvolutionContext
import com.aipet.brain.brain.evolution.ReunionType
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.state.BrainState

/**
 * Generates scored [IntentionCandidate] instances from a [WorkingContext].
 *
 * Scoring formula per candidate:
 *   score = base_weight
 *         + need_pressure
 *         + emotion_bias
 *         + relationship_modifier
 *         + perception_trigger_bonus
 *         + time_context_bonus
 *         - cooldown_penalty
 *         - fatigue_penalty
 *         - anti_repeat_penalty
 *
 * Gating rules are applied after scoring to block impossible candidates.
 */
class IntentionScorer {

    fun score(ctx: WorkingContext): List<IntentionCandidate> {
        val candidates = PetIntention.entries.map { intention ->
            buildBaseCandidate(intention, ctx)
        }
        return candidates.map { candidate ->
            applyGates(candidate, ctx)
        }
    }

    // ─── Base weight per intention ───────────────────────────────────────────

    private fun baseWeight(intention: PetIntention): Float = when (intention) {
        PetIntention.REST -> 0.6f
        PetIntention.DOZE -> 0.4f
        PetIntention.SEEK_ATTENTION -> 0.5f
        PetIntention.SEEK_COMFORT -> 0.4f
        PetIntention.INVITE_PLAY -> 0.35f
        PetIntention.PLAY -> 0.3f
        PetIntention.RESPOND_TO_USER -> 0.8f
        PetIntention.OBSERVE -> 0.45f
        PetIntention.INVESTIGATE -> 0.40f
        PetIntention.REQUEST_FOOD -> 0.3f
        PetIntention.SELF_SOOTHE -> 0.3f
        PetIntention.STAY_NEAR -> 0.5f
        PetIntention.WITHDRAW -> 0.2f
        PetIntention.LISTEN -> 0.5f
        PetIntention.STARTLE_RECOVER -> 0.2f
        PetIntention.CELEBRATE -> 0.3f
        PetIntention.RECOVER -> 0.3f
    }

    // ─── Build candidate with full scoring ──────────────────────────────────

    private fun buildBaseCandidate(
        intention: PetIntention,
        ctx: WorkingContext
    ): IntentionCandidate {
        var score = baseWeight(intention)
        val reasons = mutableListOf<String>()

        // Need pressure
        val needDelta = needPressure(intention, ctx)
        if (needDelta != 0f) {
            score += needDelta
            reasons += "need_pressure=%.2f".format(needDelta)
        }

        // Emotion momentum bias
        val emotionDelta = emotionBias(intention, ctx)
        if (emotionDelta != 0f) {
            score += emotionDelta
            reasons += "emotion_bias=%.2f".format(emotionDelta)
        }

        // Relationship modifier
        val relDelta = relationshipModifier(intention, ctx)
        if (relDelta != 0f) {
            score += relDelta
            reasons += "relationship=%.2f".format(relDelta)
        }

        // Perception trigger bonus
        val percDelta = perceptionBonus(intention, ctx)
        if (percDelta != 0f) {
            score += percDelta
            reasons += "perception=%.2f".format(percDelta)
        }

        // Time-of-day context bonus
        val timeDelta = timeContextBonus(intention, ctx)
        if (timeDelta != 0f) {
            score += timeDelta
            reasons += "time_context=%.2f".format(timeDelta)
        }

        // Cooldown penalty
        val cooldownPenalty = cooldownPenalty(intention, ctx)
        if (cooldownPenalty != 0f) {
            score -= cooldownPenalty
            reasons += "cooldown_penalty=%.2f".format(cooldownPenalty)
        }

        // Attention mode modifier
        val attnDelta = attentionModeModifier(intention, ctx)
        if (attnDelta != 0f) {
            score += attnDelta
            reasons += "attention_mode=%.2f".format(attnDelta)
        }

        // Continuity bonus: boost current intention slightly to avoid flip-flopping
        val currentPlan = ctx.currentBehavior
        if (currentPlan != null && currentPlan.intention == intention && !currentPlan.isExpired(ctx.snapshotAtMs)) {
            score += 0.10f
            reasons += "continuity_bonus=0.10"
        }

        // Evolution context modifier (long-term memory, bond, habit, lifecycle)
        val evoDelta = evolutionModifier(intention, ctx.evolutionContext)
        if (evoDelta != 0f) {
            score += evoDelta
            reasons += "evolution=%.2f".format(evoDelta)
        }

        // Fatigue penalty
        val fatigue = ctx.attention.fatigue
        if (fatigue > 0.5f) {
            val fatiguePenalty = when (intention) {
                PetIntention.INVITE_PLAY, PetIntention.PLAY -> fatigue * 0.3f
                PetIntention.SEEK_ATTENTION -> fatigue * 0.2f
                else -> 0f
            }
            if (fatiguePenalty != 0f) {
                score -= fatiguePenalty
                reasons += "fatigue_penalty=%.2f".format(fatiguePenalty)
            }
        }

        return IntentionCandidate(
            intention = intention,
            score = score,
            reasons = reasons
        )
    }

    // ─── Need pressure ───────────────────────────────────────────────────────

    private fun needPressure(intention: PetIntention, ctx: WorkingContext): Float {
        val state = ctx.petState
        val conditions = ctx.conditions
        return when (intention) {
            PetIntention.REQUEST_FOOD ->
                if (conditions.contains(PetCondition.HUNGRY)) 0.5f * (state.hunger / 100f) else 0f

            PetIntention.DOZE, PetIntention.REST ->
                if (conditions.contains(PetCondition.SLEEPY)) 0.4f * (state.sleepiness / 100f) else 0f

            PetIntention.SEEK_ATTENTION, PetIntention.SEEK_COMFORT ->
                if (conditions.contains(PetCondition.LONELY)) 0.4f * ((100 - state.social) / 100f) else 0f

            PetIntention.INVITE_PLAY, PetIntention.PLAY ->
                if (conditions.contains(PetCondition.PLAYFUL)) 0.3f else
                    if (state.energy > 60) 0.1f else 0f

            PetIntention.SELF_SOOTHE ->
                (ctx.emotionMomentum.irritation * 0.3f)

            PetIntention.OBSERVE, PetIntention.INVESTIGATE ->
                (state.energy / 100f) * 0.1f

            else -> 0f
        }
    }

    // ─── Emotion momentum bias ───────────────────────────────────────────────

    private fun emotionBias(intention: PetIntention, ctx: WorkingContext): Float {
        val m = ctx.emotionMomentum
        return when (intention) {
            PetIntention.INVITE_PLAY, PetIntention.PLAY ->
                m.moodPlayful * 0.25f + m.joy * 0.15f - m.drowsiness * 0.2f

            PetIntention.SEEK_ATTENTION ->
                m.moodNeedy * 0.25f + m.neediness * 0.15f

            PetIntention.WITHDRAW ->
                m.moodWithdrawn * 0.3f + m.irritation * 0.2f

            PetIntention.DOZE, PetIntention.REST ->
                m.moodDrowsy * 0.3f + m.drowsiness * 0.2f

            PetIntention.RESPOND_TO_USER ->
                m.moodWarm * 0.1f + m.joy * 0.1f - m.irritation * 0.15f

            PetIntention.STAY_NEAR ->
                m.moodWarm * 0.2f

            PetIntention.SEEK_COMFORT ->
                m.neediness * 0.2f

            PetIntention.STARTLE_RECOVER ->
                m.startledLevel * 0.6f

            PetIntention.CELEBRATE ->
                m.joy * 0.3f

            else -> 0f
        }
    }

    // ─── Relationship modifier ───────────────────────────────────────────────

    private fun relationshipModifier(intention: PetIntention, ctx: WorkingContext): Float {
        val rel = ctx.relationship
        val warmth = rel.warmthModifier()
        return when (intention) {
            PetIntention.STAY_NEAR ->
                rel.familiarity * 0.2f + warmth * 0.1f

            PetIntention.SEEK_ATTENTION ->
                (1f - rel.trust) * 0.1f + rel.recentNeglect * 0.2f

            PetIntention.INVITE_PLAY ->
                rel.familiarity * 0.15f + warmth * 0.1f

            PetIntention.WITHDRAW ->
                -warmth * 0.15f

            PetIntention.RESPOND_TO_USER ->
                (rel.bondScore / 100f) * 0.1f

            else -> 0f
        }
    }

    // ─── Perception trigger bonus ─────────────────────────────────────────────

    private fun perceptionBonus(intention: PetIntention, ctx: WorkingContext): Float {
        val p = ctx.perception
        val nowMs = ctx.snapshotAtMs
        var bonus = 0f

        when (intention) {
            PetIntention.RESPOND_TO_USER -> {
                if (p.touchContext.recentTap || p.touchContext.recentLongPress) bonus += 0.6f
                if (p.voiceContext.voiceActivity && p.voiceContext.commandAddressedToPet) bonus += 0.4f
            }
            PetIntention.LISTEN -> {
                if (p.voiceContext.voiceActivity) bonus += 0.5f
                if (p.audioContext.shouldOrientToSound) bonus += 0.2f
            }
            PetIntention.INVESTIGATE -> {
                if (p.attentionContext.orientingUrgency > 0.3f) bonus += p.attentionContext.orientingUrgency * 0.4f
                if (p.attentionContext.noveltySignal > 0.3f) bonus += 0.2f
            }
            PetIntention.STARTLE_RECOVER -> {
                if (p.audioContext.loudEventActive) bonus += 0.8f
            }
            PetIntention.STAY_NEAR -> {
                if (p.presence.userPresent && p.socialContext.interactionAvailability > 0.5f) bonus += 0.2f
            }
            PetIntention.SEEK_ATTENTION -> {
                if (!p.presence.userPresent) bonus -= 0.3f
                else if (p.presence.stablePresenceMs > 10_000L) bonus += 0.15f
            }
            PetIntention.INVITE_PLAY -> {
                if (p.presence.userPresent && p.socialContext.eyeContactLikelihood > 0.4f) bonus += 0.2f
            }
            PetIntention.OBSERVE -> {
                if (p.presence.userPresent && !p.voiceContext.voiceActivity &&
                    !p.touchContext.recentTap) bonus += 0.2f
            }
            PetIntention.PLAY -> {
                if (p.recentPerceptionSummary.hadTouchWithinMs(5_000L, nowMs)) bonus += 0.3f
            }
            else -> Unit
        }
        return bonus
    }

    // ─── Time-of-day context bonus ───────────────────────────────────────────

    private fun timeContextBonus(intention: PetIntention, ctx: WorkingContext): Float {
        val s = ctx.session
        return when {
            s.isNightTime && (intention == PetIntention.DOZE || intention == PetIntention.REST) -> 0.3f
            s.isMorning && (intention == PetIntention.OBSERVE || intention == PetIntention.INVESTIGATE) -> 0.15f
            s.isFirstGreetToday && intention == PetIntention.STAY_NEAR -> 0.1f
            s.isReturningAfterLongAbsence && intention == PetIntention.SEEK_ATTENTION -> 0.2f
            else -> 0f
        }
    }

    // ─── Cooldown penalty ────────────────────────────────────────────────────

    private fun cooldownPenalty(intention: PetIntention, ctx: WorkingContext): Float {
        val cd = ctx.cooldowns
        val nowMs = ctx.snapshotAtMs
        return when (intention) {
            PetIntention.INVITE_PLAY -> {
                val base = if (cd.isOnCooldown(CooldownDomain.INVITATION, nowMs)) 0.5f else 0f
                val streak = cd.invitationIgnoredStreak * 0.1f
                base + streak.coerceAtMost(0.3f)
            }
            PetIntention.RESPOND_TO_USER -> {
                if (cd.isOnCooldown(CooldownDomain.TOUCH_REACTION, nowMs)) 0.2f else 0f
            }
            PetIntention.SEEK_ATTENTION -> {
                if (cd.isOnCooldown(CooldownDomain.SEEK_ATTENTION, nowMs)) 0.35f else 0f
            }
            PetIntention.SEEK_COMFORT, PetIntention.STAY_NEAR -> {
                if (cd.isOnCooldown(CooldownDomain.LONG_PRESS_CUDDLE, nowMs)) 0.2f else 0f
            }
            else -> 0f
        }
    }

    // ─── Attention mode modifier ─────────────────────────────────────────────

    private fun attentionModeModifier(intention: PetIntention, ctx: WorkingContext): Float {
        return when (ctx.attention.mode) {
            AttentionMode.SOCIAL_LOCK -> when (intention) {
                PetIntention.RESPOND_TO_USER, PetIntention.STAY_NEAR -> 0.2f
                PetIntention.INVESTIGATE, PetIntention.STARTLE_RECOVER -> -0.1f
                else -> 0f
            }
            AttentionMode.DOZING -> when (intention) {
                PetIntention.DOZE, PetIntention.REST -> 0.2f
                PetIntention.INVITE_PLAY, PetIntention.PLAY -> -0.25f
                else -> 0f
            }
            AttentionMode.PLAY_FOCUS -> when (intention) {
                PetIntention.PLAY -> 0.25f
                PetIntention.REST -> -0.2f
                else -> 0f
            }
            AttentionMode.WITHDRAWN -> when (intention) {
                PetIntention.WITHDRAW, PetIntention.SELF_SOOTHE -> 0.25f
                PetIntention.INVITE_PLAY, PetIntention.SEEK_ATTENTION -> -0.25f
                else -> 0f
            }
            AttentionMode.ALERT -> when (intention) {
                PetIntention.STARTLE_RECOVER -> 0.4f
                PetIntention.INVESTIGATE -> 0.2f
                else -> 0f
            }
            AttentionMode.LISTENING -> when (intention) {
                PetIntention.LISTEN -> 0.3f
                PetIntention.RESPOND_TO_USER -> 0.15f
                else -> 0f
            }
            else -> 0f
        }
    }

    // ─── Gating rules ────────────────────────────────────────────────────────

    private fun applyGates(candidate: IntentionCandidate, ctx: WorkingContext): IntentionCandidate {
        val intention = candidate.intention
        val conditions = ctx.conditions
        val p = ctx.perception

        return when {
            // INVITE_PLAY blocked if sleepy
            intention == PetIntention.INVITE_PLAY &&
                conditions.contains(PetCondition.SLEEPY) ->
                candidate.blocked("sleepy:play_invite_blocked")

            // PLAY blocked if no interaction target available
            intention == PetIntention.PLAY &&
                !p.presence.userPresent && !p.touchContext.recentTap ->
                candidate.blocked("no_interaction_target")

            // SEEK_ATTENTION reduced heavily if user absent (but not blocked — internal need valid)
            intention == PetIntention.INVITE_PLAY &&
                !p.presence.userPresent ->
                candidate.blocked("user_absent:no_play_invite")

            // RESPOND_TO_USER blocked if no recent direct trigger
            intention == PetIntention.RESPOND_TO_USER &&
                !p.touchContext.recentTap && !p.touchContext.recentLongPress &&
                (!p.voiceContext.voiceActivity || !p.voiceContext.commandAddressedToPet) ->
                candidate.blocked("no_direct_trigger")

            // STARTLE_RECOVER blocked if no loud sound
            intention == PetIntention.STARTLE_RECOVER &&
                !p.audioContext.loudEventActive ->
                candidate.blocked("no_loud_event")

            // LISTEN blocked if no voice activity
            intention == PetIntention.LISTEN &&
                !p.voiceContext.voiceActivity && !p.audioContext.shouldOrientToSound ->
                candidate.blocked("no_audio_trigger")

            // CELEBRATE blocked if no joyful recent event
            intention == PetIntention.CELEBRATE &&
                ctx.emotionMomentum.joy < 0.3f ->
                candidate.blocked("insufficient_joy")

            // STAY_NEAR blocked if user absent
            intention == PetIntention.STAY_NEAR &&
                !p.presence.userPresent ->
                candidate.blocked("user_absent")

            else -> candidate
        }
    }

    // ─── Evolution context modifier ──────────────────────────────────────────

    /**
     * Applies long-term evolution signals (bond, habit, lifecycle, recent memory)
     * as a bounded modifier on intention scores.
     * Returns 0 when no evolution context is available (safe default).
     */
    private fun evolutionModifier(intention: PetIntention, evo: EvolutionContext?): Float {
        if (evo == null) return 0f
        val bond = evo.bond
        val dayPhase = evo.dayPhase
        val reunionType = evo.reunionType
        val habit = evo.habitProfile
        val modifiers = evo.lifecycleModifiers
        val recentEpisodes = evo.recentEpisodes

        val recentNeglect = recentEpisodes.any { it.neglectSignal }
        val recentGoodCare = recentEpisodes.any { it.careScoreDelta > 5 }

        return when (intention) {
            PetIntention.SEEK_ATTENTION -> {
                // High dependency and expectation increase seeking behavior
                val dependencyBoost = bond.dependency * 0.15f
                val neglectBoost = if (recentNeglect) 0.1f else 0f
                dependencyBoost + neglectBoost
            }
            PetIntention.INVITE_PLAY -> {
                // Playfulness and positive care enable play invitations more readily
                val initiativeBoost = modifiers.initiativeBias * 0.12f
                val goodCareBoost = if (recentGoodCare) 0.08f else 0f
                val nightSuppression = if (dayPhase == DayPhase.NIGHT) -0.2f else 0f
                initiativeBoost + goodCareBoost + nightSuppression
            }
            PetIntention.STAY_NEAR -> {
                // High affection and routine returns increase closeness
                val affectionBoost = bond.affection * 0.1f
                val routineBoost = if (reunionType == ReunionType.ROUTINE_RETURN) 0.08f else 0f
                affectionBoost + routineBoost
            }
            PetIntention.RECOVER -> {
                // After neglect or long absence, recovery intention is more natural
                val neglectSignal = if (recentNeglect || reunionType == ReunionType.LONG_ABSENCE) 0.15f else 0f
                val trustDeficit = (1f - bond.trust) * 0.1f
                neglectSignal + trustDeficit
            }
            PetIntention.WITHDRAW -> {
                // Low stability increases withdrawal tendency
                val instabilityFactor = (1f - bond.stability) * 0.1f
                instabilityFactor
            }
            PetIntention.CELEBRATE -> {
                // Strong bond and good recent care enable celebratory rare moments
                if (bond.affection > 0.7f && bond.trust > 0.6f && recentGoodCare) 0.1f else 0f
            }
            PetIntention.REST, PetIntention.DOZE -> {
                // Night phase increases rest preference
                if (dayPhase == DayPhase.NIGHT) modifiers.initiativeBias * -0.1f else 0f
            }
            PetIntention.OBSERVE -> {
                // High curiosity and morning phase increase observation tendency
                if (dayPhase == DayPhase.MORNING) 0.08f else 0f
            }
            else -> 0f
        }.coerceIn(-0.3f, 0.3f)
    }
}
