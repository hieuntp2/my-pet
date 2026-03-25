package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.memory.WorkingMemory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkingMemoryDebugScreen(
    currentWorkingMemory: WorkingMemory,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Working Memory",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Live runtime context - updates with each event.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DebugSectionCard(title = "State", contentSpacing = 4.dp) {
            DebugLabelValueRow(label = "Person", value = currentWorkingMemory.currentPersonId ?: "-")
            DebugLabelValueRow(label = "Object", value = currentWorkingMemory.currentObjectId ?: "-")
            DebugLabelValueRow(
                label = "Last stimulus",
                value = formatWorkingMemoryTimestamp(currentWorkingMemory.lastStimulusTs)
            )
        }
        Text(
            text = "Unknown/non-taught object detections may show a fallback label in Object.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DebugBackButton(onClick = onNavigateBack)
    }
}

private fun formatWorkingMemoryTimestamp(timestampMs: Long?): String {
    val timestamp = timestampMs ?: return "-"
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return "${formatter.format(Date(timestamp))} ($timestamp)"
}
