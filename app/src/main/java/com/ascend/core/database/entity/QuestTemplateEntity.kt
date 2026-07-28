package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Seed‑backed Daily Quest template with configurable safe ranges. List fields are
 * stored as comma‑separated strings (parsed in the mapper). Seeded from code and
 * updateable without touching player data.
 */
@Entity(tableName = "quest_template")
data class QuestTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val objectiveType: String,
    val unit: String,
    val primaryAttribute: String?,
    val exerciseId: String?,
    val minimumTarget: Int,
    val maximumTarget: Int,
    val defaultTarget: Int,
    val targetStep: Int,
    val defaultQuickAddValues: String,
    val defaultPreferredSetSize: Int,
    val minimumAllowedSetSize: Int,
    val maximumAllowedSetSize: Int,
    val supportsAutomaticProgress: Boolean,
    val supportsManualProgress: Boolean,
    val supportedVariations: String,
    val safetyWarningThreshold: Int,
    val baseRewardXp: Long,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
