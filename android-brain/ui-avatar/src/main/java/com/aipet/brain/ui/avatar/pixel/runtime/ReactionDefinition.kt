package com.aipet.brain.ui.avatar.pixel.runtime

import com.aipet.brain.ui.avatar.pixel.animation.FaceReactionType
import com.aipet.brain.ui.avatar.pixel.animation.AnimationPriority

/** A single phase within a reaction sequence. */
internal data class ReactionPhase(
    val name: String,
    val targets: LayerTargets,
    val transitionProfile: TransitionProfile,
    val holdDurationMs: Long
)

/** Full reaction sequence for a given FaceReactionType. */
internal data class ReactionDefinition(
    val type: FaceReactionType,
    val priority: Int,
    val phases: List<ReactionPhase>
)

/** All standard reaction definitions keyed by FaceReactionType. */
internal object ReactionLibrary {

    val all: Map<FaceReactionType, ReactionDefinition> = buildMap {
        put(
            FaceReactionType.TAP_ENGAGED,
            ReactionDefinition(
                type = FaceReactionType.TAP_ENGAGED,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("attention", LayerTargets(gazeX = 0f, leftTopLid = 0f, rightTopLid = 0f, leftBrowY = -0.15f, rightBrowY = -0.15f), TransitionProfile.SNAP, 80L),
                    ReactionPhase("main", LayerTargets(leftTopLid = 0.25f, rightTopLid = 0.25f, leftBottomLid = 0.20f, rightBottomLid = 0.20f, leftBrowY = 0.15f, rightBrowY = 0.15f, browWarmth = 0.85f, extraHighlight = true), TransitionProfile.SMOOTH, 340L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.18f, rightTopLid = 0.18f, leftBottomLid = 0.10f, rightBottomLid = 0.10f, browWarmth = 0.4f), TransitionProfile.SMOOTH, 240L)
                )
            )
        )
        put(
            FaceReactionType.TAP_PLAYFUL,
            ReactionDefinition(
                type = FaceReactionType.TAP_PLAYFUL,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("bounce", LayerTargets(gazeX = 0.3f, leftBrowY = -0.30f, rightBrowY = -0.30f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.PLAYFUL_POP, 200L),
                    ReactionPhase("squint", LayerTargets(leftTopLid = 0.22f, rightTopLid = 0.22f, leftBottomLid = 0.15f, rightBottomLid = 0.15f, browWarmth = 0.80f), TransitionProfile.SMOOTH, 280L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.10f, rightTopLid = 0.10f, browWarmth = 0.30f), TransitionProfile.SMOOTH, 200L)
                )
            )
        )
        put(
            FaceReactionType.TAP_ANNOYED,
            ReactionDefinition(
                type = FaceReactionType.TAP_ANNOYED,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("startle", LayerTargets(gazeX = 0f, leftTopLid = 0f, rightTopLid = 0f, leftBrowY = -0.50f, rightBrowY = -0.50f, browWarmth = 0f), TransitionProfile.STARTLED_CUT, 80L),
                    ReactionPhase("annoyed", LayerTargets(leftTopLid = 0.35f, rightTopLid = 0.35f, leftBrowY = 0.45f, rightBrowY = 0.45f, browWarmth = 0f), TransitionProfile.SMOOTH, 440L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.20f, rightTopLid = 0.20f, leftBrowY = 0.10f, rightBrowY = 0.10f), TransitionProfile.SMOOTH, 260L)
                )
            )
        )
        put(
            FaceReactionType.LONG_PRESS_CUDDLE,
            ReactionDefinition(
                type = FaceReactionType.LONG_PRESS_CUDDLE,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("warm", LayerTargets(leftTopLid = 0.30f, rightTopLid = 0.30f, leftBottomLid = 0.25f, rightBottomLid = 0.25f, leftBrowY = 0.18f, rightBrowY = 0.18f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.SLEEPY_DRIFT, 560L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.18f, rightTopLid = 0.18f, leftBottomLid = 0.10f, rightBottomLid = 0.10f, browWarmth = 0.50f), TransitionProfile.SMOOTH, 320L)
                )
            )
        )
        put(
            FaceReactionType.LONG_PRESS_SLEEPY,
            ReactionDefinition(
                type = FaceReactionType.LONG_PRESS_SLEEPY,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("droop", LayerTargets(leftTopLid = 0.55f, rightTopLid = 0.55f, leftBottomLid = 0.22f, rightBottomLid = 0.22f, leftBrowY = 0.45f, rightBrowY = 0.45f, browWarmth = 0.25f), TransitionProfile.SLEEPY_DRIFT, 560L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.38f, rightTopLid = 0.38f, leftBrowY = 0.25f, rightBrowY = 0.25f, browWarmth = 0.15f), TransitionProfile.SLEEPY_DRIFT, 340L)
                )
            )
        )
        put(
            FaceReactionType.EXCITED_GREETING,
            ReactionDefinition(
                type = FaceReactionType.EXCITED_GREETING,
                priority = AnimationPriority.GREETING,
                phases = listOf(
                    ReactionPhase("anticipate", LayerTargets(gazeX = 0f, leftBrowY = -0.25f, rightBrowY = -0.25f, browWarmth = 0.60f), TransitionProfile.SNAP, 100L),
                    ReactionPhase("burst", LayerTargets(leftTopLid = 0f, rightTopLid = 0f, leftBrowY = -0.70f, rightBrowY = -0.70f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.STARTLED_CUT, 180L),
                    ReactionPhase("bounce", LayerTargets(gazeX = 0.15f, leftBrowY = -0.35f, rightBrowY = -0.35f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.PLAYFUL_POP, 160L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.22f, rightTopLid = 0.22f, leftBottomLid = 0.15f, rightBottomLid = 0.15f, leftBrowY = 0.10f, rightBrowY = 0.10f, browWarmth = 0.80f, extraHighlight = true), TransitionProfile.SMOOTH, 260L)
                )
            )
        )
        put(
            FaceReactionType.STARTLED_SNAP,
            ReactionDefinition(
                type = FaceReactionType.STARTLED_SNAP,
                priority = AnimationPriority.STARTLED,
                phases = listOf(
                    ReactionPhase("snap", LayerTargets(gazeX = 0f, leftTopLid = 0f, rightTopLid = 0f, leftBrowY = -1.0f, rightBrowY = -1.0f, browWarmth = 0.80f, extraHighlight = true), TransitionProfile.STARTLED_CUT, 90L),
                    ReactionPhase("freeze", LayerTargets(leftBrowY = -0.65f, rightBrowY = -0.65f, browWarmth = 0.30f), TransitionProfile.SMOOTH, 400L),
                    ReactionPhase("recover", LayerTargets(leftTopLid = 0.10f, rightTopLid = 0.10f, leftBrowY = -0.30f, rightBrowY = -0.30f, browWarmth = 0f), TransitionProfile.SMOOTH, 200L),
                    ReactionPhase("settle", LayerTargets(), TransitionProfile.SMOOTH, 180L)
                )
            )
        )
        put(
            FaceReactionType.KEYWORD_ATTENTIVE,
            ReactionDefinition(
                type = FaceReactionType.KEYWORD_ATTENTIVE,
                priority = AnimationPriority.AUDIO,
                phases = listOf(
                    ReactionPhase("snap_attention", LayerTargets(gazeX = 0f, leftTopLid = 0.10f, rightTopLid = 0.10f, leftBrowY = -0.35f, rightBrowY = -0.35f), TransitionProfile.SNAP, 120L),
                    ReactionPhase("hold", LayerTargets(leftTopLid = 0.14f, rightTopLid = 0.14f, leftBrowY = -0.25f, rightBrowY = -0.25f), TransitionProfile.SMOOTH, 420L),
                    ReactionPhase("settle", LayerTargets(), TransitionProfile.SMOOTH, 200L)
                )
            )
        )
        put(
            FaceReactionType.LOUD_SOUND_STARTLED,
            ReactionDefinition(
                type = FaceReactionType.LOUD_SOUND_STARTLED,
                priority = AnimationPriority.STARTLED,
                phases = listOf(
                    ReactionPhase("snap", LayerTargets(leftTopLid = 0f, rightTopLid = 0f, leftBrowY = -0.90f, rightBrowY = -0.90f, extraHighlight = true), TransitionProfile.STARTLED_CUT, 70L),
                    ReactionPhase("freeze", LayerTargets(leftBrowY = -0.50f, rightBrowY = -0.50f), TransitionProfile.SMOOTH, 360L),
                    ReactionPhase("settle", LayerTargets(), TransitionProfile.SMOOTH, 220L)
                )
            )
        )
        put(
            FaceReactionType.GAME_CELEBRATE,
            ReactionDefinition(
                type = FaceReactionType.GAME_CELEBRATE,
                priority = AnimationPriority.GREETING,
                phases = listOf(
                    ReactionPhase("joy", LayerTargets(leftBrowY = -0.70f, rightBrowY = -0.70f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.STARTLED_CUT, 200L),
                    ReactionPhase("squint", LayerTargets(leftTopLid = 0.30f, rightTopLid = 0.30f, leftBottomLid = 0.25f, rightBottomLid = 0.25f, browWarmth = 1.0f, extraHighlight = true), TransitionProfile.PLAYFUL_POP, 280L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.20f, rightTopLid = 0.20f, browWarmth = 0.60f), TransitionProfile.SMOOTH, 240L)
                )
            )
        )
        put(
            FaceReactionType.GAME_FAIL,
            ReactionDefinition(
                type = FaceReactionType.GAME_FAIL,
                priority = AnimationPriority.INTERACTION,
                phases = listOf(
                    ReactionPhase("droop", LayerTargets(leftTopLid = 0.40f, rightTopLid = 0.40f, leftBrowY = 0.50f, rightBrowY = 0.50f, browWarmth = 0f), TransitionProfile.SMOOTH, 420L),
                    ReactionPhase("settle", LayerTargets(leftTopLid = 0.25f, rightTopLid = 0.25f, leftBrowY = 0.20f, rightBrowY = 0.20f), TransitionProfile.SMOOTH, 280L)
                )
            )
        )
    }
}
