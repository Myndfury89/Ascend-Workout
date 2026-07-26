package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A logged training session composed of [WorkoutSetEntity] rows. Distinct from a
 * quest (an accumulation goal) but feeds the same XP/attribute ledger when
 * completed. XP is awarded exactly once via (WORKOUT_COMPLETION, workoutId).
 */
@Entity(
    tableName = "workout",
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
        Index(value = ["performedAt"]),
        Index(value = ["status"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val notes: String? = null,
    val difficulty: String = "MODERATE",
    val status: String = "IN_PROGRESS",
    val performedAt: Long,
    val durationSeconds: Long? = null,
    val perceivedEffort: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * A single set within a workout (e.g. 12 reps of push-ups, or 30s of plank).
 * [reps]/[weight]/[durationSeconds]/[distance] are populated per the exercise's
 * measurement type; [volume] is the denormalised training value in the set's
 * [unit], used to grow the exercise's primary attribute.
 */
@Entity(
    tableName = "workout_set",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["workoutId"]),
        Index(value = ["exerciseId"]),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int = 0,
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSeconds: Long? = null,
    val distance: Double? = null,
    // Denormalised training value in [unit] (reps, seconds, metres, ...).
    val volume: Double,
    val unit: String,
    val completedAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
)
