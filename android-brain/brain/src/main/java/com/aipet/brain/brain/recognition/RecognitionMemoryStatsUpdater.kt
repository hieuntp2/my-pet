package com.aipet.brain.brain.recognition

data class RecognitionMemoryStatsUpdate(
    val personId: String,
    val timestampMs: Long,
    val seenCount: Int?
)

interface RecognitionMemoryStatsUpdater {
    suspend fun updatePersonSeenStats(
        personId: String,
        timestampMs: Long
    ): RecognitionMemoryStatsUpdate?

    object NoOp : RecognitionMemoryStatsUpdater {
        override suspend fun updatePersonSeenStats(
            personId: String,
            timestampMs: Long
        ): RecognitionMemoryStatsUpdate? {
            return null
        }
    }
}
