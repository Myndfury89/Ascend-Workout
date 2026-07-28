package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.QuestCheckpointEntity
import com.ascend.core.database.entity.QuestIntervalEntity
import com.ascend.core.database.entity.QuestIntervalProgressEntryEntity
import com.ascend.core.database.entity.QuestIntervalScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestIntervalDao {
    @Upsert
    suspend fun upsertSchedule(entity: QuestIntervalScheduleEntity)

    @Query("SELECT * FROM quest_interval_schedule WHERE questId = :questId")
    suspend fun getSchedule(questId: String): QuestIntervalScheduleEntity?

    @Upsert
    suspend fun upsertIntervals(entities: List<QuestIntervalEntity>)

    @Upsert
    suspend fun upsertInterval(entity: QuestIntervalEntity)

    @Query("SELECT * FROM quest_interval WHERE questId = :questId ORDER BY orderIndex ASC")
    suspend fun getIntervals(questId: String): List<QuestIntervalEntity>

    @Query("SELECT * FROM quest_interval WHERE questId = :questId ORDER BY orderIndex ASC")
    fun observeIntervals(questId: String): Flow<List<QuestIntervalEntity>>

    @Query("SELECT * FROM quest_interval WHERE id = :intervalId")
    suspend fun getInterval(intervalId: String): QuestIntervalEntity?

    @Query("DELETE FROM quest_interval WHERE questId = :questId")
    suspend fun deleteIntervals(questId: String)

    @Query("UPDATE quest_interval SET currentValue = :value, status = :status, completedAt = :completedAt WHERE id = :intervalId")
    suspend fun updateIntervalProgress(
        intervalId: String,
        value: Double,
        status: String,
        completedAt: Long?,
    )

    @Query("UPDATE quest_interval SET targetValue = :target WHERE id = :intervalId")
    suspend fun updateIntervalTarget(
        intervalId: String,
        target: Double,
    )

    /** Returns row id, or -1 when the import dedup guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressEntry(entity: QuestIntervalProgressEntryEntity): Long

    @Query("SELECT COALESCE(SUM(value), 0) FROM quest_interval_progress_entry WHERE questIntervalId = :intervalId")
    suspend fun sumIntervalProgress(intervalId: String): Double

    @Query("SELECT COALESCE(SUM(value), 0) FROM quest_interval_progress_entry WHERE questId = :questId")
    suspend fun sumDailyProgress(questId: String): Double

    @Upsert
    suspend fun upsertCheckpoints(entities: List<QuestCheckpointEntity>)

    @Query("SELECT * FROM quest_checkpoint WHERE questId = :questId ORDER BY orderIndex ASC")
    suspend fun getCheckpoints(questId: String): List<QuestCheckpointEntity>
}
