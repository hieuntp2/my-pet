package com.aipet.brain.app.ui.debug

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.memory.events.EventStore
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EventViewerScreen(
    eventStore: EventStore,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val eventJsonExporter = remember(context) {
        EventJsonExporter(context.applicationContext)
    }
    val eventJsonExportValidator = remember {
        EventJsonExportValidator()
    }
    val events by eventStore.observeLatest(limit = 100).collectAsState(initial = emptyList())
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var lastExportResult by remember { mutableStateOf<EventExportResult?>(null) }
    val formatter = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
    }
    val pickExportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri: Uri? ->
        if (selectedUri == null) {
            exportMessage = "File selection canceled."
            return@rememberLauncherForActivityResult
        }
        if (exporting || validating) {
            return@rememberLauncherForActivityResult
        }

        validating = true
        exportMessage = "Validating selected file..."
        coroutineScope.launch {
            val result = runCatching {
                val fileContent = readTextFromContentUri(
                    context = context,
                    uri = selectedUri
                )
                eventJsonExportValidator.validateJsonString(
                    source = selectedUri.toString(),
                    jsonContent = fileContent
                )
            }
            validating = false
            exportMessage = result.fold(
                onSuccess = { validationResult ->
                    validationResult.toDisplayText()
                },
                onFailure = { error ->
                    "Validation failed: ${error.message ?: "Unknown error"}"
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Event Viewer",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Persisted events: ${events.size}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Button(
                onClick = {
                    if (exporting || validating) {
                        return@Button
                    }
                    exporting = true
                    exportMessage = "Exporting events..."
                    coroutineScope.launch {
                        val result = runCatching {
                            eventJsonExporter.export(eventStore)
                        }
                        exporting = false
                        exportMessage = result.fold(
                            onSuccess = { exportResult ->
                                lastExportResult = exportResult
                                "Exported ${exportResult.eventCount} events to ${exportResult.fileName} " +
                                    "(${exportResult.fileSizeBytes} bytes)\nPath: ${exportResult.filePath}"
                            },
                            onFailure = { error ->
                                "Export failed: ${error.message ?: "Unknown error"}"
                            }
                        )
                    }
                },
                enabled = !exporting && !validating
            ) {
                Text(text = if (exporting) "Exporting..." else "Export Events")
            }

            Button(
                onClick = {
                    if (exporting || validating) {
                        return@Button
                    }
                    exporting = true
                    exportMessage = "Exporting events for sharing..."
                    coroutineScope.launch {
                        val result = runCatching {
                            eventJsonExporter.export(eventStore)
                        }.fold(
                            onSuccess = { exportResult ->
                                lastExportResult = exportResult
                                runCatching {
                                    launchShareSheet(context = context, exportResult = exportResult)
                                }.fold(
                                    onSuccess = {
                                        "Exported ${exportResult.eventCount} events and opened share sheet.\n" +
                                            "File: ${exportResult.fileName}\n" +
                                            "Path: ${exportResult.filePath}"
                                    },
                                    onFailure = { error ->
                                        "Share failed: ${error.message ?: "Unknown error"}"
                                    }
                                )
                            },
                            onFailure = { error ->
                                "Export failed: ${error.message ?: "Unknown error"}"
                            }
                        )

                        exporting = false
                        exportMessage = result
                    }
                },
                enabled = !exporting && !validating
            ) {
                Text(text = if (exporting) "Exporting..." else "Export & Share")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Button(
                onClick = {
                    if (exporting || validating) {
                        return@Button
                    }

                    val exportResult = lastExportResult
                    if (exportResult == null) {
                        exportMessage = "No exported file available. Export events first."
                        return@Button
                    }

                    validating = true
                    exportMessage = "Validating ${exportResult.fileName}..."
                    coroutineScope.launch {
                        val result = runCatching {
                            eventJsonExportValidator.validateFile(
                                file = File(exportResult.filePath)
                            )
                        }
                        validating = false
                        exportMessage = result.fold(
                            onSuccess = { validationResult ->
                                validationResult.toDisplayText()
                            },
                            onFailure = { error ->
                                "Validation failed: ${error.message ?: "Unknown error"}"
                            }
                        )
                    }
                },
                enabled = !exporting && !validating
            ) {
                Text(text = if (validating) "Validating..." else "Validate Last Export")
            }

            Button(
                onClick = {
                    if (exporting || validating) {
                        return@Button
                    }
                    pickExportFileLauncher.launch(arrayOf("application/json", "text/plain"))
                },
                enabled = !exporting && !validating
            ) {
                Text(text = if (validating) "Validating..." else "Validate Export File")
            }
        }
        DebugBackButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (exportMessage != null) {
            Text(
                text = exportMessage.orEmpty(),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (events.isEmpty()) {
            Text(text = "No events yet.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items = events, key = { it.eventId }) { event ->
                    EventRow(
                        event = event,
                        formattedTimestamp = formatter.format(Instant.ofEpochMilli(event.timestampMs))
                    )
                }
            }
        }
    }
}

private fun launchShareSheet(
    context: Context,
    exportResult: EventExportResult
) {
    val exportedFile = File(exportResult.filePath)
    val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportedFile
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        clipData = ClipData.newRawUri(exportResult.fileName, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Share exported events")
    try {
        context.startActivity(chooserIntent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("No app available to share JSON file", error)
    }
}

private suspend fun readTextFromContentUri(
    context: Context,
    uri: Uri
): String {
    return withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open selected file.")
        inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            reader.readText()
        }
    }
}

@Composable
private fun EventRow(
    event: EventEnvelope,
    formattedTimestamp: String
) {
    DebugRecordCard(
        title = event.type.name,
        subtitle = formattedTimestamp,
        id = event.eventId
    ) {
        Text(
            text = event.payloadJson.take(120).let {
                if (event.payloadJson.length > 120) "$it…" else it
            },
            style = MaterialTheme.typography.bodySmall
        )
    }
}
