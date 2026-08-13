package com.ascend.core.data.repository

import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.common.di.ApplicationScope
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.relation.WorkoutWithSets
import com.ascend.core.domain.build.RefreshBuildProfileUseCase
import com.ascend.core.model.WorkoutStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recompute-on-write for the read-only Build snapshot. Rather than hooking the workout-completion /
 * progression path (which would couple the read-only Build layer into a write path), this observes the
 * evidence itself: whenever the set of *completed* workouts changes, it recomputes and caches the
 * snapshot. So the cache is fresh the moment new verified activity lands — not only when a screen opens
 * — and Build stays strictly read-only (it watches; it never writes training or progression data).
 *
 * The signal keys on completed workouts only, so in-progress editing (logging sets mid-session) does
 * not thrash the pipeline — the recompute fires when a session actually completes.
 */
@Singleton
class BuildProfileRefresher
    @Inject
    constructor(
        private val workoutDao: WorkoutDao,
        private val refreshBuildProfile: RefreshBuildProfileUseCase,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val started = AtomicBoolean(false)

        /** Idempotent: starts observing evidence and refreshing. Safe to call once at app start. */
        fun start(userId: String = LOCAL_USER_ID) {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                workoutDao.observeWorkoutsWithSetsForUser(userId)
                    .map { completedWorkoutSignal(it) }
                    .distinctUntilChanged()
                    // A background observer must never leak an exception to the global handler: an
                    // upstream error (e.g. the DB connection going away on teardown) is swallowed here
                    // rather than crashing the app scope. Recompute-on-view remains a fallback.
                    .catch { }
                    .collectLatest {
                        runCatching { refreshBuildProfile.refresh(userId, System.currentTimeMillis()) }
                    }
            }
        }
    }

/**
 * A change signal derived only from completed workouts (count + latest update time). In-progress edits
 * leave this unchanged, so the recompute fires on completion, not on every logged set.
 */
internal fun completedWorkoutSignal(workouts: List<WorkoutWithSets>): Pair<Int, Long> {
    val completed = workouts.filter { it.workout.status == WorkoutStatus.COMPLETED.name }
    return completed.size to (completed.maxOfOrNull { it.workout.updatedAt } ?: 0L)
}
