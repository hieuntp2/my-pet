package com.aipet.brain.app.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aipet.brain.perception.camera.FrameDiagnostics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun CameraDiagnosticsOverlay(
    diagnostics: FrameDiagnostics?,
    faceCount: Int,
    topObjectLabel: String,
    topObjectConfidence: Float?,
    modifier: Modifier = Modifier,
    recognizedPersonLabel: String? = null,
) {
    val timestampFormatter = remember {
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    }

    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.68f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Camera Diagnostics",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Text(
                text = "Face count: $faceCount",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = "Person: ${recognizedPersonLabel ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (recognizedPersonLabel != null) Color(0xFF90EE90) else Color.White
            )
            Text(
                text = "Object: $topObjectLabel",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )
            Text(
                text = "Object confidence: ${
                    topObjectConfidence?.let { confidence ->
                        String.format(java.util.Locale.US, "%.3f", confidence)
                    } ?: "n/a"
                }",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White
            )

            if (diagnostics == null) {
                Text(
                    text = "Waiting for runtime diagnostics...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Frame: ${diagnostics.width} x ${diagnostics.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "Analyzer processing: ${diagnostics.processingDurationMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "Rotation: ${diagnostics.rotationDegrees} deg",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "Updated: ${timestampFormatter.format(Instant.ofEpochMilli(diagnostics.timestampMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "Processing metric is analyzer-side only.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
