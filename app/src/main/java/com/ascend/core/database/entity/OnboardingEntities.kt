package com.ascend.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Persistence for onboarding + the provisional initial assessment (schema v14). One row per local
 * user. Collections are stored as delimited strings (consistent with the rest of the schema); enums
 * as their names. Nothing here holds verified evidence — the assessment is SELF_REPORTED and
 * [InitialAssessmentEntity.replacedByEvidenceAt] stays null until a future reconciliation phase.
 */

/** Resumable onboarding progress, so an interrupted flow continues from the saved step. */
@Entity(tableName = "onboarding_state")
data class OnboardingStateEntity(
    @PrimaryKey val userId: String,
    val currentStep: String,
    val completedSteps: String,
    val skippedSteps: String,
    val startedAt: Long,
    val completedAt: Long?,
    val version: Int,
)

/** The provisional, self-reported assessment. Body weight/goal live here; height lives on the profile. */
@Entity(tableName = "initial_assessment")
data class InitialAssessmentEntity(
    @PrimaryKey val userId: String,
    val primaryGoal: String?,
    val secondaryGoals: String,
    val trainingFrequency: String?,
    val activityExperience: String,
    val activityPreferences: String,
    val equipment: String,
    val environment: String?,
    val limitations: String,
    val ageSafetyCategory: String,
    val provenance: String,
    val selfReportedAt: Long,
    val replacedByEvidenceAt: Long?,
    // Ability snapshot (comfortable recent performance only).
    val abilityPushUps: Int?,
    val abilitySquats: Int?,
    val abilityPullUp: String?,
    val abilityCardioMinutes: Int?,
    val abilitySteps: Int?,
    val abilityRecentStrength: Boolean?,
    val abilityPreferredCardio: String?,
    val abilityTypicalDuration: String?,
    // Availability.
    val availabilityTrainingDays: String?,
    val availabilitySessionDuration: String?,
    val availabilityPreferredDays: String,
    val availabilityTimeWindows: String,
    val availabilityRestDays: String,
    // Optional physiology + body metrics (private).
    val physiologySex: String,
    val physiologyWaistCm: Double?,
    val physiologyBodyFatPercent: Double?,
    val currentWeightKg: Double?,
    val goalWeightKg: Double?,
)

/** The explainable, non-locking class-affinity recommendation (advisory; user may choose any class). */
@Entity(tableName = "class_affinity_result")
data class ClassAffinityResultEntity(
    @PrimaryKey val userId: String,
    val recommendedClassId: String?,
    val classScores: String,
    val rationale: String,
    val evidenceKeys: String,
    val createdAt: Long,
)

/** The provisional initial plan header; its quests are in [InitialQuestPlanItemEntity]. */
@Entity(tableName = "initial_quest_plan")
data class InitialQuestPlanEntity(
    @PrimaryKey val userId: String,
    val difficultyBand: String,
    val rationale: String,
    val safetyAdjustments: String,
    @ColumnInfo(defaultValue = "1") val provisional: Boolean = true,
    val createdAt: Long,
)

/** One provisional starting quest within an [InitialQuestPlanEntity]. */
@Entity(
    tableName = "initial_quest_plan_item",
    foreignKeys = [
        ForeignKey(
            entity = InitialQuestPlanEntity::class,
            parentColumns = ["userId"],
            childColumns = ["planUserId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planUserId")],
)
data class InitialQuestPlanItemEntity(
    @PrimaryKey val id: String,
    val planUserId: String,
    val templateId: String,
    val name: String,
    val unit: String,
    val target: Int,
    val preferredSetSize: Int?,
    val rationale: String,
    val equipmentCompatible: Boolean,
    val scheduleCompatible: Boolean,
    val safetyAdjusted: Boolean,
    @ColumnInfo(defaultValue = "1") val provisional: Boolean = true,
    val sortOrder: Int,
)
