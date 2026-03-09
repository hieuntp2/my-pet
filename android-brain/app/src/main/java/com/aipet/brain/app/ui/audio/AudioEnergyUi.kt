package com.aipet.brain.app.ui.audio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun AudioEnergyPanel(
    isCaptureActive: Boolean,
    metrics: AudioEnergyMetrics?,
    lastUpdatedAtMs: Long?
) {
    val displayedMetrics = if (isCaptureActive) {
        metrics
    } else {
        null
    }

    val rms = displayedMetrics?.rms ?: 0.0
    val peak = displayedMetrics?.peak ?: 0.0
    val smoothed = displayedMetrics?.smoothed ?: 0.0

    Text(
        text = "RMS: ${formatEnergyValue(displayedMetrics?.rms)}",
        modifier = Modifier.fillMaxWidth()
    )
    LinearProgressIndicator(
        progress = rms.toFloat().coerceIn(0f, 1f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
    )

    Text(
        text = "Peak: ${formatEnergyValue(displayedMetrics?.peak)}",
        modifier = Modifier.fillMaxWidth()
    )
    LinearProgressIndicator(
        progress = peak.toFloat().coerceIn(0f, 1f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
    )

    Text(
        text = "Smoothed: ${formatEnergyValue(displayedMetrics?.smoothed)}",
        modifier = Modifier.fillMaxWidth()
    )
    LinearProgressIndicator(
        progress = smoothed.toFloat().coerceIn(0f, 1f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
    )

    Text(
        text = if (isCaptureActive) {
            "Updated at: ${formatUpdatedAt(lastUpdatedAtMs)}"
        } else {
            "Updated at: - (capture not running)"
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatEnergyValue(value: Double?): String {
    if (value == null) {
        return "-"
    }
    return String.format(Locale.US, "%.4f", value)
}

private fun formatUpdatedAt(timestampMs: Long?): String {
    if (timestampMs == null) {
        return "-"
    }
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return formatter.format(Date(timestampMs))
}
