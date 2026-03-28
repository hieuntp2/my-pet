package com.aipet.brain.app.avatar

import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

class HomePixelPetAvatarIntentResolver(
    private val priorityPolicy: HomePixelPetAvatarIntentPriorityPolicy = HomePixelPetAvatarIntentPriorityPolicy()
) {
    fun resolve(
        bridgeInput: HomePixelPetAvatarBridgeInput,
        previousResolution: HomePixelPetAvatarIntentResolution? = null
    ): HomePixelPetAvatarIntentResolution {
        // Greeting takes absolute priority — pet immediately reacts to app-open greeting
        // and its reaction overrides any perception or audio signal that happens to be active.
        if (bridgeInput.greetingBoostIntent != null) {
            return HomePixelPetAvatarIntentResolution(
                intent = bridgeInput.greetingBoostIntent,
                decisionReason = "greeting_active_boost",
                sourceSummary = bridgeInput.sourceSummary,
                policySummary = priorityPolicy.policySummary
            )
        }

        // Transient tap/long-press reaction takes second-highest priority.
        // Shown for TapReactionPresentationMapper.REACTION_DURATION_MS then cleared by caller.
        if (bridgeInput.transientReactionIntent != null) {
            return HomePixelPetAvatarIntentResolution(
                intent = bridgeInput.transientReactionIntent,
                decisionReason = "tap_reaction_transient",
                sourceSummary = bridgeInput.sourceSummary,
                policySummary = priorityPolicy.policySummary
            )
        }

        // Sound reaction (third priority): pet briefly reacts to voice or ambient sound.
        // Tap reactions take precedence since this is checked after transientReactionIntent.
        if (bridgeInput.soundReactionIntent != null) {
            return HomePixelPetAvatarIntentResolution(
                intent = bridgeInput.soundReactionIntent,
                decisionReason = "sound_reaction_transient",
                sourceSummary = bridgeInput.sourceSummary,
                policySummary = priorityPolicy.policySummary
            )
        }

        val candidates = buildList {
            if (bridgeInput.hasAudioAttention) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.PROCESSING,
                        reason = "audio_attention_detected"
                    )
                )
            }
            if (bridgeInput.hasDirectEngagement) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.ENGAGED,
                        reason = "direct_engagement_signal"
                    )
                )
            }
            if (bridgeInput.hasExcitedEmotion) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.EXCITED,
                        reason = "excited_emotion_signal"
                    )
                )
            }
            if (bridgeInput.hasPerceptionAsking) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.ASKING,
                        reason = "stable_unknown_candidate_detected"
                    )
                )
            }
            if (bridgeInput.hasPerceptionLooking) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.LOOKING,
                        reason = "perception_scan_active"
                    )
                )
            }
            if (bridgeInput.hasHungryEmotion) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.HUNGRY,
                        reason = "hungry_emotion_signal"
                    )
                )
            }
            if (bridgeInput.hasLowEnergy) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.LOW_ENERGY,
                        reason = "low_energy_signal"
                    )
                )
            }
            if (bridgeInput.hasSadEmotion) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.SAD,
                        reason = "sad_emotion_signal"
                    )
                )
            }
            if (bridgeInput.hasAttentiveInterest) {
                add(
                    HomePixelPetAvatarIntentCandidate(
                        intent = PixelPetAvatarIntent.ATTENTIVE,
                        reason = "attentive_interest_signal"
                    )
                )
            }
            add(
                HomePixelPetAvatarIntentCandidate(
                    intent = PixelPetAvatarIntent.NEUTRAL,
                    reason = "fallback_neutral"
                )
            )
        }

        val selectedCandidate = priorityPolicy.selectCandidate(candidates)
        val keptPrevious = priorityPolicy.shouldKeepPrevious(
            previousResolution = previousResolution,
            selectedCandidate = selectedCandidate
        )

        return if (keptPrevious && previousResolution != null) {
            previousResolution.copy(
                sourceSummary = bridgeInput.sourceSummary,
                policySummary = priorityPolicy.policySummary,
                decisionReason = "hold_previous_${previousResolution.intent.name.lowercase()}_over_neutral"
            )
        } else {
            HomePixelPetAvatarIntentResolution(
                intent = selectedCandidate.intent,
                decisionReason = selectedCandidate.reason,
                sourceSummary = bridgeInput.sourceSummary,
                policySummary = priorityPolicy.policySummary
            )
        }
    }
}

data class HomePixelPetAvatarIntentResolution(
    val intent: PixelPetAvatarIntent,
    val decisionReason: String,
    val sourceSummary: String,
    val policySummary: String
)

data class HomePixelPetAvatarIntentCandidate(
    val intent: PixelPetAvatarIntent,
    val reason: String
)

class HomePixelPetAvatarIntentPriorityPolicy {
    // Higher score wins; map order is the explicit tie-break if scores later converge.
    private val priorities = linkedMapOf(
        PixelPetAvatarIntent.PROCESSING to 500,
        PixelPetAvatarIntent.ENGAGED to 400,
        PixelPetAvatarIntent.EXCITED to 390,
        PixelPetAvatarIntent.ASKING to 300,
        PixelPetAvatarIntent.LOOKING to 250,
        PixelPetAvatarIntent.HUNGRY to 225,
        PixelPetAvatarIntent.LOW_ENERGY to 200,
        PixelPetAvatarIntent.SAD to 180,
        PixelPetAvatarIntent.ATTENTIVE to 100,
        PixelPetAvatarIntent.NEUTRAL to 0
    )

    val policySummary: String =
        "processing>engaged>excited>asking>looking>hungry>low_energy>sad>attentive>neutral; keep_previous_over_neutral=true"

    fun selectCandidate(candidates: List<HomePixelPetAvatarIntentCandidate>): HomePixelPetAvatarIntentCandidate {
        return candidates.maxWithOrNull(
            compareBy<HomePixelPetAvatarIntentCandidate> { priorityOf(it.intent) }
                .thenBy { -candidateIndex(it.intent) }
        ) ?: HomePixelPetAvatarIntentCandidate(
            intent = PixelPetAvatarIntent.NEUTRAL,
            reason = "fallback_neutral"
        )
    }

    fun shouldKeepPrevious(
        previousResolution: HomePixelPetAvatarIntentResolution?,
        selectedCandidate: HomePixelPetAvatarIntentCandidate
    ): Boolean {
        if (previousResolution == null) {
            return false
        }
        return selectedCandidate.intent == PixelPetAvatarIntent.NEUTRAL &&
            priorityOf(previousResolution.intent) > priorityOf(PixelPetAvatarIntent.NEUTRAL)
    }

    private fun priorityOf(intent: PixelPetAvatarIntent): Int = priorities.getValue(intent)

    private fun candidateIndex(intent: PixelPetAvatarIntent): Int {
        return priorities.keys.indexOf(intent)
    }
}
