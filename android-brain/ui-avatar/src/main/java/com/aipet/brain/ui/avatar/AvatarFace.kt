package com.aipet.brain.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.aipet.brain.ui.avatar.model.AvatarEmotion
import com.aipet.brain.ui.avatar.model.AvatarEyeState
import com.aipet.brain.ui.avatar.model.AvatarMouthState
import com.aipet.brain.ui.avatar.model.AvatarState

@Composable
fun AvatarFace(
    avatarState: AvatarState,
    modifier: Modifier = Modifier
) {
    val eyeState = when (avatarState.emotion) {
        AvatarEmotion.SLEEPY -> if (avatarState.eyeState == AvatarEyeState.OPEN) AvatarEyeState.HALF_OPEN else avatarState.eyeState
        AvatarEmotion.SURPRISED -> if (avatarState.eyeState == AvatarEyeState.CLOSED || avatarState.eyeState == AvatarEyeState.BLINK) AvatarEyeState.OPEN else avatarState.eyeState
        else -> avatarState.eyeState
    }

    val mouthState = when (avatarState.emotion) {
        AvatarEmotion.HAPPY -> if (avatarState.mouthState == AvatarMouthState.NEUTRAL) AvatarMouthState.SMILE else avatarState.mouthState
        AvatarEmotion.SURPRISED -> if (avatarState.mouthState == AvatarMouthState.NEUTRAL) AvatarMouthState.OPEN else avatarState.mouthState
        else -> avatarState.mouthState
    }

    val strokeColor = MaterialTheme.colorScheme.onSurface
    val faceColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .size(220.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(faceColor)
            .border(2.dp, strokeColor, RoundedCornerShape(28.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarEye(eyeState = eyeState, color = strokeColor)
                AvatarEye(eyeState = eyeState, color = strokeColor)
            }

            AvatarMouth(mouthState = mouthState, color = strokeColor)
        }
    }
}

@Composable
private fun AvatarEye(
    eyeState: AvatarEyeState,
    color: Color
) {
    when (eyeState) {
        AvatarEyeState.OPEN -> Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color)
        )

        AvatarEyeState.HALF_OPEN -> Box(
            modifier = Modifier
                .size(width = 24.dp, height = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(color)
        )

        AvatarEyeState.CLOSED,
        AvatarEyeState.BLINK -> Canvas(modifier = Modifier.size(width = 24.dp, height = 8.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AvatarMouth(
    mouthState: AvatarMouthState,
    color: Color
) {
    when (mouthState) {
        AvatarMouthState.NEUTRAL -> Canvas(modifier = Modifier.size(width = 72.dp, height = 18.dp)) {
            drawLine(
                color = color,
                start = Offset(8f, size.height / 2f),
                end = Offset(size.width - 8f, size.height / 2f),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        AvatarMouthState.SMILE -> Canvas(modifier = Modifier.size(width = 72.dp, height = 28.dp)) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
        }

        AvatarMouthState.OPEN -> Box(
            modifier = Modifier
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(50))
                .border(6.dp, color, RoundedCornerShape(50))
        )

        AvatarMouthState.SMALL_O -> Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .border(5.dp, color, CircleShape)
        )
    }
}
