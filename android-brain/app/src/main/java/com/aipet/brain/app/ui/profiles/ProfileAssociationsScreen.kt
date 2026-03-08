package com.aipet.brain.app.ui.profiles

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.reactions.PersonSeenEventPublisher
import com.aipet.brain.app.ui.debug.ObservationEventMapper
import com.aipet.brain.memory.events.EventStore
import com.aipet.brain.memory.profiles.FaceProfileEmbeddingRecord
import com.aipet.brain.memory.profiles.FaceProfileObservationLinkRecord
import com.aipet.brain.memory.profiles.FaceProfileRecord
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.memory.profiles.LinkedProfileSeenPropagationResult
import com.aipet.brain.memory.profiles.LinkedProfileSeenPropagationStatus
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private const val PROFILE_OBSERVATION_EVENTS_LIMIT = 300
private const val PROFILE_OBSERVATION_LIST_LIMIT = 80
private const val PROFILE_EMBEDDING_LIST_LIMIT = 20

@Composable
internal fun ProfileAssociationsScreen(
    faceProfileStore: FaceProfileStore,
    personStore: PersonStore,
    personSeenEventPublisher: PersonSeenEventPublisher,
    eventStore: EventStore,
    onNavigateBack: () -> Unit
) {
    val controller = remember(faceProfileStore, personStore, personSeenEventPublisher) {
        ProfileAssociationController(
            faceProfileStore = faceProfileStore,
            personStore = personStore,
            personSeenEventPublisher = personSeenEventPublisher
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    var profiles by remember { mutableStateOf<List<FaceProfileRecord>>(emptyList()) }
    var selectedProfileId by remember { mutableStateOf<String?>(null) }
    var linkedObservations by remember { mutableStateOf<List<FaceProfileObservationLinkRecord>>(emptyList()) }
    var profileEmbeddings by remember { mutableStateOf<List<FaceProfileEmbeddingRecord>>(emptyList()) }
    var persons by remember { mutableStateOf<List<PersonRecord>>(emptyList()) }
    var linkedPersonForSelectedProfile by remember { mutableStateOf<PersonRecord?>(null) }
    var linkedProfilesForSelectedPerson by remember { mutableStateOf<List<FaceProfileRecord>>(emptyList()) }
    var observationProfiles by remember { mutableStateOf<Map<String, List<FaceProfileRecord>>>(emptyMap()) }
    var loadingProfiles by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var embeddingInput by remember { mutableStateOf("0.12, 0.34, 0.56, 0.78") }
    var embeddingMetadataInput by remember { mutableStateOf("debug_manual_embedding") }
    val events by eventStore.observeLatest(limit = PROFILE_OBSERVATION_EVENTS_LIMIT)
        .collectAsState(initial = emptyList())
    val recentObservations = remember(events) {
        ObservationEventMapper.listRecent(
            events = events,
            limit = PROFILE_OBSERVATION_LIST_LIMIT
        )
    }
    val observationById = remember(recentObservations) {
        recentObservations.associateBy { observation -> observation.observationId }
    }
    val personsById = remember(persons) {
        persons.associateBy { person -> person.personId }
    }

    suspend fun refreshObservationProfiles() {
        if (recentObservations.isEmpty()) {
            observationProfiles = emptyMap()
            return
        }
        observationProfiles = recentObservations.associate { observation ->
            observation.observationId to controller.listProfilesForObservation(observation.observationId)
        }
    }

    suspend fun refreshProfiles() {
        loadingProfiles = true
        val loadedProfiles = controller.listProfiles()
        profiles = loadedProfiles
        val currentSelected = selectedProfileId
        selectedProfileId = when {
            loadedProfiles.isEmpty() -> null
            currentSelected == null -> loadedProfiles.first().profileId
            loadedProfiles.any { profile -> profile.profileId == currentSelected } -> currentSelected
            else -> loadedProfiles.first().profileId
        }
        loadingProfiles = false
        refreshObservationProfiles()
    }

    suspend fun refreshLinks() {
        linkedObservations = selectedProfileId?.let { profileId ->
            controller.listProfileObservations(profileId)
        } ?: emptyList()
    }

    suspend fun refreshPersons() {
        persons = controller.listPersons()
    }

    suspend fun refreshPersonResolutionState() {
        val profileId = selectedProfileId
        if (profileId == null) {
            linkedPersonForSelectedProfile = null
            linkedProfilesForSelectedPerson = emptyList()
            return
        }
        linkedPersonForSelectedProfile = controller.getPersonForProfile(profileId)
        linkedProfilesForSelectedPerson = linkedPersonForSelectedProfile?.let { person ->
            controller.listProfilesForPerson(person.personId)
        } ?: emptyList()
    }

    suspend fun refreshEmbeddings() {
        profileEmbeddings = selectedProfileId?.let { profileId ->
            controller.listProfileEmbeddings(profileId)
                .take(PROFILE_EMBEDDING_LIST_LIMIT)
        } ?: emptyList()
    }

    suspend fun bridgeMessage(result: LinkedProfileSeenPropagationResult): String {
        return when (result.status) {
            LinkedProfileSeenPropagationStatus.SUCCESS -> {
                refreshPersons()
                refreshPersonResolutionState()
                refreshObservationProfiles()
                val updatedPerson = result.person
                if (updatedPerson != null) {
                    "Updated seen state for ${updatedPerson.displayName}: seenCount=${updatedPerson.seenCount}, lastSeenAt=${updatedPerson.lastSeenAtMs}."
                } else {
                    "Linked profile observation propagated to known person seen state."
                }
            }
            LinkedProfileSeenPropagationStatus.PROFILE_NOT_FOUND ->
                "Bridge failed: selected profile was not found."
            LinkedProfileSeenPropagationStatus.OBSERVATION_NOT_LINKED ->
                "Bridge skipped: observation is not linked to the selected profile."
            LinkedProfileSeenPropagationStatus.PROFILE_UNRESOLVED ->
                "Bridge skipped: selected profile is unresolved and not linked to a known person."
            LinkedProfileSeenPropagationStatus.PERSON_NOT_FOUND ->
                "Bridge failed: linked person record was not found."
        }
    }

    fun parseEmbeddingInput(raw: String): List<Float>? {
        val tokens = raw.split(",")
            .map { token -> token.trim() }
            .filter { token -> token.isNotBlank() }
        if (tokens.isEmpty()) {
            return null
        }
        val parsed = tokens.map { token -> token.toFloatOrNull() ?: return null }
        return if (parsed.all { value -> value.isFinite() }) parsed else null
    }

    LaunchedEffect(controller) {
        refreshProfiles()
        refreshLinks()
        refreshEmbeddings()
        refreshPersons()
        refreshPersonResolutionState()
    }

    LaunchedEffect(selectedProfileId) {
        refreshLinks()
        refreshEmbeddings()
        refreshPersonResolutionState()
    }

    LaunchedEffect(recentObservations) {
        refreshObservationProfiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Profile Associations")
        Text(
            text = "Profiles: ${profiles.size}",
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val candidateIndex = profiles.size + 1
                        val created = runCatching {
                            controller.createProfileCandidate(
                                label = "Candidate $candidateIndex",
                                note = "Created from profile debug flow"
                            )
                        }
                        actionMessage = created.fold(
                            onSuccess = { profile ->
                                selectedProfileId = profile.profileId
                                refreshProfiles()
                                refreshLinks()
                                refreshEmbeddings()
                                refreshPersonResolutionState()
                                "Created profile candidate ${profile.profileId}."
                            },
                            onFailure = { error ->
                                "Failed to create profile candidate: ${error.message ?: "Unknown error"}"
                            }
                        )
                    }
                }
            ) {
                Text(text = "Create Profile Candidate")
            }

            Button(onClick = onNavigateBack) {
                Text(text = "Back to Debug")
            }
        }

        if (actionMessage != null) {
            Text(
                text = actionMessage.orEmpty(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (loadingProfiles) {
            Text(text = "Loading profiles...")
            return@Column
        }

        if (profiles.isEmpty()) {
            Text(text = "No profile candidates yet.")
        } else {
            Text(
                text = "Select a profile candidate:",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(items = profiles, key = { profile -> profile.profileId }) { profile ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Profile ID: ${profile.profileId}")
                            Text(text = "Status: ${profile.status}")
                            Text(
                                text = if (profile.isResolved) {
                                    "Resolution: Linked"
                                } else {
                                    "Resolution: Unresolved"
                                }
                            )
                            Text(text = "Linked person ID: ${profile.linkedPersonId ?: "-"}")
                            Text(text = "Label: ${profile.label ?: "-"}")
                            Text(text = "Note: ${profile.note ?: "-"}")
                            Button(
                                onClick = { selectedProfileId = profile.profileId },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(
                                    text = if (selectedProfileId == profile.profileId) {
                                        "Selected"
                                    } else {
                                        "Select"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Manual profile-to-person resolution:",
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        if (selectedProfileId == null) {
            Text(text = "Select a profile to link it to a known person.")
        } else {
            Text(text = "Selected profile: $selectedProfileId")
            val linkedPerson = linkedPersonForSelectedProfile
            Text(
                text = if (linkedPerson != null) {
                    "Linked person: ${linkedPerson.displayName} (${linkedPerson.personId})"
                } else {
                    "Linked person: none"
                },
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            if (linkedPerson != null) {
                Text(
                    text = "Linked person seen count: ${linkedPerson.seenCount}",
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Linked person last seen: ${linkedPerson.lastSeenAtMs.toDateTimeText(formatter)}",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (linkedPerson != null) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val profileId = selectedProfileId
                            if (profileId == null) {
                                actionMessage = "Select a profile before unlinking."
                                return@launch
                            }
                            val unlinked = controller.unlinkProfileFromPerson(profileId)
                            actionMessage = if (unlinked) {
                                refreshProfiles()
                                refreshPersonResolutionState()
                                "Unlinked profile $profileId from known person."
                            } else {
                                "Unlink failed for profile $profileId."
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(text = "Unlink Selected Profile")
                }
            }

            Button(
                onClick = {
                    coroutineScope.launch {
                        refreshPersons()
                        actionMessage = "Refreshed known persons list."
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(text = "Refresh Persons")
            }

            if (persons.isEmpty()) {
                Text(text = "No known persons available. Create one in Persons screen first.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(items = persons, key = { person -> person.personId }) { person ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Person: ${person.displayName}")
                                Text(text = "Person ID: ${person.personId}")
                                Text(text = "Owner: ${if (person.isOwner) "Yes" else "No"}")
                                Text(text = "Seen count: ${person.seenCount}")
                                Text(text = "Last seen: ${person.lastSeenAtMs.toDateTimeText(formatter)}")
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val profileId = selectedProfileId
                                            if (profileId == null) {
                                                actionMessage = "Select a profile before linking to person."
                                                return@launch
                                            }
                                            val linked = controller.linkProfileToPerson(
                                                profileId = profileId,
                                                personId = person.personId
                                            )
                                            actionMessage = if (linked) {
                                                refreshProfiles()
                                                refreshPersonResolutionState()
                                                "Linked profile $profileId to person ${person.displayName}."
                                            } else {
                                                "Manual link failed. Check selected profile and person."
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(text = "Link Selected Profile")
                                }
                            }
                        }
                    }
                }
            }

            if (linkedPerson != null) {
                Text(
                    text = "Profiles linked to ${linkedPerson.displayName}: ${linkedProfilesForSelectedPerson.size}",
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                if (linkedProfilesForSelectedPerson.isEmpty()) {
                    Text(text = "No linked profiles found for this person.")
                } else {
                    linkedProfilesForSelectedPerson.take(10).forEach { linkedProfile ->
                        Text(text = "- ${linkedProfile.profileId}")
                    }
                }
            }
        }

        Text(
            text = "Recent observations:",
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        if (recentObservations.isEmpty()) {
            Text(text = "No observations available to link yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(items = recentObservations, key = { observation -> observation.observationId }) { observation ->
                    val linkedProfilesForObservation = observationProfiles[observation.observationId].orEmpty()
                    val directlyBridgeableProfile = linkedProfilesForObservation.singleOrNull()
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Observation ID: ${observation.observationId}")
                            Text(text = "Type: ${observation.observationType}")
                            Text(text = "Source: ${observation.source}")
                            Text(
                                text = "Observed at: ${
                                    formatter.format(Instant.ofEpochMilli(observation.observedAtMs))
                                }"
                            )
                            Text(text = "Note: ${observation.note ?: "-"}")
                            Text(
                                text = when (linkedProfilesForObservation.size) {
                                    0 -> "Linked profiles: none"
                                    1 -> "Linked profile: ${linkedProfilesForObservation.first().profileId}"
                                    else -> "Linked profiles: ${linkedProfilesForObservation.joinToString { profile -> profile.profileId }}"
                                },
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            linkedProfilesForObservation.forEach { linkedProfile ->
                                val linkedPerson = linkedProfile.linkedPersonId?.let { personId ->
                                    personsById[personId]
                                }
                                Text(
                                    text = if (linkedPerson != null) {
                                        "Profile ${linkedProfile.profileId} resolves to ${linkedPerson.displayName}."
                                    } else {
                                        "Profile ${linkedProfile.profileId} is unresolved."
                                    }
                                )
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val profileId = selectedProfileId
                                        if (profileId == null) {
                                            actionMessage = "Select a profile before linking observations."
                                            return@launch
                                        }
                                        val linked = controller.linkObservationToProfile(
                                            observationId = observation.observationId,
                                            profileId = profileId
                                        )
                                        actionMessage = if (linked) {
                                            refreshLinks()
                                            refreshObservationProfiles()
                                            "Linked observation ${observation.observationId} to profile $profileId."
                                        } else {
                                            "Link failed. Check selected profile and observation."
                                        }
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(text = "Link to Selected Profile")
                            }
                            if (directlyBridgeableProfile != null) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            actionMessage = bridgeMessage(
                                                controller.recordKnownPersonSeenFromLinkedProfile(
                                                    profileId = directlyBridgeableProfile.profileId,
                                                    observationId = observation.observationId
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(text = "Run Linked Profile Bridge")
                                }
                            } else if (linkedProfilesForObservation.size > 1) {
                                Text(
                                    text = "Multiple profiles link to this observation. Use explicit profile selection below before propagating.",
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Selected profile links:",
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        if (selectedProfileId == null) {
            Text(text = "No profile selected.")
        } else if (linkedObservations.isEmpty()) {
            Text(text = "No linked observations for selected profile.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(items = linkedObservations, key = { link ->
                    "${link.profileId}:${link.observationId}"
                }) { link ->
                    val observation = observationById[link.observationId]
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "Observation ID: ${link.observationId}")
                            Text(
                                text = "Linked at: ${
                                    formatter.format(Instant.ofEpochMilli(link.linkedAtMs))
                                }"
                            )
                            Text(text = "Observed type: ${observation?.observationType ?: "Unknown"}")
                            Text(text = "Observed source: ${observation?.source ?: "Unknown"}")
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val profileId = selectedProfileId
                                        if (profileId == null) {
                                            actionMessage = "Select a profile before propagating person seen updates."
                                            return@launch
                                        }
                                        val bridgeResult = controller.recordKnownPersonSeenFromLinkedProfile(
                                            profileId = profileId,
                                            observationId = link.observationId
                                        )
                                        actionMessage = bridgeMessage(bridgeResult)
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(text = "Propagate to Known Person Seen")
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Selected profile embeddings:",
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        if (selectedProfileId == null) {
            Text(text = "Select a profile to attach embeddings.")
        } else {
            OutlinedTextField(
                value = embeddingInput,
                onValueChange = { updated -> embeddingInput = updated },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = { Text("Embedding values (comma-separated)") },
                singleLine = true
            )
            OutlinedTextField(
                value = embeddingMetadataInput,
                onValueChange = { updated -> embeddingMetadataInput = updated },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                label = { Text("Embedding metadata (optional)") },
                singleLine = true
            )
            Button(
                onClick = {
                    coroutineScope.launch {
                        val profileId = selectedProfileId
                        if (profileId == null) {
                            actionMessage = "Select a profile before attaching embeddings."
                            return@launch
                        }
                        val values = parseEmbeddingInput(embeddingInput)
                        if (values == null) {
                            actionMessage = "Invalid embedding values. Use comma-separated float numbers."
                            return@launch
                        }
                        val createdEmbedding = controller.addEmbeddingToProfile(
                            profileId = profileId,
                            values = values,
                            metadata = embeddingMetadataInput
                        )
                        actionMessage = if (createdEmbedding != null) {
                            refreshEmbeddings()
                            "Attached embedding ${createdEmbedding.embeddingId} to profile $profileId."
                        } else {
                            "Embedding attach failed. Check selected profile and values."
                        }
                    }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(text = "Attach Embedding")
            }
            Text(
                text = "Embedding count: ${profileEmbeddings.size}",
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (profileEmbeddings.isEmpty()) {
                Text(text = "No embeddings attached for selected profile.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(items = profileEmbeddings, key = { embedding ->
                        embedding.embeddingId
                    }) { embedding ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = "Embedding ID: ${embedding.embeddingId}")
                                Text(text = "Dim: ${embedding.vectorDim}")
                                Text(
                                    text = "Created at: ${
                                        formatter.format(Instant.ofEpochMilli(embedding.createdAtMs))
                                    }"
                                )
                                Text(text = "Metadata: ${embedding.metadata ?: "-"}")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Long?.toDateTimeText(formatter: DateTimeFormatter): String {
    return if (this == null) {
        "Never"
    } else {
        formatter.format(Instant.ofEpochMilli(this))
    }
}
