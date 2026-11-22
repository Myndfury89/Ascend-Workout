package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Permanent record that [classId] has awakened for [userId] (schema v18). The primary key is the
 * pair, and the DAO is insert-only, so a later snapshot cannot revoke the row. [source] is an
 * awakening-source name. [evidenceJson] is an optional explanation payload.
 */
@Entity(
    tableName = "class_awakening",
    primaryKeys = ["userId", "classId"],
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ClassAwakeningEntity(
    val userId: String,
    val classId: String,
    val awakenedAt: Long,
    val source: String,
    val evidenceJson: String? = null,
)
