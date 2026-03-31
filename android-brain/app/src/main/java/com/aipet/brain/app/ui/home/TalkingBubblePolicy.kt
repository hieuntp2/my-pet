package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.aipet.brain.ui.avatar.model.PetBubblePayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages bubble visibility, auto-dismiss timing, and cooldown between bubbles.
 *
 * Replace rule: a new [show] call replaces the current bubble immediately.
 * Cooldown: after auto-dismiss, waits [COOLDOWN_MS] before accepting the next bubble.
 * Calls to [show] during cooldown are silently dropped; use [showForce] to bypass.
 */
class TalkingBubblePolicy(private val scope: CoroutineScope) {

    private val _currentBubble = MutableStateFlow<PetBubblePayload?>(null)
    val currentBubble: StateFlow<PetBubblePayload?> = _currentBubble.asStateFlow()

    private var dismissJob: Job? = null
    private var cooldownJob: Job? = null
    private var inCooldown = false

    /**
     * Shows [payload], replacing any current bubble.
     * Silently dropped if the policy is in its post-dismiss cooldown window.
     */
    fun show(payload: PetBubblePayload) {
        if (inCooldown) return
        replaceWith(payload)
    }

    /**
     * Shows [payload] immediately, bypassing cooldown.
     * Use for high-priority reactions (e.g. tap while another bubble is visible).
     */
    fun showForce(payload: PetBubblePayload) {
        cooldownJob?.cancel()
        inCooldown = false
        replaceWith(payload)
    }

    private fun replaceWith(payload: PetBubblePayload) {
        dismissJob?.cancel()
        _currentBubble.value = payload
        dismissJob = scope.launch {
            delay(payload.durationMs)
            autoDismiss()
        }
    }

    private fun autoDismiss() {
        _currentBubble.value = null
        inCooldown = true
        cooldownJob = scope.launch {
            delay(COOLDOWN_MS)
            inCooldown = false
        }
    }

    companion object {
        const val COOLDOWN_MS: Long = 1_500L
    }
}

@Composable
fun rememberTalkingBubblePolicy(): TalkingBubblePolicy {
    val scope = rememberCoroutineScope()
    return remember { TalkingBubblePolicy(scope) }
}
