package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Working Memory Debug")
        Text(
            text = "Runtime context (updates live from event flow).",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "currentPersonId: ${currentWorkingMemory.currentPersonId ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "currentObjectId: ${currentWorkingMemory.currentObjectId ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "lastStimulusTs: ${formatWorkingMemoryTimestamp(currentWorkingMemory.lastStimulusTs)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Note: unknown/non-taught object detections may show fallback label in currentObjectId.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp)
        )
        Button(onClick = onNavigateBack) {
            Text(text = "Back to Debug")
        }
    }
}

private fun formatWorkingMemoryTimestamp(timestampMs: Long?): String {
    val timestamp = timestampMs ?: return "-"
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return "${formatter.format(Date(timestamp))} ($timestamp)"
}
