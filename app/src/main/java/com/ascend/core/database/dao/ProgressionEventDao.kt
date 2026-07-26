package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ascend.core.database.entity.ProgressionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressionEventDao {
    /** Returns row id, or -1 when the (batchId, sequence) guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(entity: ProgressionEventEntity): Long

    @Query(
        "SELECT * FROM progression_event WHERE userId = :userId AND consumedAt IS NULL " +
            "ORDER BY createdAt ASC, batchId ASC, sequence ASC",
    )
    fun observePending(userId: String): Flow<List<ProgressionEventEntity>>

    @Query(
        "SELECT * FROM progression_event WHERE userId = :userId AND consumedAt IS NULL " +
            "ORDER BY createdAt ASC, batchId ASC, sequence ASC",
    )
    suspend fun getPending(userId: String): List<ProgressionEventEntity>

    @Query("UPDATE progression_event SET consumedAt = :consumedAt WHERE id IN (:ids)")
    suspend fun markConsumed(
        ids: List<String>,
        consumedAt: Long,
    )

    @Query("SELECT COUNT(*) FROM progression_event WHERE userId = :userId")
    suspend fun count(userId: String): Int
}
