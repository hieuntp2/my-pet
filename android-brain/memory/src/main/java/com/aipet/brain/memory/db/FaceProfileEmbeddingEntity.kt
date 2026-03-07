package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "face_profile_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = FaceProfileEntity::class,
            parentColumns = ["profile_id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id"]),
        Index(value = ["created_at_ms"])
    ]
)
data class FaceProfileEmbeddingEntity(
    @PrimaryKey
    @ColumnInfo(name = "embedding_id")
    val embeddingId: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,
    @ColumnInfo(name = "vector_blob")
    val vectorBlob: ByteArray,
    @ColumnInfo(name = "vector_dim")
    val vectorDim: Int,
    @ColumnInfo(name = "metadata")
    val metadata: String?
)
