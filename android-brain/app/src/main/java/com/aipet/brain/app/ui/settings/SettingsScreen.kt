package com.aipet.brain.app.ui.settings

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
import com.aipet.brain.app.settings.CameraSelection

@Composable
fun SettingsScreen(
    selectedCamera: CameraSelection,
    onSelectCamera: (CameraSelection) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Settings")
        Text(
            text = "Camera source for perception",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Text(
            text = "Current: ${selectedCamera.displayName}",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        CameraSelection.entries.forEach { option ->
            Button(
                onClick = { onSelectCamera(option) },
                enabled = option != selectedCamera,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(text = "Use ${option.displayName} Camera")
            }
        }

        Button(onClick = onNavigateBack) {
            Text(text = "Back to Debug")
        }
    }
}
