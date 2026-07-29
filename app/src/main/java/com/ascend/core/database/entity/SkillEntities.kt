package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A player's persistent state for one Skill (unlocked flag, level, accrued Skill XP). Skill
 * definitions themselves are seed data served from `SkillCatalog`; only the mutable player state
 * is stored here.
 */
@Entity(
    tableName = "player_skill",
    primaryKeys = ["userId", "skillId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId"])],
)
data class PlayerSkillEntity(
    val userId: String,
    val skillId: String,
    val unlocked: Boolean,
    val level: Int,
    val skillXp: Long,
    val unlockedAt: Long?,
    val updatedAt: Long,
)

/** An idempotent Skill-XP grant. Unique `(userId, skillId, sourceType, sourceId)` blocks doubles. */
@Entity(
    tableName = "skill_progress_transaction",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "skillId"]),
        Index(value = ["userId", "skillId", "sourceType", "sourceId"], unique = true),
    ],
)
data class SkillProgressTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val skillId: String,
    val amount: Long,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)

/** Append-only unlock record. Unique `(userId, skillId)` makes an unlock happen exactly once. */
@Entity(
    tableName = "skill_unlock_event",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId", "skillId"], unique = true)],
)
data class SkillUnlockEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val skillId: String,
    val unlockedAt: Long,
    val triggeringSourceType: String,
    val triggeringSourceId: String,
    val evidence: String,
    val createdAt: Long,
)
