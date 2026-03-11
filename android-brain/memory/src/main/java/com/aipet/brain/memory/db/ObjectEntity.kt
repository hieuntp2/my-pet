package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "objects",
    indices = [
        Index(value = ["name"]),
        Index(value = ["created_at_ms"]),
        Index(value = ["last_seen_at_ms"])
    ]
)
data class ObjectEntity(
    @PrimaryKey
    @ColumnInfo(name = "object_id")
    val objectId: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at_ms")
    val createdAtMs: Long,
    @ColumnInfo(name = "last_seen_at_ms")
    val lastSeenAtMs: Long?,
    @ColumnInfo(name = "seen_count")
    val seenCount: Int = 0
)
