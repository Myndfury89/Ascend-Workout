package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.PlayerClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
    @Upsert
    suspend fun upsertSelection(entity: PlayerClassEntity)

    @Query("SELECT * FROM player_class WHERE userId = :userId")
    suspend fun getSelection(userId: String): PlayerClassEntity?

    @Query("SELECT * FROM player_class WHERE userId = :userId")
    fun observeSelection(userId: String): Flow<PlayerClassEntity?>

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
