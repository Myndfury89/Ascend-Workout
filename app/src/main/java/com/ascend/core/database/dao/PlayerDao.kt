package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Upsert
    suspend fun upsertProfile(entity: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    suspend fun getProfile(userId: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :userId")
    fun observeProfile(userId: String): Flow<UserProfileEntity?>

    @Query("UPDATE user_profile SET weightUnit = :weightUnit, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateWeightUnit(
        userId: String,
        weightUnit: String,
        updatedAt: Long,
    )

    @Query("SELECT avatarBodyBase FROM user_profile WHERE id = :userId")
    fun observeAvatarBodyBase(userId: String): Flow<String?>

    @Query("UPDATE user_profile SET avatarBodyBase = :value, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateAvatarBodyBase(
        userId: String,
        value: String?,
        updatedAt: Long,
    )

    @Query("UPDATE user_profile SET heightCm = :heightCm, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateHeight(
        userId: String,
        heightCm: Double?,
        updatedAt: Long,
    )

    @Query(
        "UPDATE user_profile SET ageSafetyCategory = :ageSafetyCategory, socialVisibility = :socialVisibility, " +
            "partyPresenceEnabled = :partyPresenceEnabled, strangerDiscoveryEnabled = :strangerDiscoveryEnabled, " +
            "updatedAt = :updatedAt WHERE id = :userId",
    )
    suspend fun updateSafetyProfile(
        userId: String,
        ageSafetyCategory: String,
        socialVisibility: String,
        partyPresenceEnabled: Boolean,
        strangerDiscoveryEnabled: Boolean,
        updatedAt: Long,
    )

    @Query("UPDATE user_profile SET onboardingCompleted = 1, onboardingVersion = :version, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun markOnboardingComplete(
        userId: String,
        version: Int,
        updatedAt: Long,
    )

    @Query("UPDATE user_profile SET displayName = :displayName, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateDisplayName(
        userId: String,
        displayName: String,
        updatedAt: Long,
    )

    @Query("SELECT onboardingCompleted FROM user_profile WHERE id = :userId")
    fun observeOnboardingCompleted(userId: String): Flow<Boolean?>

    @Upsert
    suspend fun upsertProgress(entity: PlayerProgressEntity)

    @Query("SELECT * FROM player_progress WHERE userId = :userId")
    suspend fun getProgress(userId: String): PlayerProgressEntity?

    @Query("SELECT * FROM player_progress WHERE userId = :userId")
    fun observeProgress(userId: String): Flow<PlayerProgressEntity?>

    @Upsert
    suspend fun upsertStats(entity: PlayerStatsEntity)

    @Query("SELECT * FROM player_stats WHERE userId = :userId")
    suspend fun getStats(userId: String): PlayerStatsEntity?

    @Query("SELECT * FROM player_stats WHERE userId = :userId")
    fun observeStats(userId: String): Flow<PlayerStatsEntity?>
}
