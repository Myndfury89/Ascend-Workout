package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Seed‑backed class definition. Collections are stored as delimited strings (parsed
 * in the mapper) to avoid a type‑converter layer. Seeded from ClassCatalog and
 * updateable independently of player progression.
 */
@Entity(tableName = "class_definition")
data class ClassDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val classTitle: String,
    val description: String,
    val fitnessIdentity: String,
    val favoredWorkoutCategories: String,
    val favoredTags: String,
    val primaryAttributes: String,
    val secondaryAttributes: String,
    val attributeMultipliers: String,
    val uniqueProficiencyKey: String,
    val uniqueProficiencyName: String,
    val favoredClassXpMultiplier: Double,
    val nonFavoredClassXpMultiplier: Double,
    val statusThemeKey: String,
    val frameVariantKey: String,
    val accentTokenKey: String,
    val proficiencyIconKey: String,
    val idleEffectKey: String,
    val progressionEffectKey: String,
    val classQuestTemplateIds: String,
    val expeditionTemplateIds: String,
    val achievementPathIds: String,
    val titleIds: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

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
    val primaryStartedAt: Long? = null,
    val secondaryStartedAt: Long? = null,
    val selectionReason: String? = null,
    val changeSource: String? = null,
    // Schema support for switch cooldown + respecialization gate (flow added later).
    val cooldownUntil: Long? = null,
    val respecQuestId: String? = null,
    val updatedAt: Long,
)

/**
 * Append‑only record of class selections, so switching classes preserves history
 * (and the immutable class‑XP / proficiency ledgers are never deleted on a switch).
 */
@Entity(
    tableName = "class_history",
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
data class ClassHistoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val classId: String,
    val slot: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val selectionReason: String? = null,
    val changeSource: String? = null,
    val createdAt: Long,
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
        Index(value = ["classId", "transactionType", "sourceType", "sourceId", "rewardType"], unique = true),
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
    val rewardType: String,
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
        Index(value = ["proficiencyKey", "transactionType", "sourceType", "sourceId", "rewardType"], unique = true),
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
    val rewardType: String,
    val createdAt: Long,
)
