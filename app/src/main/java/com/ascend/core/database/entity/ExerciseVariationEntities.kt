package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A node in an exercise's variation graph. Stable ids → idempotent seed upsert. */
@Entity(
    tableName = "exercise_variation",
    indices = [Index(value = ["exerciseId"])],
)
data class ExerciseVariationEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val name: String,
    val description: String,
    val difficultyTier: Int,
    val variationTags: String,
    val assistanceType: String,
    val assistanceValue: Double?,
    val externalLoadSupported: Boolean,
    val rangeOfMotionLevel: Int,
    val tempoProfile: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A directed edge between two variations. Unique by
 * `(sourceVariationId, destinationVariationId, progressionType)` so a graph can't hold a
 * duplicate transition.
 */
@Entity(
    tableName = "exercise_variation_edge",
    indices = [
        Index(value = ["sourceVariationId"]),
        Index(
            value = ["sourceVariationId", "destinationVariationId", "progressionType"],
            unique = true,
        ),
    ],
)
data class ExerciseVariationEdgeEntity(
    @PrimaryKey val id: String,
    val sourceVariationId: String,
    val destinationVariationId: String,
    val progressionType: String,
    val minimumSuccessfulExposures: Int,
    val minimumCompletedReps: Int,
    val minimumCompletedSets: Int,
    val maximumRpe: Double?,
    val minimumRir: Int?,
    val maximumAssistanceValue: Double?,
    val requiredRangeOfMotion: Int?,
    val requiredTempoControl: Boolean,
    val classUnlockRequirement: String?,
    val safetyNotes: String?,
    val enabled: Boolean,
)
