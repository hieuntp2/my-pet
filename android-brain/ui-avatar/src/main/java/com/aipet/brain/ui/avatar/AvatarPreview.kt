package com.aipet.brain.ui.avatar

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.aipet.brain.ui.avatar.model.AvatarState

@Preview(showBackground = true)
@Composable
fun AvatarPreview() {
    AvatarFace(avatarState = AvatarState())
}
