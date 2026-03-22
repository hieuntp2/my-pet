package com.aipet.brain.app.ui.persons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.profiles.FaceProfileStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
internal fun PersonsScreen(
    personStore: PersonStore,
    personSeenEventPublisher: PersonSeenEventPublisher,
    faceProfileStore: FaceProfileStore,
    eventBus: EventBus,
    onNavigateBack: () -> Unit,
    onNavigateToTeachPerson: () -> Unit,
    onNavigateToCreatePerson: () -> Unit,
    onNavigateToEditPerson: (String) -> Unit,
    onNavigateToPersonDetail: (String) -> Unit
) {
    val controller = remember(personStore, personSeenEventPublisher, faceProfileStore, eventBus) {
        PersonFlowController(
            personStore = personStore,
            personSeenEventPublisher = personSeenEventPublisher,
            faceProfileStore = faceProfileStore,
            eventBus = eventBus
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var personCards by remember { mutableStateOf<List<PersonDebugCard>>(emptyList()) }
    var owner by remember { mutableStateOf<com.aipet.brain.memory.persons.PersonRecord?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var deleteConfirmTarget by remember { mutableStateOf<PersonDebugCard?>(null) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    suspend fun reloadPersons() {
        loading = true
        loadError = null
        runCatching {
            controller.loadPersonCards() to controller.loadOwner()
        }.onSuccess { loaded ->
            personCards = loaded.first
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
        Text(text = "Persons", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Inspect stored people, their recognition data, and safely remove outdated entries.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Debug summary", style = MaterialTheme.typography.titleMedium)
                Text(text = "Stored persons: ${personCards.size}")
                Text(text = "Current owner: ${owner?.displayName ?: "None"}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNavigateToTeachPerson,
                        modifier = Modifier.testTag(PersonsTestTags.PERSONS_TEACH_BUTTON)
                    ) {
                        Text(text = "Teach Person")
                    }
                    OutlinedButton(onClick = onNavigateToCreatePerson) {
                        Text(text = "Create Person")
                    }
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch { reloadPersons() }
                        },
                        enabled = !loading
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "Refresh")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNavigateBack) {
                        Text(text = "Back to Debug")
                    }
                    if (owner != null) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    when (val result = controller.clearOwner()) {
                                        is OwnerAssignmentResult.Success -> actionMessage = result.message
                                        is OwnerAssignmentResult.Failure -> actionMessage = result.message
                                    }
                                    reloadPersons()
                                }
                            }
                        ) {
                            Text(text = "Clear Owner")
                        }
                    }
                }
            }
        }

        if (actionMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                Text(
                    text = actionMessage.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (deleteConfirmTarget != null) {
            AlertDialog(
                onDismissRequest = { deleteConfirmTarget = null },
                title = { Text(text = "Delete Person") },
                text = {
                    val target = deleteConfirmTarget
                    Text(
                        text = "Delete ${target?.person?.displayName}? This removes the stored person plus ${target?.profileCount ?: 0} linked face profiles and ${target?.embeddingCount ?: 0} embeddings."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = deleteConfirmTarget
                            deleteConfirmTarget = null
                            if (target != null) {
                                coroutineScope.launch {
                                    when (val result = controller.deletePerson(target.person.personId)) {
                                        is PersonDeleteResult.Success -> {
                                            actionMessage = "Deleted ${result.deletedDisplayName}. Removed ${result.profileCount} profiles and ${result.embeddingCount} embeddings."
                                        }
                                        is PersonDeleteResult.Failure -> actionMessage = result.message
                                    }
                                    reloadPersons()
                                }
                            }
                        }
                    ) {
                        Text(text = "Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deleteConfirmTarget = null }) {
                        Text(text = "Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loading) {
            Text(text = "Loading persons...")
            return@Column
        }

        if (loadError != null) {
            Text(text = "Load failed: ${loadError.orEmpty()}")
            return@Column
        }

        if (personCards.isEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "No stored persons yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Teach a person from the camera flow or create one manually to start building recognition memory.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.testTag(PersonsTestTags.PERSONS_LIST_ROOT)
        ) {
            items(items = personCards, key = { item -> item.person.personId }) { card ->
                PersonListItem(
                    card = card,
                    formatter = formatter,
                    onEditClick = { onNavigateToEditPerson(card.person.personId) },
                    onDeleteClick = { deleteConfirmTarget = card },
                    onRecordSeenClick = {
                        coroutineScope.launch {
                            when (val result = controller.recordPersonSeen(card.person.personId)) {
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
                    onOpenDetailClick = { onNavigateToPersonDetail(card.person.personId) },
                    onAssignOwnerClick = {
                        coroutineScope.launch {
                            when (val result = controller.assignOwner(card.person.personId)) {
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
    card: PersonDebugCard,
    formatter: DateTimeFormatter,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRecordSeenClick: () -> Unit,
    onOpenDetailClick: () -> Unit,
    onAssignOwnerClick: () -> Unit
) {
    val person = card.person
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PersonsTestTags.PERSONS_LIST_ROW)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = person.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = person.nickname?.let { "Nickname: $it" } ?: "No nickname saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (person.isOwner) {
                    AssistChip(onClick = {}, label = { Text("Owner") })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(label = "Seen", value = person.seenCount.toString())
                StatChip(label = "Familiarity", value = person.familiarityScore.toDisplayPercent())
                StatChip(label = "Profiles", value = card.profileCount.toString())
                StatChip(label = "Embeddings", value = card.embeddingCount.toString())
            }

            Divider()

            Text(text = "Created: ${formatter.format(Instant.ofEpochMilli(person.createdAtMs))}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Last seen: ${person.lastSeenAtMs.toLastSeenText(formatter)}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Preview: ${if (card.previewAvailable) "Available" else "No stored preview"}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Person ID: ${person.personId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onOpenDetailClick) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "Details")
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                }
                OutlinedButton(onClick = onRecordSeenClick) {
                    Text(text = "Record Seen")
                }
                OutlinedButton(onClick = onAssignOwnerClick, enabled = !person.isOwner) {
                    Text(text = if (person.isOwner) "Owner" else "Set as Owner")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color(0xFFB3261E))
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun Float.toDisplayPercent(): String {
    val value = if (isFinite()) coerceIn(0f, 1f) else 0f
    return "${(value * 100).toInt()}%"
}

private fun Long?.toLastSeenText(formatter: DateTimeFormatter): String {
    return this?.let { timestamp -> formatter.format(Instant.ofEpochMilli(timestamp)) } ?: "Never"
}
