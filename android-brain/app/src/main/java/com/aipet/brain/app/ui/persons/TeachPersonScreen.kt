package com.aipet.brain.app.ui.persons

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aipet.brain.memory.persons.PersonStore
import com.aipet.brain.memory.teachsamples.TeachSampleStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private const val TEACH_SAMPLE_LIST_LIMIT = 25

private data class PendingTeachSampleCapture(
    val sampleNumber: Int,
    val sampleNote: String,
    val imageUri: Uri
)

@Composable
internal fun TeachPersonScreen(
    teachSessionId: String,
    teachSampleStore: TeachSampleStore,
    teachSampleImageStorage: TeachSampleImageStorage,
    personStore: PersonStore,
    teachPersonSaveController: TeachPersonSaveController,
    onCaptureSample: suspend (String, String) -> Result<TeachPersonCapturedObservation>,
    captureImageUriOverride: (suspend (String) -> Result<String>)? = null,
    initialCompletionConfirmedAtMs: Long? = null,
    onTeachSessionCompletionConfirmed: (suspend (Long) -> Boolean)? = null,
    onTeachSessionCompletionCleared: (suspend () -> Boolean)? = null,
    onNavigateBack: () -> Unit,
    onPersonSaved: (String) -> Unit
) {
    val personFlowController = remember(personStore) { PersonFlowController(personStore = personStore) }
    val teachFlowController = remember(personFlowController, teachSampleStore) {
        TeachPersonFlowController(
            personFlowController = personFlowController,
            teachSampleStore = teachSampleStore
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    val capturedSamples by teachFlowController.observeCapturedSamplesForSession(
        sessionId = teachSessionId,
        limit = TEACH_SAMPLE_LIST_LIMIT
    ).collectAsState(initial = emptyList())

    var uiState by remember {
        mutableStateOf(
            TeachPersonUiState(
                capturedSamples = capturedSamples,
                completionConfirmedAtMs = initialCompletionConfirmedAtMs
            )
        )
    }
    var pendingCapture by remember {
        mutableStateOf<PendingTeachSampleCapture?>(null)
    }
    val cameraCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { didCapture ->
        val activeCapture = pendingCapture
        if (activeCapture == null) {
            uiState = uiState.copy(
                isCapturing = false,
                message = "Capture failed: pending capture request is missing."
            )
            return@rememberLauncherForActivityResult
        }
        pendingCapture = null
        if (!didCapture) {
            uiState = uiState.copy(
                isCapturing = false,
                message = "Capture canceled."
            )
            return@rememberLauncherForActivityResult
        }

        coroutineScope.launch {
            val captureMessage = onCaptureSample(
                activeCapture.sampleNote,
                activeCapture.imageUri.toString()
            ).fold(
                onSuccess = { capturedObservation ->
                    val persisted = teachFlowController.recordCapturedSampleForSession(
                        sessionId = teachSessionId,
                        sample = capturedObservation
                    )
                    if (persisted) {
                        "Captured sample ${activeCapture.sampleNumber}."
                    } else {
                        "Capture failed: unable to persist sample."
                    }
                },
                onFailure = { error ->
                    "Capture failed: ${error.message ?: "Unknown error"}"
                }
            )
            uiState = uiState.copy(
                isCapturing = false,
                message = captureMessage
            )
        }
    }

    LaunchedEffect(capturedSamples) {
        uiState = uiState.copy(capturedSamples = capturedSamples)
    }

    LaunchedEffect(teachSessionId, initialCompletionConfirmedAtMs) {
        uiState = uiState.copy(completionConfirmedAtMs = initialCompletionConfirmedAtMs)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag(PersonsTestTags.TEACH_PERSON_SCREEN_ROOT)
    ) {
        Text(text = "Teach Person")
        Text(
            text = "Session: $teachSessionId",
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        Text(
            text = "Captured samples: ${uiState.capturedSamples.size}",
            modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_SAMPLE_COUNT_TEXT)
        )

        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = { updated ->
                uiState = uiState.copy(displayName = updated)
            },
            label = { Text("Display name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PersonsTestTags.TEACH_PERSON_DISPLAY_NAME_INPUT)
        )

        OutlinedTextField(
            value = uiState.nickname,
            onValueChange = { updated ->
                uiState = uiState.copy(nickname = updated)
            },
            label = { Text("Nickname (optional)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Button(
                onClick = {
                    if (uiState.isCapturing) {
                        return@Button
                    }
                    val nextSampleNumber = uiState.capturedSamples.size + 1
                    val sampleNote = "${TeachPersonFlowController.sessionTokenFor(teachSessionId)};sample=$nextSampleNumber"
                    uiState = uiState.copy(
                        isCapturing = true,
                        message = "Capturing sample $nextSampleNumber..."
                    )
                    val captureOverride = captureImageUriOverride
                    if (captureOverride != null) {
                        coroutineScope.launch {
                            val captureMessage = captureOverride(sampleNote).fold(
                                onSuccess = { imageUri ->
                                    onCaptureSample(sampleNote, imageUri).fold(
                                        onSuccess = { capturedObservation ->
                                            val persisted = teachFlowController.recordCapturedSampleForSession(
                                                sessionId = teachSessionId,
                                                sample = capturedObservation
                                            )
                                            if (persisted) {
                                                "Captured sample $nextSampleNumber."
                                            } else {
                                                "Capture failed: unable to persist sample."
                                            }
                                        },
                                        onFailure = { error ->
                                            "Capture failed: ${error.message ?: "Unknown error"}"
                                        }
                                    )
                                },
                                onFailure = { error ->
                                    "Capture failed: ${error.message ?: "Unknown error"}"
                                }
                            )
                            uiState = uiState.copy(
                                isCapturing = false,
                                message = captureMessage
                            )
                        }
                        return@Button
                    }

                    val captureToken = "${teachSessionId}_${System.currentTimeMillis()}_$nextSampleNumber"
                    val captureRequest = runCatching {
                        PendingTeachSampleCapture(
                            sampleNumber = nextSampleNumber,
                            sampleNote = sampleNote,
                            imageUri = teachSampleImageStorage.createCameraCaptureImageUri(
                                sampleCaptureId = captureToken
                            )
                        )
                    }.getOrElse { error ->
                        uiState = uiState.copy(
                            isCapturing = false,
                            message = "Capture failed: ${error.message ?: "Unable to create camera capture target."}"
                        )
                        return@Button
                    }

                    pendingCapture = captureRequest
                    uiState = uiState.copy(
                        isCapturing = true,
                        message = "Launching camera for sample $nextSampleNumber..."
                    )
                    runCatching {
                        cameraCaptureLauncher.launch(captureRequest.imageUri)
                    }.onFailure { error ->
                        pendingCapture = null
                        uiState = uiState.copy(
                            isCapturing = false,
                            message = "Capture failed: ${error.message ?: "Unable to launch camera."}"
                        )
                    }
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.testTag(PersonsTestTags.TEACH_PERSON_ADD_SAMPLE_BUTTON)
            ) {
                Text(text = if (uiState.isCapturing) "Capturing..." else "Capture Sample")
            }

            Button(
                onClick = onNavigateBack,
                enabled = !uiState.isCapturing && !uiState.isSaving
            ) {
                Text(text = "Back")
            }
        }

        val qualityGateResult = uiState.qualityGateResult
        val minimumSamplesReady = uiState.capturedSamples.size >= TeachPersonSaveController.MINIMUM_REQUIRED_SAMPLES
        val bestSampleSelection = uiState.bestSampleSelection
        val sessionSummary = uiState.teachSessionSummary
        val pruningSuggestions = uiState.pruningSuggestions
        val completionState = uiState.completionState

        // ---- PRIMARY ACTION ----
        Button(
            onClick = {
                if (uiState.isSaving) {
                    return@Button
                }
                if (!minimumSamplesReady) {
                    uiState = uiState.copy(
                        message = "Capture at least ${TeachPersonSaveController.MINIMUM_REQUIRED_SAMPLES} sample before saving."
                    )
                    return@Button
                }
                if (!qualityGateResult.canSaveTeachPerson) {
                    uiState = uiState.copy(
                        message = qualityGateResult.saveBlockedReason
                            ?: "Save blocked by quality gate."
                    )
                    return@Button
                }
                uiState = uiState.copy(isSaving = true, message = null)
                coroutineScope.launch {
                    // Auto-confirm session so user does not need to press a separate button first.
                    if (completionState.isReadyToComplete && !completionState.isCompleted) {
                        val confirmResult = teachFlowController.confirmTeachSessionCompletion(
                            capturedSamples = uiState.capturedSamples,
                            completionConfirmedAtMs = uiState.completionConfirmedAtMs
                        )
                        if (confirmResult is TeachSessionCompletionConfirmationResult.Confirmed) {
                            val completedAtMs = confirmResult.completionState.completedAtMs
                            if (completedAtMs != null) {
                                onTeachSessionCompletionConfirmed?.invoke(completedAtMs)
                                uiState = uiState.copy(completionConfirmedAtMs = completedAtMs)
                            }
                        }
                    }
                    val saveResult = runCatching {
                        teachPersonSaveController.saveTaughtPerson(
                            displayName = uiState.displayName,
                            nickname = uiState.nickname,
                            capturedSamples = uiState.capturedSamples
                        )
                    }.getOrElse { error ->
                        TeachPersonPersistenceResult.Failure(
                            message = error.message ?: "Save failed."
                        )
                    }

                    uiState = uiState.copy(isSaving = false)
                    when (saveResult) {
                        is TeachPersonPersistenceResult.Success -> onPersonSaved(saveResult.personId)
                        is TeachPersonPersistenceResult.ValidationError -> {
                            uiState = uiState.copy(message = saveResult.message)
                        }
                        is TeachPersonPersistenceResult.Failure -> {
                            uiState = uiState.copy(message = saveResult.message)
                        }
                    }
                }
            },
            enabled = !uiState.isCapturing && !uiState.isSaving && minimumSamplesReady && qualityGateResult.canSaveTeachPerson,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_SAVE_BUTTON)
        ) {
            Text(text = if (uiState.isSaving) "Saving..." else "Save Person")
        }

        // Error/status message always visible directly below Save.
        if (uiState.message != null) {
            Text(
                text = uiState.message.orEmpty(),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_MESSAGE_TEXT)
            )
        }

        // ---- Debug status lines ----
        Text(
            text = if (!minimumSamplesReady) {
                "Save gate: BLOCKED — need at least ${TeachPersonSaveController.MINIMUM_REQUIRED_SAMPLES} sample."
            } else if (qualityGateResult.canSaveTeachPerson) {
                "Save gate: PASS (${qualityGateResult.qualifiedSampleCount}/${qualityGateResult.requiredQualifiedSampleCount} qualified)."
            } else {
                "Save gate: BLOCKED — ${qualityGateResult.saveBlockedReason ?: "Quality gate not met."}"
            },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_SAVE_GATE_STATUS_TEXT)
        )
        if (minimumSamplesReady && !qualityGateResult.canSaveTeachPerson && qualityGateResult.failingSampleObservationIds.isNotEmpty()) {
            Text(
                text = "Gate failing IDs: ${qualityGateResult.failingSampleObservationIds.joinToString()}",
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_SAVE_GATE_FAILURE_SAMPLES_TEXT)
            )
        }
        Text(
            text = when (completionState.status) {
                TeachSessionCompletionStatus.BLOCKED -> "Completion: BLOCKED"
                TeachSessionCompletionStatus.READY_TO_COMPLETE -> "Completion: READY"
                TeachSessionCompletionStatus.COMPLETED -> "Completion: CONFIRMED"
            },
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_COMPLETION_STATUS_TEXT)
        )
        if (!completionState.isCompleted && completionState.completionBlockedReason != null) {
            Text(
                text = completionState.completionBlockedReason!!,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_COMPLETION_BLOCKED_REASON_TEXT)
            )
        }
        if (completionState.isCompleted && completionState.completedAtMs != null) {
            Text(
                text = "Session confirmed at: ${
                    formatter.format(Instant.ofEpochMilli(completionState.completedAtMs))
                }",
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_COMPLETION_CONFIRMED_AT_TEXT)
            )
        }
        if (bestSampleSelection.hasPreferredSample) {
            Text(
                text = "Preferred sample: ${bestSampleSelection.bestSampleId}",
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_BEST_SAMPLE_TEXT)
            )
        }
        Text(
            text = "Session: total=${sessionSummary.totalSampleCount}, qualified=${sessionSummary.qualifiedSampleCount}/${sessionSummary.requiredQualifiedSampleCount}, ready=${if (sessionSummary.canSave) "YES" else "NO"}",
            modifier = Modifier
                .padding(bottom = 4.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_SESSION_SUMMARY_TEXT)
        )
        Text(
            text = "Pruning: keep=${pruningSuggestions.keepSampleCount}, candidates=${if (pruningSuggestions.hasPruningCandidates) pruningSuggestions.pruningCandidateIds.joinToString() else "-"}",
            modifier = Modifier
                .padding(bottom = 8.dp)
                .testTag(PersonsTestTags.TEACH_PERSON_PRUNING_SUMMARY_TEXT)
        )
        // Secondary: let user manually mark the session complete without saving yet.
        if (!completionState.isCompleted) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        val confirmationResult = teachFlowController.confirmTeachSessionCompletion(
                            capturedSamples = uiState.capturedSamples,
                            completionConfirmedAtMs = uiState.completionConfirmedAtMs
                        )
                        when (confirmationResult) {
                            is TeachSessionCompletionConfirmationResult.Confirmed -> {
                                val completedAtMs = confirmationResult.completionState.completedAtMs
                                val persisted = if (completedAtMs != null && onTeachSessionCompletionConfirmed != null) {
                                    onTeachSessionCompletionConfirmed.invoke(completedAtMs)
                                } else {
                                    true
                                }
                                uiState = uiState.copy(
                                    completionConfirmedAtMs = completedAtMs,
                                    message = if (persisted) "Session marked complete." else "Unable to persist session confirmation."
                                )
                            }
                            is TeachSessionCompletionConfirmationResult.Blocked -> {
                                uiState = uiState.copy(
                                    message = confirmationResult.completionState.completionBlockedReason
                                        ?: "Teach session is not ready to complete."
                                )
                            }
                            is TeachSessionCompletionConfirmationResult.AlreadyCompleted -> {
                                uiState = uiState.copy(message = "Teach session is already confirmed.")
                            }
                        }
                    }
                },
                enabled = !uiState.isCapturing &&
                    !uiState.isSaving &&
                    completionState.isReadyToComplete &&
                    !completionState.isCompleted,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_CONFIRM_COMPLETION_BUTTON)
            ) {
                Text(text = "Mark Session Complete (optional)")
            }
        }

        if (uiState.capturedSamples.isEmpty()) {
            Text(text = "No captured samples yet. Capture at least one sample before saving.")
            return@Column
        }

        val warningSampleCount = sessionSummary.warningSampleCount
        val lowScoreSampleCount = uiState.capturedSamples.count { sample ->
            sample.scoredQuality.level == SampleQualityLevel.LOW
        }
        if (warningSampleCount > 0) {
            Text(
                text = "Advisory warnings on $warningSampleCount sample(s). Hard quality gate still decides save eligibility.",
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag(PersonsTestTags.TEACH_PERSON_WARNING_SUMMARY_TEXT)
            )
        }
        if (lowScoreSampleCount > 0) {
            Text(
                text = "Low quality score on $lowScoreSampleCount sample(s). Capture at least one face-crop sample with MEDIUM/HIGH level to pass save gate.",
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.testTag(PersonsTestTags.TEACH_PERSON_SAMPLE_LIST)
        ) {
            itemsIndexed(
                items = uiState.capturedSamples,
                key = { _, sample -> sample.observationId }
            ) { index, sample ->
                TeachPersonSampleCard(
                    sampleIndex = index + 1,
                    sample = sample,
                    isPreferred = bestSampleSelection.preferredSampleIds.contains(sample.observationId),
                    pruningSuggestion = pruningSuggestions.suggestionsByObservationId[sample.observationId],
                    formatter = formatter,
                    cleanupActionEnabled = !uiState.isCapturing && !uiState.isSaving,
                    onRemoveSample = { observationId ->
                        coroutineScope.launch {
                            when (val cleanupResult = teachFlowController.removeTeachSample(
                                sessionId = teachSessionId,
                                observationId = observationId,
                                completionConfirmedAtMs = uiState.completionConfirmedAtMs
                            )) {
                                is TeachSampleCleanupResult.Success -> {
                                    val completionResetMessage = if (cleanupResult.completionConfirmationReset) {
                                        " Completion confirmation was reset because session data changed."
                                    } else {
                                        ""
                                    }
                                    val completionClearPersisted = if (cleanupResult.completionConfirmationReset) {
                                        onTeachSessionCompletionCleared?.invoke() ?: true
                                    } else {
                                        true
                                    }
                                    val persistenceWarning = if (
                                        cleanupResult.completionConfirmationReset && !completionClearPersisted
                                    ) {
                                        " Completion reset could not be persisted."
                                    } else {
                                        ""
                                    }
                                    uiState = uiState.copy(
                                        completionConfirmedAtMs = cleanupResult.updatedCompletionConfirmedAtMs,
                                        message = "Removed sample ${cleanupResult.removedObservationId}.$completionResetMessage$persistenceWarning"
                                    )
                                }
                                is TeachSampleCleanupResult.ValidationError -> {
                                    uiState = uiState.copy(message = cleanupResult.message)
                                }
                                is TeachSampleCleanupResult.Failure -> {
                                    uiState = uiState.copy(message = cleanupResult.message)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TeachPersonSampleCard(
    sampleIndex: Int,
    sample: TeachPersonCapturedSample,
    isPreferred: Boolean,
    pruningSuggestion: SamplePruningSuggestion?,
    formatter: DateTimeFormatter,
    cleanupActionEnabled: Boolean,
    onRemoveSample: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PersonsTestTags.TEACH_PERSON_SAMPLE_ITEM)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Sample #$sampleIndex")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            ) {
                Button(
                    onClick = { onRemoveSample(sample.observationId) },
                    enabled = cleanupActionEnabled,
                    modifier = Modifier.testTag(PersonsTestTags.TEACH_PERSON_REMOVE_SAMPLE_BUTTON)
                ) {
                    Text(text = "Remove Sample")
                }
            }
            Text(text = "Preferred sample: ${if (isPreferred) "BEST" else "No"}")
            val retentionHint = pruningSuggestion?.retentionHint ?: SampleRetentionHint.KEEP
            Text(text = "Retention hint: ${retentionHint.toDisplayLabel()}")
            if (pruningSuggestion?.isPruningCandidate == true) {
                Text(text = "Pruning candidate: Yes")
            } else {
                Text(text = "Pruning candidate: No")
            }
            Text(
                text = "Pruning reasons: ${
                    pruningSuggestion?.reasons?.toDisplayText() ?: "-"
                }"
            )
            TeachPersonSamplePreview(imageUri = sample.imageUri)
            Text(text = "Face crop: ${if (sample.faceCropUri != null) "Available" else "Unavailable"}")
            if (sample.faceCropUri != null) {
                TeachPersonFaceCropPreview(faceCropUri = sample.faceCropUri)
            }
            Text(text = "Quality score: ${sample.scoredQuality.score}/100")
            Text(text = "Quality level: ${sample.scoredQuality.level.name}")
            Text(text = "Quality deductions: ${sample.scoredQuality.deductions.toDisplayText()}")
            if (sample.hasSoftWarning) {
                Text(text = "Advisory warnings:")
                sample.softWarnings.forEach { warning ->
                    Text(text = "- ${warning.warningMessage}")
                }
            } else {
                Text(text = "Advisory warnings: None")
            }
            Text(text = "Source: ${sample.source}")
            Text(text = "Quality status: ${sample.qualityMetadata.qualityStatus.name}")
            Text(text = "Quality flags: ${sample.qualityMetadata.qualityFlags.toDisplayText()}")
            Text(text = "Quality note: ${sample.qualityMetadata.note ?: "-"}")
            Text(
                text = "Quality evaluated at: ${
                    sample.qualityMetadata.evaluatedAtMs?.let { timestamp ->
                        formatter.format(Instant.ofEpochMilli(timestamp))
                    } ?: "-"
                }"
            )
            Text(
                text = "Observed at: ${
                    formatter.format(Instant.ofEpochMilli(sample.observedAtMs))
                }"
            )
            Text(text = "Observation ID: ${sample.observationId}")
            Text(text = "Image URI: ${sample.imageUri}")
            Text(text = "Face crop URI: ${sample.faceCropUri ?: "-"}")
            Text(text = "Note: ${sample.note ?: "-"}")
        }
    }
}

@Composable
private fun TeachPersonSamplePreview(imageUri: String) {
    val context = LocalContext.current
    val imageBitmap = remember(context, imageUri) {
        loadImageBitmap(context = context, imageUri = imageUri)
    }
    if (imageBitmap == null) {
        Text(text = "Preview unavailable")
        return
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = "Captured sample preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun TeachPersonFaceCropPreview(faceCropUri: String) {
    val context = LocalContext.current
    val imageBitmap = remember(context, faceCropUri) {
        loadImageBitmap(context = context, imageUri = faceCropUri)
    }
    if (imageBitmap == null) {
        Text(text = "Face crop preview unavailable")
        return
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = "Captured face crop preview",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(bottom = 8.dp)
    )
}

private fun loadImageBitmap(
    context: Context,
    imageUri: String
): ImageBitmap? {
    val parsedUri = Uri.parse(imageUri)
    val bitmap = runCatching {
        context.contentResolver.openInputStream(parsedUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: when (parsedUri.scheme) {
            null, "file" -> BitmapFactory.decodeFile(parsedUri.path)
            else -> null
        }
    }.getOrNull() ?: return null
    return bitmap.asImageBitmap()
}

private fun Set<*>.toDisplayText(): String {
    if (isEmpty()) {
        return "-"
    }
    return asSequence()
        .map { item -> item.toString() }
        .sorted()
        .joinToString(separator = ", ")
}

private fun SampleRetentionHint.toDisplayLabel(): String {
    return when (this) {
        SampleRetentionHint.KEEP -> "KEEP"
        SampleRetentionHint.RECAPTURE_SUGGESTED -> "RECAPTURE_SUGGESTED"
        SampleRetentionHint.REMOVE_SUGGESTED -> "REMOVE_SUGGESTED"
    }
}
