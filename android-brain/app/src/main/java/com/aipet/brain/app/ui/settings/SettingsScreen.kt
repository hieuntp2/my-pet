package com.aipet.brain.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.settings.CameraSelection

@Composable
fun SettingsScreen(
    selectedCamera: CameraSelection,
    soundEnabled: Boolean,
    isResetInProgress: Boolean,
    onSelectCamera: (CameraSelection) -> Unit,
    onSoundEnabledChange: (Boolean) -> Unit,
    onResetPetData: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(text = "Use ${option.displayName} Camera")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = "Pet reaction sound")
        Text(
            text = if (soundEnabled) {
                "Enabled"
            } else {
                "Muted"
            }
        )
        Switch(
            checked = soundEnabled,
            onCheckedChange = onSoundEnabledChange
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(text = "Reset pet data")
        Text(
            text = "Clears pet profile, pet state, diary/events, learned persons/objects, and onboarding."
        )

        if (!showResetConfirmation) {
            OutlinedButton(
                onClick = { showResetConfirmation = true },
                enabled = !isResetInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Reset pet data")
            }
        } else {
            Button(
                onClick = {
                    onResetPetData()
                    showResetConfirmation = false
                },
                enabled = !isResetInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isResetInProgress) {
                        "Resetting..."
                    } else {
                        "Confirm reset"
                    }
                )
            }
            OutlinedButton(
                onClick = { showResetConfirmation = false },
                enabled = !isResetInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Cancel")
            }
        }

        Button(
            onClick = onNavigateBack,
            enabled = !isResetInProgress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(text = "Back to Debug")
        }
    }
}
