package com.aipet.brain.app.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PetNamingOnboardingScreen(
    currentName: String,
    draftName: String,
    onDraftNameChanged: (String) -> Unit,
    onConfirmName: () -> Unit,
    onKeepCurrentName: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome to your pet",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Your pet is already awake and ready. Give it a name now, or keep \"$currentName\" and start playing immediately."
        )
        OutlinedTextField(
            value = draftName,
            onValueChange = onDraftNameChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = {
                Text(text = "Pet name")
            }
        )
        Text(
            text = "You can keep the current name and start playing immediately.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = onConfirmName,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Start with this name")
        }
        Button(
            onClick = onKeepCurrentName,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Keep \"$currentName\"")
        }
    }
}
