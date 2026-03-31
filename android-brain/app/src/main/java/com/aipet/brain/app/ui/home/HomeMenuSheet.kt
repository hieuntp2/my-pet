package com.aipet.brain.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipet.brain.app.BuildConfig

/**
 * Slide-up bottom sheet for the Home screen menu button.
 * Provides access to Debug and Diary screens, plus care shortcuts (Feed/Play/Rest).
 *
 * [onDismiss] fires when the scrim or outside area is tapped.
 * Uses a simple layered Box (no ModalBottomSheet API) for maximum BOM compatibility.
 */
@Composable
fun HomeMenuSheet(
    onDismiss: () -> Unit,
    onDebug: () -> Unit,
    onDiary: () -> Unit,
    onFeedPet: () -> Unit,
    onPlayWithPet: () -> Unit,
    onLetPetRest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(HomeColors.sheetDark)
                .clickable(enabled = false, onClick = {})
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Handle indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(HomeColors.sheetHandle)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (BuildConfig.DEBUG) {
                SheetMenuItem(label = "Debug", onClick = onDebug)
            }
            SheetMenuItem(label = "Diary", onClick = onDiary)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(1.dp)
                    .background(HomeColors.sheetDivider)
            )

            SheetMenuItem(label = "Feed", description = "Reduce hunger", onClick = onFeedPet)
            SheetMenuItem(
                label = "Play",
                description = "Catch the Spark — boost social & bond",
                onClick = onPlayWithPet
            )
            SheetMenuItem(
                label = "Rest",
                description = "Restore energy",
                onClick = onLetPetRest
            )
        }
    }
}

@Composable
private fun SheetMenuItem(
    label: String,
    description: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Column {
            Text(
                text = label,
                color = HomeColors.textPrimary,
                fontSize = 16.sp
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = HomeColors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
