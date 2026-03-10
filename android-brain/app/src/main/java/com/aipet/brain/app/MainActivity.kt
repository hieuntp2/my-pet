package com.aipet.brain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.aipet.brain.app.debug.AppCrashReport
import com.aipet.brain.app.debug.AppCrashReporter
import com.aipet.brain.app.ui.PetBrainApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CrashAwareRoot {
                PetBrainApp()
            }
        }
    }
}

@Composable
private fun CrashAwareRoot(appContent: @Composable () -> Unit) {
    val appContext = LocalContext.current.applicationContext
    val clipboardManager = LocalClipboardManager.current
    var persistedCrash by remember { mutableStateOf(AppCrashReporter.latest(appContext)) }
    var shouldLaunchApp by rememberSaveable(
        persistedCrash?.occurredAtMs,
        persistedCrash?.summary
    ) {
        mutableStateOf(persistedCrash == null)
    }

    if (!shouldLaunchApp && persistedCrash != null) {
        val report = persistedCrash ?: return
        MaterialTheme {
            CrashReportScreen(
                report = report,
                onCopyStackTrace = {
                    clipboardManager.setText(AnnotatedString(report.stackTrace))
                },
                onOpenApp = {
                    AppCrashReporter.clear(appContext)
                    persistedCrash = null
                    shouldLaunchApp = true
                }
            )
        }
        return
    }

    appContent()
}

@Composable
private fun CrashReportScreen(
    report: AppCrashReport,
    onCopyStackTrace: () -> Unit,
    onOpenApp: () -> Unit
) {
    val occurredAtLabel = remember(report.occurredAtMs) {
        report.occurredAtMs.toDebugTimestamp()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "App crash captured on previous run",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(text = "Time: $occurredAtLabel")
            Text(text = "Thread: ${report.threadName}")
            Text(text = "Source: ${report.source}")
            Text(text = "Summary: ${report.summary}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenApp) {
                    Text("Open app")
                }
                OutlinedButton(onClick = onCopyStackTrace) {
                    Text("Copy stack trace")
                }
            }
            SelectionContainer {
                Text(
                    text = report.stackTrace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun Long.toDebugTimestamp(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
    return formatter.format(Date(this))
}
