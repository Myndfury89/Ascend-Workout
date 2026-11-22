package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ascend.core.database.entity.ClassAwakeningEntity
import kotlinx.coroutines.flow.Flow

/**
 * Permanent awakening rows. Insert-or-ignore only: there is no update and no delete, so an
 * awakening cannot be revoked.
 */
@Dao
interface ClassAwakeningDao {
    /** Row id of the insert, or -1 when `(userId, classId)` is already awake. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: ClassAwakeningEntity): Long

    @Query("SELECT * FROM class_awakening WHERE userId = :userId")
    fun observe(userId: String): Flow<List<ClassAwakeningEntity>>

    @Query("SELECT classId FROM class_awakening WHERE userId = :userId")
    suspend fun ids(userId: String): List<String>
}
