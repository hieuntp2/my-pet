package com.aipet.brain.app.ui.audio.model

/**
 * Runtime metadata for one pet response clip.
 *
 * `durationMs` is optional because the current playback engine resolves duration from
 * the resource at load time; the manifest can be enriched later with fixed values.
 */
data class AudioClipMetadata(
    val category: AudioCategory,
    val resourceId: Int,
    val logicalClipName: String,
    val durationMs: Long? = null
)
