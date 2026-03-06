package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.memory.events.EventStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventViewerScreen(
    eventStore: EventStore,
    onNavigateBack: () -> Unit
) {
    val events by eventStore.observeLatest(limit = 100).collectAsState(initial = emptyList())
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Event Viewer")
        Text(
            text = "Persisted events: ${events.size}",
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(text = "Back to Debug")
        }

        if (events.isEmpty()) {
            Text(text = "No persisted events yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = events, key = { it.eventId }) { event ->
                    EventRow(
                        event = event,
                        formattedTimestamp = formatter.format(Instant.ofEpochMilli(event.timestampMs))
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    event: EventEnvelope,
    formattedTimestamp: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Type: ${event.type}")
            Text(text = "Time: $formattedTimestamp")
            Text(text = "Id: ${event.eventId}")
            Text(text = "Payload: ${event.payloadJson}")
        }
    }
}
