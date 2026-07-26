package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Immutable XP ledger. Idempotency guard: the unique index on
 * (transactionType, sourceType, sourceId) makes a second AWARD for the same
 * source impossible, while still allowing a matching REVERSAL row.
 */
@Entity(
    tableName = "xp_transaction",
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
        Index(value = ["sourceType", "sourceId"]),
        Index(
            value = ["transactionType", "sourceType", "sourceId"],
            unique = true,
        ),
    ],
)
data class XpTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Long,
    val transactionType: String,
    val sourceType: String,
    val sourceId: String,
    val description: String,
    val reversedTransactionId: String? = null,
    val createdAt: Long,
)

/**
 * Attribute ledger. Mirrors the XP dedup guard, additionally keyed by attribute
 * so one source can grant several attributes exactly once each.
 */
@Entity(
    tableName = "attribute_transaction",
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
        Index(
            value = ["attributeType", "transactionType", "sourceType", "sourceId"],
            unique = true,
        ),
    ],
)
data class AttributeTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val attributeType: String,
    val amount: Long,
    val transactionType: String,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)
