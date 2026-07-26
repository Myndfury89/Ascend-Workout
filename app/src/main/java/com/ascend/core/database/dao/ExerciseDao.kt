package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Upsert
    suspend fun upsert(entity: ExerciseEntity)

    @Upsert
    suspend fun upsertAll(entities: List<ExerciseEntity>)

    @Query("SELECT * FROM exercise ORDER BY category ASC, name ASC")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise ORDER BY category ASC, name ASC")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise WHERE id = :exerciseId")
    suspend fun getById(exerciseId: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercise")
    suspend fun count(): Int
}
