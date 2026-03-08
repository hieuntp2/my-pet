package com.aipet.brain.brain.traits

data class TraitsSnapshot(
    val snapshotId: String,
    val capturedAtMs: Long,
    val curiosity: Float,
    val sociability: Float,
    val energy: Float,
    val patience: Float,
    val boldness: Float
)
