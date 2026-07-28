package com.ascend.core.domain.training

import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.model.QuestOutcome
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.SetPerformance
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Derives training performance from the **existing** workout and quest records — no
 * duplicated raw set data. Exercise sessions come from completed workouts' sets;
 * Daily Quest outcomes from finished quests linked to a template's exercise.
 */
class PerformanceHistoryRepository
    @Inject
    constructor(
        private val workoutDao: WorkoutDao,
        private val questDao: QuestDao,
    ) {
        /** Completed sessions of an exercise, oldest first. */
        suspend fun exerciseSessions(
            userId: String,
            exerciseId: String,
        ): List<SessionPerformance> =
            workoutDao.getExerciseSetHistory(userId, exerciseId)
                .groupBy { it.workoutId }
                .map { (_, rows) ->
                    val first = rows.first()
                    SessionPerformance(
                        exerciseId = exerciseId,
                        prescriptionId = null,
                        sets = rows.sortedBy { it.orderIndex }.map { SetPerformance(reps = it.reps ?: 0, weight = it.weight) },
                        perceivedEffort = first.perceivedEffort,
                        completedAt = first.performedAt,
                    )
                }
                .sortedBy { it.completedAt }

        /** Recent finished Daily Quest outcomes for the template's exercise, newest first. */
        suspend fun recentQuestOutcomes(
            userId: String,
            exerciseId: String,
            limit: Int = DEFAULT_WINDOW,
        ): List<QuestOutcome> =
            questDao.getRecentQuestOutcomes(userId, exerciseId, limit).map {
                QuestOutcome(
                    questId = it.questId,
                    target = it.target.roundToInt(),
                    completed = it.current.roundToInt(),
                    status = it.status,
                    createdAt = it.createdAt,
                )
            }

        private companion object {
            const val DEFAULT_WINDOW = 5
        }
    }
