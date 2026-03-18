package com.aipet.brain.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActivitiesSection(
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    careHint: String,
    cooldownHint: String?,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Care activities",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = careHint,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onFeedPet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Feed")
                }
                Button(
                    onClick = onPlayWithPet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Play")
                }
                Button(
                    onClick = onLetPetRest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Rest")
                }
            }
            if (cooldownHint != null) {
                Text(
                    text = cooldownHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
