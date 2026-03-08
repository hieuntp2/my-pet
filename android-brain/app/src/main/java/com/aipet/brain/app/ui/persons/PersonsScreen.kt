package com.aipet.brain.app.ui.persons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
internal fun PersonsScreen(
    personStore: PersonStore,
    personSeenEventPublisher: PersonSeenEventPublisher,
    onNavigateBack: () -> Unit,
    onNavigateToTeachPerson: () -> Unit,
    onNavigateToCreatePerson: () -> Unit,
    onNavigateToEditPerson: (String) -> Unit
) {
    val controller = remember(personStore, personSeenEventPublisher) {
        PersonFlowController(
            personStore = personStore,
            personSeenEventPublisher = personSeenEventPublisher
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var persons by remember { mutableStateOf<List<PersonRecord>>(emptyList()) }
    var owner by remember { mutableStateOf<PersonRecord?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    suspend fun reloadPersons() {
        loading = true
        loadError = null
        runCatching {
            controller.loadPersons() to controller.loadOwner()
        }.onSuccess { loaded ->
            persons = loaded.first
            owner = loaded.second
        }.onFailure { error ->
            loadError = error.message ?: "Failed to load persons."
        }
        loading = false
    }

    LaunchedEffect(controller) {
        reloadPersons()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag(PersonsTestTags.PERSONS_SCREEN_ROOT)
    ) {
        Text(text = "Persons")
        Text(
            text = "Stored persons: ${persons.size}",
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        Text(
            text = "Current owner: ${owner?.displayName ?: "None"}",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Button(
                onClick = onNavigateToTeachPerson,
                modifier = Modifier.testTag(PersonsTestTags.PERSONS_TEACH_BUTTON)
            ) {
                Text(text = "Teach Person")
            }
            Button(onClick = onNavigateToCreatePerson) {
                Text(text = "Create Person")
            }
            Button(
                onClick = {
                    coroutineScope.launch {
                        reloadPersons()
                    }
                },
                enabled = !loading
            ) {
                Text(text = "Refresh")
            }
        }

        Button(onClick = onNavigateBack, modifier = Modifier.padding(bottom = 12.dp)) {
            Text(text = "Back to Debug")
        }

        if (owner != null) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        when (val result = controller.clearOwner()) {
                            is OwnerAssignmentResult.Success -> actionMessage = result.message
                            is OwnerAssignmentResult.Failure -> actionMessage = result.message
                        }
                        reloadPersons()
                    }
                },
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(text = "Clear Owner")
            }
        }

        if (actionMessage != null) {
            Text(
                text = actionMessage.orEmpty(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (loading) {
            Text(text = "Loading persons...")
            return@Column
        }

        if (loadError != null) {
            Text(text = "Load failed: ${loadError.orEmpty()}")
            return@Column
        }

        if (persons.isEmpty()) {
            Text(text = "No persons saved yet.")
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.testTag(PersonsTestTags.PERSONS_LIST_ROOT)
        ) {
            items(items = persons, key = { person -> person.personId }) { person ->
                PersonListItem(
                    person = person,
                    formatter = formatter,
                    onEditClick = { onNavigateToEditPerson(person.personId) },
                    onRecordSeenClick = {
                        coroutineScope.launch {
                            when (val result = controller.recordPersonSeen(person.personId)) {
                                is PersonSeenRecordResult.Success -> {
                                    actionMessage = "Recorded seen for ${result.updatedPerson.displayName}."
                                }
                                is PersonSeenRecordResult.Failure -> {
                                    actionMessage = result.message
                                }
                            }
                            reloadPersons()
                        }
                    },
                    onAssignOwnerClick = {
                        coroutineScope.launch {
                            when (val result = controller.assignOwner(person.personId)) {
                                is OwnerAssignmentResult.Success -> actionMessage = result.message
                                is OwnerAssignmentResult.Failure -> actionMessage = result.message
                            }
                            reloadPersons()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PersonListItem(
    person: PersonRecord,
    formatter: DateTimeFormatter,
    onEditClick: () -> Unit,
    onRecordSeenClick: () -> Unit,
    onAssignOwnerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PersonsTestTags.PERSONS_LIST_ROW)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Name: ${person.displayName}")
            Text(text = "Nickname: ${person.nickname ?: "-"}")
            Text(text = "Owner: ${if (person.isOwner) "Yes" else "No"}")
            Text(text = "Seen count: ${person.seenCount}")
            Text(text = "Last seen: ${person.lastSeenAtMs.toLastSeenText(formatter)}")
            Text(text = "Id: ${person.personId}")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Button(onClick = onEditClick) {
                    Text(text = "Edit")
                }
                Button(onClick = onRecordSeenClick) {
                    Text(text = "Record Seen")
                }
                Button(
                    onClick = onAssignOwnerClick,
                    enabled = !person.isOwner
                ) {
                    Text(text = if (person.isOwner) "Owner" else "Set as Owner")
                }
            }
        }
    }
}

private fun Long?.toLastSeenText(
    formatter: DateTimeFormatter
): String {
    return if (this == null) {
        "Never"
    } else {
        formatter.format(Instant.ofEpochMilli(this))
    }
}
