package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The execution plan for a Daily Quest (one per quest). */
@Entity(
    tableName = "quest_interval_schedule",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["questId"], unique = true)],
)
data class QuestIntervalScheduleEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val scheduleMode: String,
    val activeWindowStart: Long,
    val activeWindowEnd: Long,
    val intervalCount: Int,
    val distributionStrategy: String,
    val adaptiveRedistributionEnabled: Boolean,
    val redistributionPreference: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "quest_interval",
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
data class QuestIntervalEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val title: String,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val targetValue: Double,
    val currentValue: Double,
    val isCumulative: Boolean,
    val status: String,
    val orderIndex: Int,
    val reminderEnabled: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "quest_checkpoint",
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
data class QuestCheckpointEntity(
    @PrimaryKey val id: String,
    val questId: String,
    val title: String,
    val targetValue: Double,
    val dueAt: Long,
    val isCumulative: Boolean,
    val status: String,
    val orderIndex: Int,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * One logged contribution toward an interval. The import dedup guard — unique
 * (sourceApplication, externalRecordId) — blocks re‑importing the same external
 * (e.g. Health Connect) record across interval + daily totals.
 */
@Entity(
    tableName = "quest_interval_progress_entry",
    foreignKeys = [
        ForeignKey(
            entity = QuestIntervalEntity::class,
            parentColumns = ["id"],
            childColumns = ["questIntervalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["questIntervalId"]),
        Index(value = ["questId"]),
        Index(value = ["sourceApplication", "externalRecordId"], unique = true),
    ],
)
data class QuestIntervalProgressEntryEntity(
    @PrimaryKey val id: String,
    val questIntervalId: String,
    val questId: String,
    val value: Double,
    val source: String,
    val sourceApplication: String? = null,
    val externalRecordId: String? = null,
    val completedAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
