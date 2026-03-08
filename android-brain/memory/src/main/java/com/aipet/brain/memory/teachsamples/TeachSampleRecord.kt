package com.aipet.brain.memory.teachsamples

data class TeachSampleRecord(
    val sampleId: String,
    val sessionId: String,
    val observationId: String,
    val observedAtMs: Long,
    val source: String,
    val note: String?,
    val imageUri: String,
    val faceCropUri: String? = null,
    val qualityMetadata: SampleQualityMetadata = SampleQualityMetadata.default,
    val createdAtMs: Long
)
