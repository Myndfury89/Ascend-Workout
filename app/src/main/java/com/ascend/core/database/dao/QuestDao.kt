package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.relation.QuestWithObjectives
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    @Upsert
    suspend fun upsertQuest(entity: QuestEntity)

    @Upsert
    suspend fun upsertObjective(entity: QuestObjectiveEntity)

    @Query("SELECT * FROM quest WHERE id = :questId")
    suspend fun getQuest(questId: String): QuestEntity?

    @Query("SELECT * FROM quest WHERE id = :questId")
    fun observeQuest(questId: String): Flow<QuestEntity?>

    @Transaction
    @Query("SELECT * FROM quest WHERE id = :questId")
    fun observeQuestWithObjectives(questId: String): Flow<QuestWithObjectives?>

    @Transaction
    @Query("SELECT * FROM quest WHERE id = :questId")
    suspend fun getQuestWithObjectives(questId: String): QuestWithObjectives?

    @Transaction
    @Query("SELECT * FROM quest WHERE userId = :userId ORDER BY COALESCE(scheduledDate, createdAt) DESC")
    fun observeQuestsWithObjectivesForUser(userId: String): Flow<List<QuestWithObjectives>>

    @Query("SELECT * FROM quest WHERE userId = :userId ORDER BY COALESCE(scheduledDate, createdAt) DESC")
    fun observeQuestsForUser(userId: String): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest_objective WHERE questId = :questId ORDER BY orderIndex ASC")
    suspend fun getObjectives(questId: String): List<QuestObjectiveEntity>

    @Query("SELECT * FROM quest_objective WHERE questId = :questId ORDER BY orderIndex ASC")
    fun observeObjectives(questId: String): Flow<List<QuestObjectiveEntity>>

    @Query("SELECT * FROM quest_objective WHERE id = :objectiveId")
    suspend fun getObjective(objectiveId: String): QuestObjectiveEntity?

    /** Returns row id, or -1 when the import dedup guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressEntry(entity: QuestProgressEntryEntity): Long

    @Query("SELECT * FROM quest_progress_entry WHERE objectiveId = :objectiveId ORDER BY completedAt ASC")
    suspend fun getProgressEntries(objectiveId: String): List<QuestProgressEntryEntity>

    @Query("SELECT * FROM quest_progress_entry WHERE objectiveId = :objectiveId ORDER BY completedAt ASC")
    fun observeProgressEntries(objectiveId: String): Flow<List<QuestProgressEntryEntity>>

    @Query("SELECT * FROM quest_progress_entry WHERE id = :entryId")
    suspend fun getProgressEntry(entryId: String): QuestProgressEntryEntity?

    @Query("DELETE FROM quest_progress_entry WHERE id = :entryId")
    suspend fun deleteProgressEntry(entryId: String)

    @Query("UPDATE quest_progress_entry SET value = :value, updatedAt = :updatedAt WHERE id = :entryId")
    suspend fun updateProgressEntryValue(
        entryId: String,
        value: Double,
        updatedAt: Long,
    )

    /** Authoritative cumulative progress for an objective (sum of its entries). */
    @Query("SELECT COALESCE(SUM(value), 0) FROM quest_progress_entry WHERE objectiveId = :objectiveId")
    suspend fun sumProgress(objectiveId: String): Double

    @Query("UPDATE quest_objective SET currentValue = :value, status = :status WHERE id = :objectiveId")
    suspend fun updateObjectiveProgress(
        objectiveId: String,
        value: Double,
        status: String,
    )

    @Query("UPDATE quest_objective SET targetValue = :target WHERE id = :objectiveId")
    suspend fun updateObjectiveTarget(
        objectiveId: String,
        target: Double,
    )

    @Query("UPDATE quest SET status = :status, updatedAt = :updatedAt WHERE id = :questId")
    suspend fun updateQuestStatus(
        questId: String,
        status: String,
        updatedAt: Long,
    )

    /** Recent finished quest outcomes for a template's exercise — the daily‑quest baseline source. */
    @Query(
        "SELECT q.id AS questId, q.status AS status, o.targetValue AS target, o.currentValue AS current, q.createdAt AS createdAt " +
            "FROM quest q JOIN quest_objective o ON o.questId = q.id " +
            "WHERE q.userId = :userId AND o.exerciseId = :exerciseId " +
            "AND q.status IN ('COMPLETED', 'OVER_COMPLETED', 'PARTIALLY_COMPLETED') " +
            "ORDER BY q.createdAt DESC LIMIT :limit",
    )
    suspend fun getRecentQuestOutcomes(
        userId: String,
        exerciseId: String,
        limit: Int,
    ): List<QuestOutcomeRow>
}

/** Projection of a finished quest's target vs. achieved amount. */
data class QuestOutcomeRow(
    val questId: String,
    val status: String,
    val target: Double,
    val current: Double,
    val createdAt: Long,
)
