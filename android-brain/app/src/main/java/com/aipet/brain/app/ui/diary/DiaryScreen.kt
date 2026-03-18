package com.aipet.brain.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardImportance
import com.aipet.brain.memory.daily.DailySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DiaryScreen(
    memoryCards: List<MemoryCard>,
    dailySummaries: List<DailySummary>,
    onNavigateBack: () -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(zoneId)
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val memoryCardsByDate = memoryCards.groupBy { card ->
        Instant.ofEpochMilli(card.timestampMs).atZone(zoneId).toLocalDate()
    }
    val dailySummaryByDate = dailySummaries.associateBy { it.date }
    val visibleDates = (memoryCardsByDate.keys + dailySummaryByDate.keys)
        .distinct()
        .sortedDescending()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Diary",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Recent pet memories and day summaries from saved events.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Visible memories: ${memoryCards.size} • Daily summaries: ${dailySummaries.size}",
            style = MaterialTheme.typography.bodySmall
        )
        Button(onClick = onNavigateBack) {
            Text(text = "Back")
        }

        if (memoryCards.isEmpty() && dailySummaries.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No pet memories yet",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Your diary will fill with real pet moments and daily summaries after greetings, pets, and care activities are saved.",
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                visibleDates.forEach { date ->
                    item(key = "header-$date") {
                        Text(
                            text = dateFormatter.format(date),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    val dailySummary = dailySummaryByDate[date]
                    if (dailySummary != null) {
                        item(key = "daily-summary-$date") {
                            DailySummaryCard(summary = dailySummary)
                        }
                    } else {
                        item(key = "missing-summary-$date") {
                            Text(
                                text = "No daily summary available for this day yet.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    items(
                        items = memoryCardsByDate[date].orEmpty(),
                        key = { card -> card.id }
                    ) { card ->
                        MemoryCardEntry(
                            card = card,
                            timestampFormatter = timestampFormatter
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(summary: DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily summary",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = summary.summary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Interactions ${summary.interactionCount} • Activities ${summary.activityCount}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (summary.highlights.isNotEmpty()) {
                Text(
                    text = "Highlights: ${summary.highlights.joinToString(separator = ", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            val stateSnapshotText = buildStateSnapshotText(summary)
            if (stateSnapshotText != null) {
                Text(
                    text = stateSnapshotText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MemoryCardEntry(
    card: MemoryCard,
    timestampFormatter: DateTimeFormatter
) {
    val notable = card.importance == MemoryCardImportance.NOTABLE
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (notable) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (notable) {
                Text(
                    text = card.notableMomentLabel ?: "Notable moment",
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = if (notable) 8.dp else 0.dp)
            )
            Text(
                text = card.summary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Event memory • ${card.category.name.lowercase()} • ${card.sourceEventType.name} • ${timestampFormatter.format(Instant.ofEpochMilli(card.timestampMs))}",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun buildStateSnapshotText(summary: DailySummary): String? {
    val parts = buildList {
        summary.dominantMood?.let { add("Mood ${it.lowercase().replace('_', ' ')}") }
        summary.energySnapshot?.let { add("energy $it") }
        summary.hungerSnapshot?.let { add("hunger $it") }
        summary.sleepinessSnapshot?.let { add("sleepiness $it") }
    }
    if (parts.isEmpty()) {
        return null
    }
    return "State snapshot: ${parts.joinToString(separator = " • ")}"
}
