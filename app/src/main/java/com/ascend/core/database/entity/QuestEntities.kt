package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quest",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["scheduledDate"]),
        Index(value = ["status"]),
    ],
)
data class QuestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String? = null,
    val questType: String,
    val scheduledDate: Long? = null,
    val startTime: Long? = null,
    val deadline: Long? = null,
    val recurrenceRule: String? = null,
    val difficulty: String = "MODERATE",
    val status: String = "SCHEDULED",
    val baseRewardXp: Long = 0,
    val partialRewardEnabled: Boolean = true,
    val overCompletionEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "quest_objective",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questId"])],
)
data class QuestObjectiveEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val exerciseId: String? = null,
    val title: String,
    val objectiveType: String,
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val unit: String,
    // Which attribute this objective's volume trains (e.g. push-ups -> STRENGTH).
    val primaryAttribute: String? = null,
    val preferredSetSize: Int? = null,
    val minimumSetSize: Int? = null,
    val maximumSetSize: Int? = null,
    val completionRule: String = "ALL_REQUIRED",
    val orderIndex: Int = 0,
    val status: String = "ACTIVE",
)

/**
 * A single logged contribution toward an objective (e.g. one set of push-ups).
 * Import idempotency guard: unique (sourceApplication, externalRecordId) blocks
 * re-importing the same external record. Manual entries leave both null, and
 * SQLite treats NULLs as distinct, so manual logging is never blocked.
 */
@Entity(
    tableName = "quest_progress_entry",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestObjectiveEntity::class,
            parentColumns = ["id"],
            childColumns = ["objectiveId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["questId"]),
        Index(value = ["objectiveId"]),
        Index(value = ["externalRecordId"]),
        Index(value = ["sourceApplication", "externalRecordId"], unique = true),
    ],
)
data class QuestProgressEntryEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val objectiveId: String,
    val value: Double,
    val source: String,
    val sourceApplication: String? = null,
    val externalRecordId: String? = null,
    val completedAt: Long,
    val perceivedEffort: Int? = null,
    val durationSeconds: Long? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
