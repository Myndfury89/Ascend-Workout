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
    // Onboarding + safety/privacy (schema v14). Canonical height is centimetres. Age is stored only
    // as the derived four-band safety category (never a birth year/DOB). Social defaults start
    // private with presence/discovery OFF; DB defaults keep NOT NULL columns consistent with the
    // v13->v14 ALTER for rows inserted without them.
    @ColumnInfo(defaultValue = "0") val onboardingVersion: Int = 0,
    val heightCm: Double? = null,
    @ColumnInfo(defaultValue = "NOT_PROVIDED") val ageSafetyCategory: String = "NOT_PROVIDED",
    @ColumnInfo(defaultValue = "PRIVATE") val socialVisibility: String = "PRIVATE",
    @ColumnInfo(defaultValue = "0") val partyPresenceEnabled: Boolean = false,
    @ColumnInfo(defaultValue = "0") val strangerDiscoveryEnabled: Boolean = false,
    // Cosmetic "Your Ascended" avatar body base (schema v16). Nullable = not chosen yet (prompt on
    // first open). Purely a visual presentation choice — fully independent of physiologySex, never
    // inferred from it, and never affects any gameplay/progression data.
    val avatarBodyBase: String? = null,
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
