package com.aipet.brain.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.model.PetBubblePayload

/**
 * Talking bubble that floats near the pet avatar.
 *
 * Visibility is driven by [payload]: non-null shows the bubble, null hides it.
 * Enter/exit use a soft fade + scale so the appearance feels like pet speech, not a toast.
 */
@Composable
fun PetTalkingBubble(
    payload: PetBubblePayload?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = payload != null,
        enter = fadeIn() + scaleIn(initialScale = 0.88f),
        exit = fadeOut() + scaleOut(targetScale = 0.88f),
        modifier = modifier
    ) {
        if (payload != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "\"${payload.text}\"",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
