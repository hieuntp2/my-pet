package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.pet.AbsenceBucket
import com.aipet.brain.brain.pet.PetBehaviorFamily
import com.aipet.brain.brain.pet.PetBehaviorScoringEngine
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotionalConfig
import com.aipet.brain.brain.pet.PetGreetingContext
import com.aipet.brain.brain.pet.PetGreetingStyle
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.pet.RelationshipStage
import com.aipet.brain.brain.pet.SessionInteractionState
import com.aipet.brain.brain.personality.PetTrait

/**
 * Unified debug screen for the emotional life system (NX batches).
 * Provides visibility into all v2 state layers, scoring, and scenario injectors.
 */
@Composable
fun EmotionalSystemsDebugScreen(
    petState: PetState?,
    petTraits: PetTrait?,
    petConditions: Set<PetCondition>,
    currentAbsenceBucket: AbsenceBucket?,
    currentGreetingStyle: PetGreetingStyle?,
    currentRelationshipStage: RelationshipStage?,
    sessionState: SessionInteractionState,
    lastBehaviorScoringResult: PetBehaviorScoringEngine.ScoringResult?,
    onNavigateBack: () -> Unit,
    // Scenario injection actions
    onInjectTiredPet: () -> Unit,
    onInjectHungryPet: () -> Unit,
    onInjectNeglectedPet: () -> Unit,
    onInjectBondedPet: () -> Unit,
    onInjectOverstimulatedPet: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Emotional Systems Debug",
            style = MaterialTheme.typography.headlineSmall
        )

        // ── Needs (Layer A) ───────────────────────────────────────────────────
        EmotionalDebugCard(title = "Needs (Layer A)") {
            if (petState != null) {
                DebugRow("Energy", "${petState.energy}")
                DebugRow("Hunger", "${petState.hunger}")
                DebugRow("Sleepiness", "${petState.sleepiness}")
                DebugRow("Social need", "${petState.social}")
                DebugRow("Comfort", "${petState.comfort}")
                DebugRow("Stimulation", "${petState.stimulation}")
            } else {
                Text("No state", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Mood (Layer B) ────────────────────────────────────────────────────
        EmotionalDebugCard(title = "Mood (Layer B)") {
            if (petState != null) {
                DebugRow("Valence", "${petState.moodValence}")
                DebugRow("Arousal", "${petState.moodArousal}")
            } else {
                Text("No state", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Relationship (Layer D) ────────────────────────────────────────────
        EmotionalDebugCard(title = "Relationship (Layer D)") {
            if (petState != null) {
                DebugRow("Bond", "${petState.bond}")
                DebugRow("Trust", "${petState.trustScore}")
                DebugRow("Attachment", "${petState.attachmentScore}")
                DebugRow("Neglect streak", "${petState.neglectStreak}")
                DebugRow("Care streak", "${petState.careStreak}")
                DebugRow("Stage", currentRelationshipStage?.name ?: "—")
            } else {
                Text("No state", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Personality (Layer E) ─────────────────────────────────────────────
        EmotionalDebugCard(title = "Personality (Layer E)") {
            if (petTraits != null) {
                DebugRow("Playful", "%.2f".format(petTraits.playful))
                DebugRow("Lazy", "%.2f".format(petTraits.lazy))
                DebugRow("Curious", "%.2f".format(petTraits.curious))
                DebugRow("Sociability", "%.2f".format(petTraits.social))
                DebugRow("Patience", "%.2f".format(petTraits.patience))
                DebugRow("Attachment", "%.2f".format(petTraits.attachment))
                DebugRow("Energy profile", "%.2f".format(petTraits.energyProfile))
            } else {
                Text("No traits", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Active conditions ────────────────────────────────────────────────
        EmotionalDebugCard(title = "Active Conditions") {
            if (petConditions.isEmpty()) {
                Text("None", style = MaterialTheme.typography.bodySmall)
            } else {
                petConditions.forEach { condition ->
                    Text("• ${condition.name}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── Greeting resolution ──────────────────────────────────────────────
        EmotionalDebugCard(title = "Greeting / Return Context") {
            DebugRow("Absence bucket", currentAbsenceBucket?.name ?: "—")
            DebugRow("Greeting style", currentGreetingStyle?.name ?: "—")
        }

        // ── Session stats ─────────────────────────────────────────────────────
        EmotionalDebugCard(title = "Session State") {
            DebugRow("Interaction count", "${sessionState.sessionInteractionCount}")
            DebugRow("Invitation count", "${sessionState.invitationCount}")
            DebugRow("Ignored invitations", "${sessionState.ignoredInvitationCount}")
            DebugRow("Was comforted", "${sessionState.wasComfortedThisSession}")
        }

        // ── Behavior scoring ──────────────────────────────────────────────────
        EmotionalDebugCard(title = "Behavior Scoring (last)") {
            if (lastBehaviorScoringResult != null) {
                DebugRow("Winner", lastBehaviorScoringResult.winner.name)
                Text(
                    text = "Ranked candidates:",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
                lastBehaviorScoringResult.rankedCandidates.forEach { candidate ->
                    Text(
                        text = "  ${candidate.family.name}: ${"%.3f".format(candidate.score)} — ${candidate.reasons.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (lastBehaviorScoringResult.suppressionNotes.isNotEmpty()) {
                    Text(
                        text = "Suppressions:",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    lastBehaviorScoringResult.suppressionNotes.forEach { note ->
                        Text("  ⚠ $note", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Text("No scoring data", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ── Scenario injection ────────────────────────────────────────────────
        Text(
            text = "Scenario Injection (Debug Only)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onInjectTiredPet,
                modifier = Modifier.weight(1f)
            ) { Text("Tired") }
            Button(
                onClick = onInjectHungryPet,
                modifier = Modifier.weight(1f)
            ) { Text("Hungry") }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onInjectNeglectedPet,
                modifier = Modifier.weight(1f)
            ) { Text("Neglected") }
            Button(
                onClick = onInjectBondedPet,
                modifier = Modifier.weight(1f)
            ) { Text("Bonded") }
        }
        Button(
            onClick = onInjectOverstimulatedPet,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Overstimulated") }

        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Back to Debug")
        }
    }
}

@Composable
private fun EmotionalDebugCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun DebugRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}
