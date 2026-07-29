package com.ascend.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatarUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val onboardingCompleted: Boolean = false,
    val measurementSystem: String = "METRIC",
    // Display unit for weights (canonical storage is always kilograms). Defaults from the
    // measurement system but may be overridden independently. A DB default keeps the NOT NULL
    // column consistent with the v12->v13 ALTER for rows inserted without it.
    @ColumnInfo(defaultValue = "KILOGRAMS") val weightUnit: String = "KILOGRAMS",
    val timezone: String? = null,
    val localOnly: Boolean = true,
    val cloudSyncEnabled: Boolean = false,
)

@Entity(
    tableName = "player_progress",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlayerProgressEntity(
    @PrimaryKey val userId: String,
    val level: Int = 1,
    val currentLevelXp: Long = 0,
    val lifetimeXp: Long = 0,
    val rank: String = "INITIATE",
    val currentTitleId: String? = null,
    val activeStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalWorkouts: Int = 0,
    val totalQuests: Int = 0,
    val totalExpeditions: Int = 0,
    val updatedAt: Long,
)

@Entity(
    tableName = "player_stats",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlayerStatsEntity(
    @PrimaryKey val userId: String,
    val strength: Long = 0,
    val endurance: Long = 0,
    val agility: Long = 0,
    val discipline: Long = 0,
    val recovery: Long = 0,
    val updatedAt: Long,
)
