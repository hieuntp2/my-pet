package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.brain.events.EventEnvelope

@Composable
fun DebugScreen(
    latestEvent: EventEnvelope?,
    onNavigateToHome: () -> Unit,
    onNavigateToEventViewer: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onEmitTestEvent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Debug")
        Text(
            text = "Latest event: ${latestEvent?.type ?: "None"}",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Latest id: ${latestEvent?.eventId ?: "-"}",
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Button(
            onClick = onEmitTestEvent,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(text = "Emit TEST_EVENT")
        }

        Button(
            onClick = onNavigateToEventViewer,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(text = "Open Event Viewer")
        }

        Button(
            onClick = onNavigateToCamera,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(text = "Open Camera")
        }

        Button(onClick = onNavigateToHome) {
            Text(text = "Back to Home")
        }
    }
}
