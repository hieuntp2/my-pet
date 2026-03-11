package com.aipet.brain.app.ui.persons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun PersonDetailScreen(
    personId: String?,
    personStore: PersonStore,
    faceProfileStore: FaceProfileStore,
    onNavigateBack: () -> Unit
) {
    var person by remember(personId) { mutableStateOf<PersonRecord?>(null) }
    var embeddingCount by remember(personId) { mutableStateOf(0) }
    var loadMessage by remember(personId) { mutableStateOf("Loading person...") }
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    LaunchedEffect(personId, personStore, faceProfileStore) {
        if (personId.isNullOrBlank()) {
            person = null
            loadMessage = "No person selected."
            return@LaunchedEffect
        }
        runCatching {
            val loadedPerson = personStore.getById(personId)
            if (loadedPerson == null) {
                Triple<PersonRecord?, Int, String?>(null, 0, "Person not found.")
            } else {
                val profiles = faceProfileStore.listProfilesForPerson(personId)
                val count = profiles.sumOf { profile ->
                    faceProfileStore.listProfileEmbeddings(profile.profileId).size
                }
                Triple(loadedPerson, count, null)
            }
        }.onSuccess { result ->
            person = result.first
            embeddingCount = result.second
            loadMessage = result.third ?: ""
        }.onFailure { error ->
            person = null
            embeddingCount = 0
            loadMessage = error.message ?: "Failed to load person detail."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Person Detail")
        if (person == null) {
            Text(text = loadMessage)
            Button(onClick = onNavigateBack) { Text("Back") }
            return@Column
        }

        val loaded = person ?: return@Column
        Text(text = "Name: ${loaded.displayName}")
        Text(text = "Nickname: ${loaded.nickname ?: "-"}")
        Text(text = "Person ID: ${loaded.personId}")
        Text(text = "Owner: ${if (loaded.isOwner) "Yes" else "No"}")
        Text(text = "Seen count: ${loaded.seenCount}")
        Text(text = "Familiarity: ${(loaded.familiarityScore.coerceIn(0f,1f) * 100f).toInt()}%")
        Text(text = "Created at: ${formatter.format(Instant.ofEpochMilli(loaded.createdAtMs))}")
        Text(text = "Updated at: ${formatter.format(Instant.ofEpochMilli(loaded.updatedAtMs))}")
        Text(
            text = "Last seen: ${loaded.lastSeenAtMs?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "Never"}"
        )
        Text(text = "Embedding count: $embeddingCount")

        Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 12.dp)) {
            Text(text = "Back to Persons")
        }
    }
}
