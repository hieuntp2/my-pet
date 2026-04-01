package com.aipet.brain.memory.semantics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SemanticMemoryFactDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SemanticMemoryFactEntity)

    @Query("SELECT * FROM semantic_memory_facts WHERE fact_key = :key LIMIT 1")
    suspend fun loadByKey(key: String): SemanticMemoryFactEntity?

    @Query("SELECT * FROM semantic_memory_facts ORDER BY last_updated_at_ms DESC")
    suspend fun loadAll(): List<SemanticMemoryFactEntity>

    @Query("SELECT * FROM semantic_memory_facts WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    suspend fun loadWithMinConfidence(minConfidence: Float): List<SemanticMemoryFactEntity>

    @Query("DELETE FROM semantic_memory_facts WHERE fact_key = :key")
    suspend fun deleteByKey(key: String)
}
