package com.aipet.brain.perception.camera

data class FrameDiagnostics(
    val timestampMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val format: Int,
    val processingDurationMs: Long
)
