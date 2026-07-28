package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import com.ascend.core.database.relation.WorkoutWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Upsert
    suspend fun upsertWorkout(entity: WorkoutEntity)

    @Upsert
    suspend fun upsertSet(entity: WorkoutSetEntity)

    @Query("SELECT * FROM workout WHERE id = :workoutId")
    suspend fun getWorkout(workoutId: String): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :workoutId")
    suspend fun getWorkoutWithSets(workoutId: String): WorkoutWithSets?

    @Transaction
    @Query("SELECT * FROM workout WHERE id = :workoutId")
    fun observeWorkoutWithSets(workoutId: String): Flow<WorkoutWithSets?>

    @Transaction
    @Query("SELECT * FROM workout WHERE userId = :userId ORDER BY performedAt DESC")
    fun observeWorkoutsWithSetsForUser(userId: String): Flow<List<WorkoutWithSets>>

    @Query("SELECT * FROM workout_set WHERE id = :setId")
    suspend fun getSet(setId: String): WorkoutSetEntity?

    @Query("SELECT * FROM workout_set WHERE workoutId = :workoutId ORDER BY orderIndex ASC")
    suspend fun getSets(workoutId: String): List<WorkoutSetEntity>

    @Query("DELETE FROM workout_set WHERE id = :setId")
    suspend fun deleteSet(setId: String)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM workout_set WHERE workoutId = :workoutId")
    suspend fun maxOrderIndex(workoutId: String): Int

    @Query("UPDATE workout SET status = :status, durationSeconds = :durationSeconds, updatedAt = :updatedAt WHERE id = :workoutId")
    suspend fun updateWorkoutStatus(
        workoutId: String,
        status: String,
        durationSeconds: Long?,
        updatedAt: Long,
    )

    /** Completed sets for an exercise across the user's history — the performance source. */
    @Query(
        "SELECT w.id AS workoutId, w.performedAt AS performedAt, w.perceivedEffort AS perceivedEffort, " +
            "ws.reps AS reps, ws.weight AS weight, ws.orderIndex AS orderIndex " +
            "FROM workout_set ws JOIN workout w ON ws.workoutId = w.id " +
            "WHERE w.userId = :userId AND ws.exerciseId = :exerciseId AND w.status = 'COMPLETED' " +
            "ORDER BY w.performedAt ASC, ws.orderIndex ASC",
    )
    suspend fun getExerciseSetHistory(
        userId: String,
        exerciseId: String,
    ): List<ExerciseSetHistoryRow>
}

/** Projection of one logged set with its session's time + effort. */
data class ExerciseSetHistoryRow(
    val workoutId: String,
    val performedAt: Long,
    val perceivedEffort: Int?,
    val reps: Int?,
    val weight: Double?,
    val orderIndex: Int,
)
