package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "teach_samples",
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["created_at_ms"]),
        Index(value = ["observation_id"], unique = true)
    ]
)
data class TeachSampleEntity(
    @PrimaryKey
    @ColumnInfo(name = "sample_id")
    val sampleId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "observation_id")
    val observationId: String,
    @ColumnInfo(name = "observed_at_ms")
    val observedAtMs: Long,
    @ColumnInfo(name = "source")
    val source: String,
    @ColumnInfo(name = "note")
    val note: String?,
    @ColumnInfo(name = "image_uri")
    val imageUri: String,
    @ColumnInfo(name = "face_crop_uri")
    val faceCropUri: String? = null,
    @ColumnInfo(name = "quality_status")
    val qualityStatus: String,
    @ColumnInfo(name = "quality_flags")
    val qualityFlags: String,
    @ColumnInfo(name = "quality_note")
    val qualityNote: String?,
    @ColumnInfo(name = "quality_evaluated_at_ms")
    val qualityEvaluatedAtMs: Long?,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long
)
