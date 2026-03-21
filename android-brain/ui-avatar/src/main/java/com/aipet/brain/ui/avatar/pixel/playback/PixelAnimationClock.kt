package com.aipet.brain.ui.avatar.pixel.playback

fun interface PixelAnimationClock {
    fun nowMillis(): Long
}

object SystemPixelAnimationClock : PixelAnimationClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
