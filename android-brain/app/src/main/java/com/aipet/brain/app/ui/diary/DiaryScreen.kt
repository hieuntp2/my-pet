package com.aipet.brain.app.ui.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.ui.navigation.PetPrimaryDestination
import com.aipet.brain.app.ui.navigation.PetPrimaryNavigationBar
import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardCategory
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
    onNavigateHome: () -> Unit,
    onNavigateDiary: () -> Unit,
    onNavigateDebug: () -> Unit
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
        PetPrimaryNavigationBar(
            selectedDestination = PetPrimaryDestination.Diary,
            onNavigateHome = onNavigateHome,
            onNavigateDiary = onNavigateDiary,
            onNavigateDebug = onNavigateDebug
        )
        Text(
            text = "Diary",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Moments your pet remembered from real greetings, pets, and care.",
            style = MaterialTheme.typography.bodyMedium
        )

        when {
            memoryCards.isEmpty() && dailySummaries.isEmpty() -> {
                DiaryMessageCard(
                    title = "No memories yet",
                    body = "Your diary will start filling after your pet greets you, enjoys a pet, or shares a care moment."
                )
            }

            memoryCards.size <= 2 && dailySummaries.isEmpty() -> {
                DiaryMessageCard(
                    title = "A quiet start",
                    body = "Only a few moments have been saved so far. Keep interacting and the diary will grow naturally."
                )
            }

            else -> {
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

                        dailySummaryByDate[date]?.let { dailySummary ->
                            item(key = "daily-summary-$date") {
                                DailySummaryCard(summary = dailySummary)
                            }
                        }

                        if (dailySummaryByDate[date] == null && memoryCardsByDate[date].orEmpty().size <= 2) {
                            item(key = "quiet-day-$date") {
                                DiaryMessageCard(
                                    title = "A quiet day",
                                    body = "Only a small number of moments were saved for this day, so the diary is keeping it simple."
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
}

@Composable
private fun DiaryMessageCard(
    title: String,
    body: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                modifier = Modifier.padding(top = 8.dp)
            )
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
            if (summary.highlights.isNotEmpty()) {
                Text(
                    text = "Highlights: ${summary.highlights.joinToString(separator = ", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
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
                text = "${card.category.toReadableLabel()} • ${timestampFormatter.format(Instant.ofEpochMilli(card.timestampMs))}",
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

private fun MemoryCardCategory.toReadableLabel(): String {
    return when (this) {
        MemoryCardCategory.GREETING -> "Greeting"
        MemoryCardCategory.INTERACTION -> "Interaction"
        MemoryCardCategory.CARE -> "Care"
    }
}
