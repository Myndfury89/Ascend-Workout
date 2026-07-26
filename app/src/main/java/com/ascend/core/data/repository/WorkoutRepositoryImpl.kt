package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.CompleteWorkoutResult
import com.ascend.core.domain.repository.NewSetSpec
import com.ascend.core.domain.repository.NewWorkoutSpec
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.domain.repository.WorkoutRepository
import com.ascend.core.domain.repository.XpAwardResult
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Exercise
import com.ascend.core.model.Workout
import com.ascend.core.model.WorkoutStatus
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

@Singleton
class WorkoutRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val workoutDao: WorkoutDao,
        private val exerciseDao: ExerciseDao,
        private val progressionRepository: ProgressionRepository,
        private val xpCalculator: XpCalculator,
        private val attributeCalculator: AttributeProgressCalculator,
    ) : WorkoutRepository {
        private fun now() = System.currentTimeMillis()

        override fun observeExercises(): Flow<List<Exercise>> = exerciseDao.observeAll().map { list -> list.map { it.toDomain() } }

        override suspend fun getExercise(exerciseId: String): Exercise? = exerciseDao.getById(exerciseId)?.toDomain()

        override fun observeWorkoutsForUser(userId: String): Flow<List<Workout>> =
            workoutDao.observeWorkoutsWithSetsForUser(userId).map { list -> list.map { it.toDomain() } }

        override fun observeWorkout(workoutId: String): Flow<Workout?> = workoutDao.observeWorkoutWithSets(workoutId).map { it?.toDomain() }

        override suspend fun getWorkout(workoutId: String): Workout? = workoutDao.getWorkoutWithSets(workoutId)?.toDomain()

        override suspend fun createWorkout(spec: NewWorkoutSpec): String =
            db.withTransaction {
                val workoutId = newId()
                val ts = now()
                workoutDao.upsertWorkout(
                    WorkoutEntity(
                        id = workoutId,
                        userId = spec.userId,
                        title = spec.title,
                        notes = spec.notes,
                        difficulty = spec.difficulty.name,
                        status = WorkoutStatus.IN_PROGRESS.name,
                        performedAt = spec.performedAt ?: ts,
                        createdAt = ts,
                        updatedAt = ts,
                    ),
                )
                workoutId
            }

        override suspend fun addSet(
            workoutId: String,
            spec: NewSetSpec,
        ): String =
            db.withTransaction {
                val setId = newId()
                val ts = now()
                val order = workoutDao.maxOrderIndex(workoutId) + 1
                workoutDao.upsertSet(
                    WorkoutSetEntity(
                        id = setId,
                        workoutId = workoutId,
                        exerciseId = spec.exerciseId,
                        orderIndex = order,
                        reps = spec.reps,
                        weight = spec.weight,
                        durationSeconds = spec.durationSeconds,
                        distance = spec.distance,
                        volume = spec.volume,
                        unit = spec.unit,
                        completedAt = ts,
                        createdAt = ts,
                        updatedAt = ts,
                    ),
                )
                setId
            }

        override suspend fun deleteSet(setId: String): Boolean {
            workoutDao.getSet(setId) ?: return false
            workoutDao.deleteSet(setId)
            return true
        }

        override suspend fun completeWorkout(workoutId: String): CompleteWorkoutResult {
            val workout = workoutDao.getWorkoutWithSets(workoutId)?.toDomain() ?: return CompleteWorkoutResult.NotFound
            if (workout.sets.isEmpty()) return CompleteWorkoutResult.Empty

            return db.withTransaction {
                val difficulty = workout.difficulty
                val durationSeconds = workout.durationSeconds ?: derivedDurationSeconds(workout)
                val durationMinutes = (durationSeconds / 60).toInt()

                val xp =
                    xpCalculator.workoutXp(
                        durationMinutes = durationMinutes,
                        intensity = intensityFor(difficulty),
                        volumeScore = volumeScore(workout.totalVolume),
                    )

                val xpOutcome =
                    progressionRepository.awardXp(
                        userId = workout.userId,
                        amount = xp,
                        sourceType = XpSourceType.WORKOUT_COMPLETION,
                        sourceId = workoutId,
                        description = "Workout: ${workout.title}",
                    )

                workoutDao.updateWorkoutStatus(workoutId, WorkoutStatus.COMPLETED.name, durationSeconds, now())

                if (xpOutcome is XpAwardResult.Duplicate) {
                    return@withTransaction CompleteWorkoutResult.AlreadyCompleted
                }

                val deltas = LinkedHashMap<AttributeType, Long>()
                workout.volumeByAttribute.forEach { (attribute, volume) ->
                    val points = attributeCalculator.volumePoints(volume, difficulty)
                    if (points > 0) deltas[attribute] = (deltas[attribute] ?: 0L) + points
                }
                deltas[AttributeType.DISCIPLINE] =
                    (deltas[AttributeType.DISCIPLINE] ?: 0L) + attributeCalculator.disciplinePoints(difficulty)

                progressionRepository.awardAttributes(workout.userId, deltas, XpSourceType.WORKOUT_COMPLETION, workoutId)

                val awarded = xpOutcome as XpAwardResult.Awarded
                CompleteWorkoutResult.Completed(awarded.amount, awarded.newLevel, awarded.leveledUp, deltas)
            }
        }

        /** Span from the first to the last logged set; 0 when a single instant. */
        private fun derivedDurationSeconds(workout: Workout): Long {
            if (workout.sets.size < 2) return 0
            val times = workout.sets.map { it.completedAt }
            return ((times.max() - times.min()) / 1000).coerceAtLeast(0)
        }

        private fun intensityFor(difficulty: Difficulty): Float =
            when (difficulty) {
                Difficulty.EASY -> 0.3f
                Difficulty.MODERATE -> 0.5f
                Difficulty.HARD -> 0.75f
                Difficulty.EXTREME -> 1.0f
            }

        /** Normalise total training volume into the 0f..1f the XP formula expects. */
        private fun volumeScore(totalVolume: Double): Float =
            (totalVolume / VOLUME_REFERENCE).coerceIn(0.0, 1.0).let { (it * 100).roundToLong() / 100f }

        private companion object {
            // Volume (reps/seconds/metres) at which the volume XP component maxes out.
            const val VOLUME_REFERENCE = 250.0
        }
    }
