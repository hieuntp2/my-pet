package com.aipet.brain.app.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DebugSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerStyle: TextStyle? = null,
    headerColor: Color? = null,
    contentSpacing: Dp = 2.dp,
    contentPadding: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            Text(
                text = title,
                style = headerStyle ?: MaterialTheme.typography.labelLarge,
                color = headerColor ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
        }
    }
}

@Composable
fun DebugLabelValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle? = null,
    valueStyle: TextStyle? = null,
    labelColor: Color? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = labelStyle ?: MaterialTheme.typography.bodySmall,
            color = labelColor ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = valueStyle ?: MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DebugBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    outlined: Boolean = true,
    label: String = "Back to Debug"
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(text = label)
        }
    }
}

@Composable
fun DebugRecordCard(
    title: String,
    subtitle: String? = null,
    id: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!id.isNullOrBlank()) {
                Text(
                    text = id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}
