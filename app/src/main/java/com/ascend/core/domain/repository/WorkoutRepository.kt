package com.ascend.core.domain.repository

import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Exercise
import com.ascend.core.model.RewardBreakdown
import com.ascend.core.model.Workout
import kotlinx.coroutines.flow.Flow

/** A set to append to a workout; unused measurements stay null. */
data class NewSetSpec(
    val exerciseId: String,
    val volume: Double,
    val unit: String,
    val reps: Int? = null,
    val weight: Double? = null,
    val durationSeconds: Long? = null,
    val distance: Double? = null,
)

data class NewWorkoutSpec(
    val userId: String,
    val title: String,
    val notes: String? = null,
    val difficulty: Difficulty = Difficulty.MODERATE,
    val performedAt: Long? = null,
)

sealed interface CompleteWorkoutResult {
    data class Completed(
        val xpAwarded: Long,
        val newLevel: Int,
        val leveledUp: Boolean,
        val attributeDeltas: Map<AttributeType, Long>,
        val rewardBreakdown: RewardBreakdown,
    ) : CompleteWorkoutResult

    /** A workout with no logged sets earns nothing; nothing is awarded. */
    data object Empty : CompleteWorkoutResult

    data object AlreadyCompleted : CompleteWorkoutResult

    data object NotFound : CompleteWorkoutResult
}

interface WorkoutRepository {
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun getExercise(exerciseId: String): Exercise?

    fun observeWorkoutsForUser(userId: String): Flow<List<Workout>>

    fun observeWorkout(workoutId: String): Flow<Workout?>

    suspend fun getWorkout(workoutId: String): Workout?

    suspend fun createWorkout(spec: NewWorkoutSpec): String

    /** Append a set; returns the new set id. */
    suspend fun addSet(
        workoutId: String,
        spec: NewSetSpec,
    ): String

    suspend fun deleteSet(setId: String): Boolean

    /**
     * Finish a workout, awarding XP + attributes exactly once (idempotent on the
     * workout id). Duration-in-minutes and volume drive the XP; each set's
     * exercise primary attribute grows with its volume.
     */
    suspend fun completeWorkout(workoutId: String): CompleteWorkoutResult
}
