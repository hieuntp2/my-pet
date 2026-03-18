package com.aipet.brain.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActivitiesSection(
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Care activities")
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
        Text(
            text = "Feed, play, or rest with your pet.",
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
