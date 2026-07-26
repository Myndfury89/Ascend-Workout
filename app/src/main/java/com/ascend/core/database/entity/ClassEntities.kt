package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A player's class selection (one row per user). */
@Entity(
    tableName = "player_class",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlayerClassEntity(
    @PrimaryKey val userId: String,
    val primaryClassId: String? = null,
    val secondaryClassId: String? = null,
    val updatedAt: Long,
)

/**
 * Immutable Class‑XP ledger, separate from the (class‑neutral) player XP ledger.
 * Idempotency guard mirrors the player XP ledger, additionally keyed by class so a
 * source awards each class exactly once.
 */
@Entity(
    tableName = "class_xp_transaction",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "classId"]),
        Index(value = ["classId", "transactionType", "sourceType", "sourceId"], unique = true),
    ],
)
data class ClassXpTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val classId: String,
    val amount: Long,
    val transactionType: String,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)

/**
 * Immutable ledger for class‑unique proficiencies (Force, Body Mastery, Energy
 * Control, …), keyed by proficiency so one source grants each exactly once.
 */
@Entity(
    tableName = "class_proficiency_transaction",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["userId", "proficiencyKey"]),
        Index(value = ["proficiencyKey", "transactionType", "sourceType", "sourceId"], unique = true),
    ],
)
data class ClassProficiencyTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val proficiencyKey: String,
    val amount: Long,
    val transactionType: String,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)
