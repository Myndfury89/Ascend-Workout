package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.BuildProfileSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BuildProfileDao {
    @Upsert
    suspend fun upsert(entity: BuildProfileSnapshotEntity)

    @Query("SELECT * FROM build_profile_snapshot WHERE userId = :userId")
    fun observe(userId: String): Flow<BuildProfileSnapshotEntity?>

    @Query("SELECT * FROM build_profile_snapshot WHERE userId = :userId")
    suspend fun get(userId: String): BuildProfileSnapshotEntity?
}
