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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.memory.events.EventStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private const val OBSERVATION_QUERY_LIMIT = 300
private const val OBSERVATION_UI_LIMIT = 50

@Composable
fun ObservationViewerScreen(
    eventStore: EventStore,
    onNavigateBack: () -> Unit,
    onRecordDebugObservation: suspend () -> Result<Unit>
) {
    val coroutineScope = rememberCoroutineScope()
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    val events by eventStore.observeLatest(limit = OBSERVATION_QUERY_LIMIT)
        .collectAsState(initial = emptyList())
    val observations = remember(events) {
        ObservationEventMapper.listRecent(
            events = events,
            limit = OBSERVATION_UI_LIMIT
        )
    }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Observations",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Recent observations: ${observations.size}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Button(
            onClick = {
                if (recording) {
                    return@Button
                }
                recording = true
                actionMessage = "Recording debug observation..."
                coroutineScope.launch {
                    val result = onRecordDebugObservation()
                    actionMessage = result.fold(
                        onSuccess = { "Debug person-like observation recorded." },
                        onFailure = { error ->
                            "Observation record failed: ${error.message ?: "Unknown error"}"
                        }
                    )
                    recording = false
                }
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(text = if (recording) "Recording..." else "Record Debug Observation")
        }

        DebugBackButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (actionMessage != null) {
            Text(
                text = actionMessage.orEmpty(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (observations.isEmpty()) {
            Text(text = "No observations recorded yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = observations, key = { record -> record.observationId }) { record ->
                    DebugRecordCard(
                        title = record.observationType,
                        subtitle = formatter.format(Instant.ofEpochMilli(record.observedAtMs)),
                        id = record.observationId
                    ) {
                        Text(
                            text = "Source: ${record.source}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (record.note != null) {
                            Text(
                                text = record.note,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
