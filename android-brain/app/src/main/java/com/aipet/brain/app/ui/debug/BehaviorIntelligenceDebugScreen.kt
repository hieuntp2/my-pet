package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.attention.AttentionDebugState
import com.aipet.brain.brain.b2.domain.BehaviorEngineDebugState
import com.aipet.brain.brain.fusion.PerceptionFusionSnapshot
import com.aipet.brain.app.ui.navigation.PetPrimaryDestination
import com.aipet.brain.app.ui.navigation.PetPrimaryNavigationBar

@Composable
fun BehaviorIntelligenceDebugScreen(
    behaviorDebugState: BehaviorEngineDebugState?,
    attentionDebugState: AttentionDebugState?,
    fusionSnapshot: PerceptionFusionSnapshot,
    onNavigateToHome: () -> Unit,
    onNavigateToDiary: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PetPrimaryNavigationBar(
            selectedDestination = PetPrimaryDestination.Debug,
            onNavigateHome = onNavigateToHome,
            onNavigateDiary = onNavigateToDiary,
            onNavigateDebug = onNavigateToDebug
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Text("Behavior Intelligence Debug", style = MaterialTheme.typography.titleLarge)

            // ── Behavior Engine v2 ────────────────────────────────────────────
            BehaviorEngineSection(behaviorDebugState)

            HorizontalDivider()

            // ── Attention System ──────────────────────────────────────────────
            AttentionSystemSection(attentionDebugState)

            HorizontalDivider()

            // ── Perception Fusion ─────────────────────────────────────────────
            PerceptionFusionSection(fusionSnapshot)
        }
    }
}

@Composable
private fun BehaviorEngineSection(state: BehaviorEngineDebugState?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Behavior Engine v2", style = MaterialTheme.typography.titleMedium)
            if (state == null) {
                Text("Not initialized", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            DebugRow("Active Intention", state.activeIntention.name)
            DebugRow("Active Plan", "${state.activePlanId} (${state.activePlanLabel})")
            DebugRow("Last Cycle", state.lastScoringCycleMs.toReadableTime())
            DebugRow("Emotion Mood", state.emotionMomentum.dominantMoodLabel())
            DebugRow("Joy/Comfort/Curiosity",
                "J=%.2f C=%.2f Cr=%.2f".format(
                    state.emotionMomentum.joy, state.emotionMomentum.comfort, state.emotionMomentum.curiosity))
            DebugRow("Drowsy/Irritation/Startled",
                "D=%.2f Ir=%.2f St=%.2f".format(
                    state.emotionMomentum.drowsiness, state.emotionMomentum.irritation, state.emotionMomentum.startledLevel))
            DebugRow("Mood Fields",
                "Pl=%.2f Wd=%.2f Wm=%.2f Dr=%.2f Ne=%.2f".format(
                    state.emotionMomentum.moodPlayful, state.emotionMomentum.moodWithdrawn,
                    state.emotionMomentum.moodWarm, state.emotionMomentum.moodDrowsy, state.emotionMomentum.moodNeedy))
            DebugRow("Relationship Bond", "${state.relationshipState.bondScore}")
            DebugRow("Familiarity/Trust",
                "F=%.2f T=%.2f".format(state.relationshipState.familiarity, state.relationshipState.trust))
            DebugRow("Warmth/Neglect",
                "W=%.2f N=%.2f".format(state.relationshipState.recentWarmth, state.relationshipState.recentNeglect))
            DebugRow("Invitation Streak", "${state.cooldownState.invitationIgnoredStreak}")
            DebugRow("Recent Taps", "${state.cooldownState.recentTapCount}")

            Text("Top 5 Candidates:", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp))
            state.topCandidates.forEach { c ->
                val marker = if (c.isWinner) "★ " else "  "
                val blockedStr = if (c.blocked) " [BLOCKED: ${c.blockReason}]" else ""
                Text(
                    "$marker${c.intention.name}: %.3f$blockedStr".format(c.score),
                    style = MaterialTheme.typography.bodySmall
                )
                if (c.reasons.isNotEmpty()) {
                    Text("    ${c.reasons.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AttentionSystemSection(state: AttentionDebugState?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Attention System", style = MaterialTheme.typography.titleMedium)
            if (state == null) {
                Text("Not initialized", style = MaterialTheme.typography.bodySmall)
                return@Column
            }
            DebugRow("Mode", state.currentMode.name)
            DebugRow("Active Target", "${state.activeTarget.type.name} (${state.activeTarget.id ?: "-"})")
            DebugRow("Salience", "%.3f".format(state.activeTarget.salience))
            DebugRow("Intensity/Stickiness", "I=%.2f S=%.2f".format(state.intensity, state.stickiness))
            DebugRow("Fatigue", "%.3f".format(state.fatigue))
            DebugRow("Hold Duration", "${state.holdDurationMs}ms")
            DebugRow("Can Interrupt", "${state.availableForInterrupt}")
            DebugRow("Last Shift", state.lastShiftAtMs.toReadableTime())
            DebugRow("Last Shift Reason", state.lastShiftReason)

            Text("Candidates:", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp))
            state.topCandidates.take(5).forEach { c ->
                Text(
                    "  ${c.type.name}: raw=%.3f eff=%.3f cost=%.3f".format(
                        c.rawSalience, c.effectiveSalience, c.switchCostPenalty),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PerceptionFusionSection(snapshot: PerceptionFusionSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Perception Fusion", style = MaterialTheme.typography.titleMedium)

            val p = snapshot.presence
            DebugRow("User Present", "${p.userPresent}")
            DebugRow("Familiar User", "${p.familiarUserPresent}")
            DebugRow("Recognized ID", p.recognizedPersonId ?: "-")
            DebugRow("Face Count", "${p.faceCount}")
            DebugRow("Stable Presence", "${p.stablePresenceMs}ms")
            DebugRow("Absence", "${p.absenceMs}ms")

            val a = snapshot.audioContext
            DebugRow("Audio Level", "%.3f".format(a.ambientLevel))
            DebugRow("Loud Event", "${a.loudEventActive}")
            DebugRow("Self Playback", "${a.selfPlaybackActive}")
            DebugRow("External Confidence", "%.3f".format(a.externalSoundConfidence))
            DebugRow("Should Orient", "${a.shouldOrientToSound}")

            val v = snapshot.voiceContext
            DebugRow("Voice Active", "${v.voiceActivity}")
            DebugRow("Command Type", v.commandType?.name ?: "-")
            DebugRow("Cmd Confidence", "%.3f".format(v.commandConfidence))
            DebugRow("Addressed To Pet", "${v.commandAddressedToPet}")
            DebugRow("Recent Speech", "${v.recentSpeechMs}ms ago")

            val t = snapshot.touchContext
            DebugRow("Recent Tap", "${t.recentTap}")
            DebugRow("Recent Long Press", "${t.recentLongPress}")
            DebugRow("Affection", "%.3f".format(t.affectionLikelihood))
            DebugRow("Spam", "%.3f".format(t.spamLikelihood))
            DebugRow("Recent Taps", "${t.recentTapCount}")

            val s = snapshot.socialContext
            DebugRow("User Watching", "${s.userWatchingPet}")
            DebugRow("Eye Contact", "%.3f".format(s.eyeContactLikelihood))
            DebugRow("Availability", "%.3f".format(s.interactionAvailability))
            DebugRow("Warmth Signal", "%.3f".format(s.socialWarmthSignal))

            DebugRow("Env Pressure", "%.3f".format(snapshot.environmentContext.interactionPressure))
            DebugRow("Updated", snapshot.updatedAtMs.toReadableTime())
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.45f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(0.55f)
        )
    }
}

private fun Long.toReadableTime(): String {
    if (this <= 0L) return "-"
    val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
    return sdf.format(java.util.Date(this))
}
