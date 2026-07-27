package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.ClassDefinitionEntity
import com.ascend.core.database.entity.ClassHistoryEntity
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.PlayerClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    // ---- Definitions (seed data) ----
    @Upsert
    suspend fun upsertDefinitions(entities: List<ClassDefinitionEntity>)

    @Query("SELECT * FROM class_definition WHERE enabled = 1 ORDER BY name ASC")
    fun observeDefinitions(): Flow<List<ClassDefinitionEntity>>

    @Query("SELECT * FROM class_definition ORDER BY name ASC")
    suspend fun getDefinitions(): List<ClassDefinitionEntity>

    @Query("SELECT * FROM class_definition WHERE id = :id")
    suspend fun getDefinition(id: String): ClassDefinitionEntity?

    @Query("SELECT COUNT(*) FROM class_definition")
    suspend fun countDefinitions(): Int

    // ---- Selection ----
    @Upsert
    suspend fun upsertSelection(entity: PlayerClassEntity)

    @Query("SELECT * FROM player_class WHERE userId = :userId")
    suspend fun getSelection(userId: String): PlayerClassEntity?

    @Query("SELECT * FROM player_class WHERE userId = :userId")
    fun observeSelection(userId: String): Flow<PlayerClassEntity?>

    // ---- History ----
    @Insert
    suspend fun insertHistory(entity: ClassHistoryEntity)

    @Query("SELECT * FROM class_history WHERE userId = :userId ORDER BY startedAt ASC, createdAt ASC")
    suspend fun getHistory(userId: String): List<ClassHistoryEntity>

    @Query("UPDATE class_history SET endedAt = :endedAt WHERE userId = :userId AND slot = :slot AND endedAt IS NULL")
    suspend fun closeOpenHistory(
        userId: String,
        slot: String,
        endedAt: Long,
    )

    // ---- Ledgers ----

    /** Returns row id, or -1 when the exactly-once guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClassXp(entity: ClassXpTransactionEntity): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM class_xp_transaction WHERE userId = :userId AND classId = :classId")
    suspend fun totalClassXp(
        userId: String,
        classId: String,
    ): Long

    @Query("SELECT COALESCE(SUM(amount), 0) FROM class_proficiency_transaction WHERE userId = :userId AND proficiencyKey = :key")
    suspend fun totalProficiency(
        userId: String,
        key: String,
    ): Long

    /** Returns row id, or -1 when the exactly-once guard blocks a duplicate. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProficiency(entity: ClassProficiencyTransactionEntity): Long
}
