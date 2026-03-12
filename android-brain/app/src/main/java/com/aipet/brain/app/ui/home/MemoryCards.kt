package com.aipet.brain.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.memory.objects.ObjectRecord
import com.aipet.brain.memory.persons.PersonRecord
import java.util.Locale

@Composable
internal fun RecentInteractionsCard(
    interactions: List<EventEnvelope>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Recent Interactions")
            if (interactions.isEmpty()) {
                Text(
                    text = "No recent interactions yet.",
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }
            interactions.forEach { interaction ->
                Text(
                    text = "${interaction.type} @ ${interaction.timestampMs}",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun TopPersonsCard(
    persons: List<PersonRecord>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Top Persons")
            if (persons.isEmpty()) {
                Text(
                    text = "No known persons yet.",
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }
            persons.forEach { person ->
                val normalizedFamiliarity = person.familiarityScore.coerceIn(0f, 1f)
                val familiarityText = String.format(
                    Locale.US,
                    "%.2f",
                    normalizedFamiliarity
                )
                Text(
                    text = "${person.displayName} - familiarity: $familiarityText",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun RecentObjectsCard(
    objects: List<ObjectRecord>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Recent Objects")
            if (objects.isEmpty()) {
                Text(
                    text = "No known objects seen yet.",
                    modifier = Modifier.padding(top = 8.dp)
                )
                return@Column
            }
            objects.forEach { obj ->
                val lastSeenAtMsText = obj.lastSeenAtMs?.toString() ?: "-"
                Text(
                    text = "${obj.name} - last seen: $lastSeenAtMsText",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
