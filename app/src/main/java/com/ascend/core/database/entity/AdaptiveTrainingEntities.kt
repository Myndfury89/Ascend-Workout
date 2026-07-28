package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A prescribed exercise (current or proposed). Applying a recommendation supersedes the old row. */
@Entity(
    tableName = "exercise_prescription",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId", "exerciseId", "status"])],
)
data class ExercisePrescriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val exerciseId: String,
    val progressionStrategy: String,
    val targetSets: Int?,
    val minimumReps: Int?,
    val maximumReps: Int?,
    val targetWeight: Double?,
    val targetRestSeconds: Int?,
    val targetDurationSeconds: Long?,
    val targetDistance: Double?,
    val targetPace: Double?,
    val tempo: String?,
    val variationId: String?,
    val assistanceValue: Double?,
    val effectiveFrom: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** A structured readiness evaluation, kept for audit and recommendation linkage. */
@Entity(
    tableName = "training_readiness_snapshot",
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
data class TrainingReadinessSnapshotEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val exerciseId: String?,
    val questTemplateId: String?,
    val readinessState: String,
    val score: Double,
    val confidence: Double,
    val evidence: String,
    val positiveSignals: String,
    val limitingSignals: String,
    val missingSignals: String,
    val safetyState: String,
    val safetyFlags: String,
    val createdAt: Long,
)

/** A pending/accepted/applied progression recommendation. Survives restarts. */
@Entity(
    tableName = "progression_recommendation",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId", "status"])],
)
data class ProgressionRecommendationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val recommendationType: String,
    val exerciseId: String?,
    val questTemplateId: String?,
    val currentPrescriptionId: String?,
    val proposedPrescriptionId: String?,
    val proposedTarget: Int?,
    val readinessState: String,
    val reason: String,
    val evidence: String,
    val confidence: Double,
    val safetyState: String,
    val requiresConfirmation: Boolean,
    val status: String,
    val generatedAt: Long,
    val expiresAt: Long,
    val acceptedAt: Long?,
    val rejectedAt: Long?,
    val appliedAt: Long?,
)

/** A current/proposed cardio prescription. Applying a recommendation supersedes the old row. */
@Entity(
    tableName = "cardio_prescription",
    foreignKeys = [
        ForeignKey(
            entity = UserProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["userId", "exerciseId", "status"])],
)
data class CardioPrescriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val exerciseId: String,
    val mode: String,
    val targetDurationSeconds: Long?,
    val targetDistanceMeters: Double?,
    val targetPaceSecondsPerKm: Double?,
    val targetSpeed: Double?,
    val incline: Double?,
    val resistance: Double?,
    val intervalCount: Int?,
    val workIntervalSeconds: Int?,
    val restIntervalSeconds: Int?,
    val targetHrZoneSeconds: Long?,
    val effectiveFrom: Long,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A proven progression milestone. The unique [milestoneKey] guarantees the same
 * milestone is never rewarded twice.
 */
@Entity(
    tableName = "progression_milestone",
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
        Index(value = ["milestoneKey"], unique = true),
    ],
)
data class ProgressionMilestoneEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val milestoneKey: String,
    val milestoneType: String,
    val exerciseId: String?,
    val questTemplateId: String?,
    val sourceRecommendationId: String?,
    val previousValue: Double,
    val newValue: Double,
    val createdAt: Long,
)
