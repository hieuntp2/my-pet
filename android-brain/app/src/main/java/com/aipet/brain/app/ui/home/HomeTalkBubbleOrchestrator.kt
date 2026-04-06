package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.aipet.brain.app.behavior.experience.TalkDirective
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
 * 1. Behavior-authoritative talk directive
 * 2. App-open greeting
 * 3. Interaction feedback
 * 4. Ambient need line (hunger, sleepy, lonely) after idle delay
 */
@Composable
fun rememberHomeTalkBubbleOrchestrator(
    behaviorTalkDirective: TalkDirective?,
    appOpenGreeting: PetGreetingReaction?,
    feedbackMessage: String?,
    feedbackToken: Long,
    conditions: Set<PetCondition>,
    todaySummary: HomeTodaySummary?
): HomeTalkBubble? {
    var currentBubble by remember { mutableStateOf<HomeTalkBubble?>(null) }
    var lastAmbientLine by remember { mutableStateOf<String?>(null) }
    var hasShownContinuityBubble by remember { mutableStateOf(false) }
    val currentConditions by rememberUpdatedState(conditions)
    val currentTodaySummary by rememberUpdatedState(todaySummary)

    // Behavior-authoritative talk output has highest priority.
    LaunchedEffect(behaviorTalkDirective?.issuedAtMs) {
        val directive = behaviorTalkDirective ?: return@LaunchedEffect
        currentBubble = HomeTalkBubble(message = directive.message)
        delay(directive.maxDisplayMs.coerceAtLeast(800L))
        if (currentBubble?.message == directive.message) {
            currentBubble = null
        }
    }

    // Show greeting immediately when it arrives.
    LaunchedEffect(appOpenGreeting?.message) {
        val greeting = appOpenGreeting ?: return@LaunchedEffect
        currentBubble = HomeTalkBubble(message = greeting.message)
        delay(BUBBLE_DISMISS_GREETING_MS)
        if (currentBubble?.message == greeting.message) {
            currentBubble = null
        }
    }

    // Show interaction feedback, overriding greeting if present.
    LaunchedEffect(feedbackToken) {
        val message = feedbackMessage ?: return@LaunchedEffect
        currentBubble = HomeTalkBubble(message = message)
        delay(BUBBLE_DISMISS_FEEDBACK_MS)
        if (currentBubble?.message == message) {
            currentBubble = null
        }
    }

    // Ambient need lines shown after extended idle time with anti-repeat.
    LaunchedEffect(Unit) {
        while (true) {
            delay(AMBIENT_IDLE_TRIGGER_MS)
            if (currentBubble == null) {
                val continuitySummary = currentTodaySummary
                if (!hasShownContinuityBubble && continuitySummary != null) {
                    val continuityLine = formatContinuityLine(continuitySummary)
                    hasShownContinuityBubble = true
                    currentBubble = HomeTalkBubble(message = continuityLine)
                    delay(BUBBLE_DISMISS_AMBIENT_MS)
                    if (currentBubble?.message == continuityLine) {
                        currentBubble = null
                    }
                    continue
                }
                val ambientLine = resolveAmbientLine(
                    conditions = currentConditions,
                    previousLine = lastAmbientLine
                )
                if (ambientLine != null) {
                    lastAmbientLine = ambientLine
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

private fun resolveAmbientLine(
    conditions: Set<PetCondition>,
    previousLine: String?
): String? {
    val linePool = when {
        PetCondition.HUNGRY in conditions -> hungerLines
        PetCondition.SLEEPY in conditions -> sleepyLines
        PetCondition.LONELY in conditions -> lonelyLines
        else -> return null
    }
    return pickAmbientLine(
        pool = linePool,
        previousLine = previousLine
    )
}

private fun pickAmbientLine(
    pool: List<String>,
    previousLine: String?
): String? {
    if (pool.isEmpty()) {
        return null
    }
    if (pool.size == 1 || previousLine == null) {
        return pool.random()
    }

    val weightedPool = buildList {
        addAll(pool)
        // Light weight toward index 0 for a stable identity tone.
        add(pool.first())
    }
    val candidate = weightedPool.random()
    if (candidate != previousLine) {
        return candidate
    }
    return pool.firstOrNull { line -> line != previousLine } ?: candidate
}

private fun formatContinuityLine(summary: HomeTodaySummary): String {
    val raw = summary.body.trim().ifBlank { summary.title.trim() }
    if (raw.isBlank()) {
        return "I remember our moments today."
    }
    return if (raw.length <= 72) {
        "Today: $raw"
    } else {
        "Today: ${raw.take(69)}..."
    }
}

private val hungerLines = listOf(
    "a little hungry now...",
    "thinking about food right now",
    "my tummy is asking for a snack"
)

private val sleepyLines = listOf(
    "feeling very sleepy...",
    "mmm... eyes are heavy",
    "*yawn*"
)

private val lonelyLines = listOf(
    "want to hang out with me?",
    "...kind of missing you",
    "stay with me for a bit"
)
