package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import com.aipet.brain.brain.evolution.EvolutionContext
import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import com.aipet.brain.brain.evolution.domain.SemanticMemoryFact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug screen for the full Evolution System.
 * Shows episodes, bond state, semantic facts, habit profile, and lifecycle phase.
 */
@Composable
fun EvolutionSystemDebugScreen(
    evolutionContext: EvolutionContext?,
    recentEpisodes: List<MemoryEpisode>,
    semanticFacts: List<SemanticMemoryFact>,
    bond: BondStateV2?,
    habitProfile: UserHabitProfile?,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text("← Back")
        }
        Text("Evolution System Debug", style = MaterialTheme.typography.headlineSmall)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bond State
            item {
                EvoDebugCard(title = "Bond State v2") {
                    if (bond != null) {
                        EvoLabelValue("Affection", "%.2f".format(bond.affection))
                        EvoLabelValue("Trust", "%.2f".format(bond.trust))
                        EvoLabelValue("Dependency", "%.2f".format(bond.dependency))
                        EvoLabelValue("Stability", "%.2f".format(bond.stability))
                        EvoLabelValue("Label", bond.label())
                    } else {
                        Text("Not loaded", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Evolution Context
            item {
                EvoDebugCard(title = "Evolution Context") {
                    if (evolutionContext != null) {
                        EvoLabelValue("Day Phase", evolutionContext.dayPhase.name)
                        EvoLabelValue("Reunion Type", evolutionContext.reunionType.name)
                        EvoLabelValue("Expectation", evolutionContext.expectationState.name)
                        EvoLabelValue("Personality Profile", evolutionContext.personalityProfile)
                        EvoLabelValue("Lifecycle Energy Bias", evolutionContext.lifecycleModifiers.energyBias.toString())
                        EvoLabelValue("Lifecycle Initiative", "%.2f".format(evolutionContext.lifecycleModifiers.initiativeBias))
                    } else {
                        Text("No context yet", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Habit Profile
            item {
                EvoDebugCard(title = "User Habit Profile") {
                    if (habitProfile != null) {
                        EvoLabelValue("Strongest Daypart", habitProfile.strongestDaypart)
                        EvoLabelValue("Consistency", "%.2f".format(habitProfile.recentConsistencyScore))
                        EvoLabelValue("Preferred Slots", habitProfile.preferredTimeSlotsJson)
                        EvoLabelValue("Avg Session", formatMs(habitProfile.avgSessionLengthMs))
                        EvoLabelValue("Primary Style", habitProfile.primaryInteractionStyle)
                    } else {
                        Text("Not loaded", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Semantic Facts
            item {
                Text("Semantic Facts (${semanticFacts.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(semanticFacts) { fact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(fact.key, style = MaterialTheme.typography.labelMedium)
                        Text(fact.valueJson, style = MaterialTheme.typography.bodySmall)
                        EvoLabelValue("Confidence", "%.2f".format(fact.confidence))
                        EvoLabelValue("Episodes", fact.sourceEpisodeCount.toString())
                    }
                }
            }

            // Recent Episodes
            item {
                Text("Recent Episodes (${recentEpisodes.size})", style = MaterialTheme.typography.titleMedium)
            }
            items(recentEpisodes) { ep ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(ep.summaryText, style = MaterialTheme.typography.bodyMedium)
                        EvoLabelValue("Importance", "%.2f".format(ep.importanceScore))
                        EvoLabelValue("Duration", formatMs(ep.durationMs))
                        EvoLabelValue("Interactions", ep.interactionCount.toString())
                        EvoLabelValue("Care Δ", ep.careScoreDelta.toString())
                        EvoLabelValue("Bond Δ", ep.bondDelta.toString())
                        EvoLabelValue("Neglect", ep.neglectSignal.toString())
                        EvoLabelValue("Reunion", ep.reunionType)
                        EvoLabelValue("Emotion", ep.dominantPetEmotion)
                        Text(
                            formatDate(ep.createdAtMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    if (ms <= 0L) return "—"
    val minutes = ms / 60_000L
    val seconds = (ms % 60_000L) / 1000L
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

private fun formatDate(ms: Long): String {
    if (ms <= 0L) return "—"
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ms))
}

@Composable
private fun EvoDebugCard(
    title: String,
    content: @Composable () -> Unit
) {
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
private fun EvoLabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
