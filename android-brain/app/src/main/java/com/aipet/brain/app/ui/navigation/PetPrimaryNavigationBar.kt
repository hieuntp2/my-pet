package com.aipet.brain.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class PetPrimaryDestination {
    Home,
    Diary,
    Debug
}

@Composable
fun PetPrimaryNavigationBar(
    selectedDestination: PetPrimaryDestination,
    onNavigateHome: () -> Unit,
    onNavigateDiary: () -> Unit,
    onNavigateDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavigationButton(
            label = "Home",
            selected = selectedDestination == PetPrimaryDestination.Home,
            onClick = onNavigateHome,
            modifier = Modifier.weight(1f)
        )
        NavigationButton(
            label = "Diary",
            selected = selectedDestination == PetPrimaryDestination.Diary,
            onClick = onNavigateDiary,
            modifier = Modifier.weight(1f)
        )
        NavigationButton(
            label = "Debug",
            selected = selectedDestination == PetPrimaryDestination.Debug,
            onClick = onNavigateDebug,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowScope.NavigationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    }
}
