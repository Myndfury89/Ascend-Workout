package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted presentation event (the "ProgressionEventQueue"). Written the instant a
 * reward is earned, drained later by whatever presents it. Exactly‑once enqueue is
 * guaranteed by the unique index on (batchId, sequence): re‑running the same earning
 * transaction can't duplicate the animation script. [consumedAt] null == pending.
 */
@Entity(
    tableName = "progression_event",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "consumedAt"]),
        Index(value = ["batchId", "sequence"], unique = true),
    ],
)
data class ProgressionEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val batchId: String,
    val sequence: Int,
    val type: String,
    val sourceType: String,
    val sourceId: String,
    val attributeType: String? = null,
    val subjectKey: String? = null,
    val fromValue: Long,
    val toValue: Long,
    val label: String? = null,
    val createdAt: Long,
    val consumedAt: Long? = null,
)
