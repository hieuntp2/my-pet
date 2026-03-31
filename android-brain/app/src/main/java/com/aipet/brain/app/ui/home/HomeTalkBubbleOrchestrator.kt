package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetGreetingReaction
import kotlinx.coroutines.delay

private const val BUBBLE_DISMISS_GREETING_MS = 5_000L
private const val BUBBLE_DISMISS_FEEDBACK_MS = 3_500L
private const val BUBBLE_DISMISS_AMBIENT_MS = 4_000L
private const val AMBIENT_IDLE_TRIGGER_MS = 25_000L

/**
 * Orchestrates which [HomeTalkBubble] to display on the Home stage at any given moment.
 *
 * Priority order:
 * 1. App-open greeting (highest — shown once until dismissed or any interaction occurs)
 * 2. Interaction feedback (e.g. after tap/long-press/activity)
 * 3. Ambient need line (hunger, sleepy, lonely) — only shown after sufficient idle time
 *
 * Each bubble is auto-dismissed after its type-specific timeout.
 */
@Composable
fun rememberHomeTalkBubbleOrchestrator(
    appOpenGreeting: PetGreetingReaction?,
    feedbackMessage: String?,
    feedbackToken: Long,
    conditions: Set<PetCondition>
): HomeTalkBubble? {
    var currentBubble by remember { mutableStateOf<HomeTalkBubble?>(null) }
    val currentConditions by rememberUpdatedState(conditions)

    // Show greeting immediately when it arrives
    LaunchedEffect(appOpenGreeting?.message) {
        val greeting = appOpenGreeting ?: return@LaunchedEffect
        currentBubble = HomeTalkBubble(message = greeting.message)
        delay(BUBBLE_DISMISS_GREETING_MS)
        if (currentBubble?.message == greeting.message) {
            currentBubble = null
        }
    }

    // Show interaction feedback, overriding greeting if present
    LaunchedEffect(feedbackToken) {
        val msg = feedbackMessage ?: return@LaunchedEffect
        currentBubble = HomeTalkBubble(message = msg)
        delay(BUBBLE_DISMISS_FEEDBACK_MS)
        if (currentBubble?.message == msg) {
            currentBubble = null
        }
    }

    // Ambient need lines shown after extended idle time
    LaunchedEffect(Unit) {
        while (true) {
            delay(AMBIENT_IDLE_TRIGGER_MS)
            // Only show ambient line if no primary message is active
            if (currentBubble == null) {
                val ambientLine = resolveAmbientLine(currentConditions)
                if (ambientLine != null) {
                    currentBubble = HomeTalkBubble(message = ambientLine)
                    delay(BUBBLE_DISMISS_AMBIENT_MS)
                    if (currentBubble?.message == ambientLine) {
                        currentBubble = null
                    }
                }
            }
        }
    }

    return currentBubble
}

private fun resolveAmbientLine(conditions: Set<PetCondition>): String? {
    return when {
        PetCondition.HUNGRY in conditions -> hungerLines.random()
        PetCondition.SLEEPY in conditions -> sleepyLines.random()
        PetCondition.LONELY in conditions -> lonelyLines.random()
        else -> null
    }
}

private val hungerLines = listOf(
    "hơi đói rồi đó…",
    "…đang nghĩ đến đồ ăn",
    "bụng kêu rồi nè"
)

private val sleepyLines = listOf(
    "buồn ngủ ghê…",
    "mmm… mắt nặng quá",
    "*yawn*"
)

private val lonelyLines = listOf(
    "chơi với mình không?",
    "…hơi nhớ bạn đó",
    "ở đây với mình nha"
)
