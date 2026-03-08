package com.aipet.brain.memory.teachsamples

enum class SampleQualityStatus {
    UNASSESSED,
    LIMITED_SOURCE
}

enum class SampleQualityFlag {
    DEBUG_GENERATED_IMAGE,
    NOT_CAMERA_FRAME
}

data class SampleQualityMetadata(
    val qualityStatus: SampleQualityStatus,
    val qualityFlags: Set<SampleQualityFlag> = emptySet(),
    val note: String? = null,
    val evaluatedAtMs: Long? = null
) {
    companion object {
        val default: SampleQualityMetadata = SampleQualityMetadata(
            qualityStatus = SampleQualityStatus.UNASSESSED
        )
    }
}
