package com.aipet.brain.app.ui.traits

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
import com.aipet.brain.brain.traits.TraitsSnapshot

@Composable
fun TraitsScreen(
    currentTraits: TraitsSnapshot?,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Traits")
        if (currentTraits == null) {
            Text(text = "No traits snapshot available.")
        } else {
            Text(text = "Curiosity: ${currentTraits.curiosity.formatTrait()}")
            Text(text = "Sociability: ${currentTraits.sociability.formatTrait()}")
            Text(text = "Energy: ${currentTraits.energy.formatTrait()}")
            Text(text = "Patience: ${currentTraits.patience.formatTrait()}")
            Text(text = "Boldness: ${currentTraits.boldness.formatTrait()}")
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "Back to Debug")
        }
    }
}

private fun Float.formatTrait(): String {
    return String.format("%.2f", this)
}
