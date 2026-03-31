package com.aipet.brain.app.gameplay

import com.aipet.brain.app.ui.home.SparkGameController
import com.aipet.brain.app.ui.home.SparkGamePhase
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.state.BrainState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Runs a periodic eligibility check and, when all anti-fatigue conditions pass,
 * instructs [SparkGameController] to begin the pet-led invitation sequence.
 *
 * Must be started from a scope that survives the Home screen (e.g. viewModelScope or the
 * app coroutine scope in PetBrainApp). Cancelled automatically when the scope is cancelled.
 *
 * Architecture: lives in :app because it needs both game UI state (SparkGameController)
 * and brain-level condition/state signals. The :brain module only publishes events — it
 * has no direct dependency on UI state.
 */
class GameInvitationEngine(
    private val sparkController: SparkGameController,
    private val policy: GameInvitationPolicy = GameInvitationPolicy(),
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Starts the periodic invitation evaluation loop.
     *
     * @param petConditionsFlow     Emits the current active [PetCondition] set.
     * @param brainStateFlow        Emits the current [BrainState].
     * @param scope                 Coroutine scope to launch the evaluation loop in.
     */
    fun start(
        petConditionsFlow: StateFlow<Set<PetCondition>>,
        brainStateFlow: StateFlow<BrainState>,
        scope: CoroutineScope
    ) {
        scope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                evaluateAndMaybeInvite(
                    petConditions = petConditionsFlow.value,
                    brainState = brainStateFlow.value
                )
            }
        }
    }

    /** Called by the game when it completes (win or lose) so policy suppression activates. */
    fun onGameCompleted() {
        policy.recordGameCompleted(nowProvider())
    }

    /** Called when the user manually starts the game from the menu (non-invitation path). */
    fun onManualGameStarted() {
        policy.recordManualGameStarted(nowProvider())
    }

    /** Called by SparkGameController when an invitation is ignored (timeout). */
    fun onInviteIgnored() {
        policy.recordInviteIgnored()
    }

    private fun evaluateAndMaybeInvite(
        petConditions: Set<PetCondition>,
        brainState: BrainState
    ) {
        val nowMs = nowProvider()
        val gamePhase = sparkController.state.phase

        if (policy.isEligible(
                nowMs = nowMs,
                gamePhase = gamePhase,
                petConditions = petConditions,
                brainState = brainState
            )
        ) {
            policy.recordInvitationSent(nowMs)
            sparkController.startInvite()
        }
    }

    companion object {
        /** How often the engine checks invitation eligibility. */
        private const val CHECK_INTERVAL_MS = 30_000L  // Every 30 seconds
    }
}
