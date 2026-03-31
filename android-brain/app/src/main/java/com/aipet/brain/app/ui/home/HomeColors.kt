package com.aipet.brain.app.ui.home

import androidx.compose.ui.graphics.Color

/**
 * Named color tokens for the Home stage and its overlays.
 *
 * Keep all palette decisions here. Raw hex literals in HomeScreen, HomeMenuSheet,
 * HomeFxOverlay, SparkMiniGame, and HomeAmbientGlow should reference these instead.
 */
internal object HomeColors {
    // Stage backgrounds
    val stageDark = Color(0xFF080810)
    val sheetDark = Color(0xFF141428)

    // Accent
    val accentCyan = Color(0xFF7DDFFF)

    // Text
    val textPrimary = Color(0xFFE8F4FF)
    val textSecondary = Color(0x77E8F4FF)
    val iconTint = Color(0xCCE8F4FF)
    val bubbleText = Color(0xE0E8F4FF)

    // Sheet chrome
    val sheetHandle = Color(0x44FFFFFF)
    val sheetDivider = Color(0x22FFFFFF)

    // Hearts FX
    val heartPrimary = Color(0xFFFF6B9D)
    val heartLight = Color(0xFFFF8CB4)
    val heartVibrant = Color(0xFFFF4A80)
    val heartPale = Color(0xFFFFB3CC)
    val heartWarm = Color(0xFFFF9EC5)

    // ZZZ / Spark FX
    val zzzBlue = Color(0xFF9BB4E8)
    val exclamationYellow = Color(0xFFFFCC00)

    // Ambient glow palette (HomeAmbientGlow)
    val glowHungry = Color(0xFFFF8C42)
    val glowSleepy = Color(0xFF7B68EE)
    val glowLonely = Color(0xFF9BB4E8)
    val glowHappy = Color(0xFF7DDFFF)       // same as accentCyan
    val glowCurious = Color(0xFF64B5F6)
    val glowSad = Color(0xFF546E7A)
    val glowDefault = Color(0xFF3A7BD5)
}
