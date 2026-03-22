package com.aipet.brain.app.ui.perception

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** Sealed type representing what the pet has spotted that is unknown. */
sealed class TeachUnknownTarget {
    data class UnknownFace(val thumbnail: Bitmap?) : TeachUnknownTarget()
    data class UnknownObject(val canonicalLabel: String, val confidence: Float, val thumbnail: Bitmap? = null) : TeachUnknownTarget()
}

/**
 * A floating dialog shown when the background camera notices an unknown face or object.
 * The user can type a name and tap Save, or dismiss to skip (with cooldown applied by the caller).
 */
@Composable
fun TeachUnknownDialog(
    target: TeachUnknownTarget,
    onSave: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameField by remember { mutableStateOf(TextFieldValue("")) }

    val title = when (target) {
        is TeachUnknownTarget.UnknownFace -> "Who is this?"
        is TeachUnknownTarget.UnknownObject -> "What is this?"
    }
    val hint = when (target) {
        is TeachUnknownTarget.UnknownFace -> "Enter a name for this person"
        is TeachUnknownTarget.UnknownObject -> "Enter a name for this object"
    }
    val subtitle = when (target) {
        is TeachUnknownTarget.UnknownFace ->
            "The pet spotted an unfamiliar face. Give them a name so the pet can remember."
        is TeachUnknownTarget.UnknownObject ->
            "The pet sees \"${target.canonicalLabel}\" " +
                "(${(target.confidence * 100).toInt()}% confident). Give it a name so the pet can remember."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail row — show for both faces and objects if available
                val thumbnail = when (target) {
                    is TeachUnknownTarget.UnknownFace -> target.thumbnail
                    is TeachUnknownTarget.UnknownObject -> target.thumbnail
                }
                if (thumbnail != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = when (target) {
                                is TeachUnknownTarget.UnknownFace -> "Detected face"
                                is TeachUnknownTarget.UnknownObject -> "Detected object"
                            },
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    label = { Text(hint) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = nameField.text.trim()
                    if (trimmed.isNotBlank()) onSave(trimmed)
                },
                enabled = nameField.text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}
