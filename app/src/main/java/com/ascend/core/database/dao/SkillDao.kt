package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.PlayerSkillEntity
import com.ascend.core.database.entity.SkillProgressTransactionEntity
import com.ascend.core.database.entity.SkillUnlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Upsert
    suspend fun upsertPlayerSkill(entity: PlayerSkillEntity)

    @Query("SELECT * FROM player_skill WHERE userId = :userId AND skillId = :skillId")
    suspend fun getPlayerSkill(
        userId: String,
        skillId: String,
    ): PlayerSkillEntity?

    @Query("SELECT * FROM player_skill WHERE userId = :userId")
    suspend fun getPlayerSkills(userId: String): List<PlayerSkillEntity>

    @Query("SELECT * FROM player_skill WHERE userId = :userId")
    fun observePlayerSkills(userId: String): Flow<List<PlayerSkillEntity>>

    @Query("DELETE FROM player_skill WHERE userId = :userId AND skillId = :skillId")
    suspend fun deletePlayerSkill(
        userId: String,
        skillId: String,
    )

    /** Returns the row id, or -1 when the idempotency guard blocks a duplicate grant. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(entity: SkillProgressTransactionEntity): Long

    /** Returns the row id, or -1 when this Skill was already unlocked for the user. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockEvent(entity: SkillUnlockEventEntity): Long

    @Query("SELECT * FROM skill_unlock_event WHERE userId = :userId")
    suspend fun getUnlockEvents(userId: String): List<SkillUnlockEventEntity>
}
