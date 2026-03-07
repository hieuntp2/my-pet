package com.aipet.brain.app.ui.persons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import kotlinx.coroutines.launch

@Composable
fun PersonEditorScreen(
    personStore: PersonStore,
    personId: String?,
    onNavigateBack: () -> Unit,
    onPersonSaved: (String) -> Unit
) {
    val controller = remember(personStore) { PersonFlowController(personStore) }
    val coroutineScope = rememberCoroutineScope()
    var existingPerson by remember { mutableStateOf<PersonRecord?>(null) }
    var displayName by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(personId != null) }
    var saving by remember { mutableStateOf(false) }
    var formMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(controller, personId) {
        formMessage = null
        if (personId == null) {
            existingPerson = null
            displayName = ""
            nickname = ""
            loading = false
            return@LaunchedEffect
        }

        loading = true
        runCatching {
            controller.loadPerson(personId)
        }.onSuccess { loaded ->
            existingPerson = loaded
            if (loaded != null) {
                displayName = loaded.displayName
                nickname = loaded.nickname.orEmpty()
            } else {
                formMessage = "Person not found."
            }
        }.onFailure { error ->
            formMessage = "Failed to load person: ${error.message ?: "Unknown error"}"
        }
        loading = false
    }

    val isEditing = personId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = if (isEditing) "Edit Person" else "Create Person")

        if (loading) {
            Text(text = "Loading person...")
            Button(onClick = onNavigateBack) {
                Text(text = "Back")
            }
            return@Column
        }

        if (isEditing && existingPerson == null) {
            Text(text = formMessage ?: "Person not found.")
            Button(onClick = onNavigateBack) {
                Text(text = "Back")
            }
            return@Column
        }

        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Nickname") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (formMessage != null) {
            Text(text = formMessage.orEmpty())
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNavigateBack,
                enabled = !saving
            ) {
                Text(text = "Cancel")
            }
            Button(
                onClick = {
                    if (saving) {
                        return@Button
                    }
                    saving = true
                    formMessage = null
                    val input = PersonEditorInput(
                        displayName = displayName,
                        nickname = nickname
                    )
                    coroutineScope.launch {
                        val saveResult = runCatching {
                            if (isEditing) {
                                controller.updatePerson(
                                    existingPerson = existingPerson
                                        ?: return@runCatching PersonSaveResult.Failure("Person not found."),
                                    input = input
                                )
                            } else {
                                controller.createPerson(input)
                            }
                        }.getOrElse { error ->
                            PersonSaveResult.Failure(
                                message = error.message ?: "Save failed."
                            )
                        }
                        saving = false
                        when (saveResult) {
                            is PersonSaveResult.Success -> onPersonSaved(saveResult.personId)
                            is PersonSaveResult.ValidationError -> {
                                formMessage = saveResult.message
                            }
                            is PersonSaveResult.Failure -> {
                                formMessage = saveResult.message
                            }
                        }
                    }
                },
                enabled = !saving
            ) {
                Text(text = if (saving) "Saving..." else "Save")
            }
        }
    }
}
