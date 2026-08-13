package com.ascend.core.data.repository

import com.ascend.core.database.dao.AdaptiveTrainingDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.relation.SetWithExercise
import com.ascend.core.database.relation.WorkoutWithSets
import com.ascend.core.domain.build.ActivityFamily
import com.ascend.core.domain.build.ActivityModality
import com.ascend.core.domain.build.BuildEvidence
import com.ascend.core.domain.build.BuildEvidenceProvider
import com.ascend.core.domain.build.DistanceEvidence
import com.ascend.core.domain.build.EvidenceSource
import com.ascend.core.domain.build.FamilyEvidence
import com.ascend.core.domain.build.PaceEvidence
import com.ascend.core.domain.build.RecoveryKind
import com.ascend.core.domain.build.RecoverySignal
import com.ascend.core.domain.build.SessionEvidence
import com.ascend.core.domain.build.StrengthSetEvidence
import com.ascend.core.model.WorkoutStatus
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val DAY_MS = 86_400_000L
private const val RECOVERY_LOOKBACK_DAYS = 60L
private const val READINESS_FRACTION_MAX = 1.0
private const val PERCENT = 100.0
private const val EPLEY_REPS_DIVISOR = 30.0

/**
 * Gathers read-only Build evidence from Ascend-native tables (completed workouts + their sets, and
 * readiness snapshots). It only reads; it never writes training or progression data. A Health Connect
 * provider can later feed the same [BuildEvidence] shapes without changing the engine.
 *
 * Evidence-quality windowing (28d/56d) is applied downstream by the engine; this provider simply
 * surfaces recent verified evidence. Personal-record detection is not wired yet (a bounded, optional
 * Strength contribution) — it can be added without changing this shape.
 */
class AscendBuildEvidenceProvider
    @Inject
    constructor(
        private val workoutDao: WorkoutDao,
        private val adaptiveTrainingDao: AdaptiveTrainingDao,
    ) : BuildEvidenceProvider {
        override suspend fun gather(userId: String): BuildEvidence {
            val workouts =
                workoutDao.observeWorkoutsWithSetsForUser(userId).first()
                    .filter { it.workout.status == WorkoutStatus.COMPLETED.name }

            val sessions = mutableListOf<SessionEvidence>()
            val strengthSets = mutableListOf<StrengthSetEvidence>()
            val families = mutableListOf<FamilyEvidence>()
            val distances = mutableListOf<DistanceEvidence>()
            val paces = mutableListOf<PaceEvidence>()
            val personalRecordSetIds = personalRecordSetIds(workouts)

            workouts.forEach { ws ->
                val workout = ws.workout
                val durationSeconds = workout.durationSeconds ?: ws.sets.sumOf { it.set.durationSeconds ?: 0L }
                sessions += SessionEvidence(workout.performedAt, durationSeconds, EvidenceSource.ASCEND_WORKOUT)
                families += FamilyEvidence(workout.performedAt, dominantFamily(ws.sets), durationSeconds, EvidenceSource.ASCEND_WORKOUT)

                ws.sets.forEach { swe ->
                    val set = swe.set
                    if (isStrength(swe.exercise) && set.volume > 0.0) {
                        strengthSets +=
                            StrengthSetEvidence(
                                at = set.completedAt,
                                volume = set.volume,
                                isPersonalRecord = set.id in personalRecordSetIds,
                                source = EvidenceSource.ASCEND_WORKOUT,
                            )
                    }
                    val meters = set.distance
                    if (meters != null && meters > 0.0) {
                        val modality = modalityOf(swe.exercise)
                        distances += DistanceEvidence(set.completedAt, meters, modality, EvidenceSource.ASCEND_CARDIO)
                        val seconds = set.durationSeconds
                        if (seconds != null && seconds > 0L) {
                            paces += PaceEvidence(set.completedAt, meters / seconds.toDouble(), modality, EvidenceSource.ASCEND_CARDIO)
                        }
                    }
                }
            }

            val since = System.currentTimeMillis() - RECOVERY_LOOKBACK_DAYS * DAY_MS
            val recovery =
                adaptiveTrainingDao.getReadinessSnapshotsForUser(userId, since).map {
                    RecoverySignal(
                        at = it.createdAt,
                        kind = RecoveryKind.READINESS_SNAPSHOT,
                        // Readiness score may be stored 0..1 or 0..100; normalize to the 0..100 the engine expects.
                        readinessScore = if (it.score <= READINESS_FRACTION_MAX) it.score * PERCENT else it.score,
                        source = EvidenceSource.ASCEND_WORKOUT,
                    )
                }

            return BuildEvidence(
                sessions = sessions,
                strengthSets = strengthSets,
                families = families,
                distances = distances,
                paces = paces,
                recovery = recovery,
            )
        }

        /** A session's identity is its dominant modality; a mixed session prefers its non-gym family. */
        private fun dominantFamily(sets: List<SetWithExercise>): ActivityFamily {
            val fams = sets.mapNotNull { swe -> swe.exercise?.let(::exerciseFamily) }
            if (fams.isEmpty()) return ActivityFamily.TRADITIONAL_STRENGTH
            val pool = fams.filterNot { it.isBaseline }.ifEmpty { fams }
            return pool.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
        }

        private fun exerciseFamily(exercise: ExerciseEntity): ActivityFamily {
            val tags = exercise.tags.split(",").map { it.trim().uppercase() }.toSet()
            return when {
                "COMBAT" in tags -> ActivityFamily.COMBAT
                "AQUATIC" in tags -> ActivityFamily.AQUATIC
                exercise.id == "ex-row" -> ActivityFamily.ROW
                "MOBILITY" in tags || "RECOVERY" in tags || exercise.category.equals("Recovery", ignoreCase = true) ->
                    ActivityFamily.MOBILITY
                exercise.category.equals("Weights", ignoreCase = true) || exercise.isWeighted ||
                    "HEAVY_STRENGTH" in tags || "HYPERTROPHY" in tags -> ActivityFamily.TRADITIONAL_STRENGTH
                "BODYWEIGHT" in tags -> ActivityFamily.BODYWEIGHT
                exercise.measurementType == "DISTANCE" || "STEADY_STATE_CARDIO" in tags -> ActivityFamily.RUN_WALK
                else -> ActivityFamily.CONDITIONING
            }
        }

        private fun modalityOf(exercise: ExerciseEntity?): ActivityModality =
            when {
                exercise == null -> ActivityModality.OTHER
                exercise.id == "ex-row" -> ActivityModality.ROW
                exercise.id.contains("cycl", ignoreCase = true) || exercise.name.contains("cycl", ignoreCase = true) ->
                    ActivityModality.CYCLE
                exercise.id.contains("swim", ignoreCase = true) || "AQUATIC" in exercise.tags.uppercase() ->
                    ActivityModality.SWIM
                exercise.name.contains("walk", ignoreCase = true) -> ActivityModality.WALK
                else -> ActivityModality.RUN
            }

        private fun isStrength(exercise: ExerciseEntity?): Boolean =
            exercise != null &&
                (exercise.isWeighted || exercise.category.equals("Weights", ignoreCase = true) || exercise.primaryAttribute == "STRENGTH")

        /**
         * Verified personal records from Ascend-native history: per strength exercise, walk the user's
         * completed weighted sets in chronological order and flag a set as a PR when its estimated 1RM
         * (Epley: weight x (1 + reps/30)) strictly beats every earlier set of that exercise. The first
         * set of an exercise is a baseline, never a PR — so simply trying a new movement grants nothing.
         * These flags feed the engine's already-capped PR contribution, which bounds their effect.
         */
        private fun personalRecordSetIds(workouts: List<WorkoutWithSets>): Set<String> {
            data class TimedSet(val id: String, val at: Long, val order: Int, val oneRepMax: Double)

            val byExercise = HashMap<String, MutableList<TimedSet>>()
            workouts.forEach { ws ->
                ws.sets.forEach { swe ->
                    val set = swe.set
                    val weight = set.weight
                    val reps = set.reps
                    if (isStrength(swe.exercise) && weight != null && weight > 0.0 && reps != null && reps > 0) {
                        byExercise.getOrPut(set.exerciseId) { mutableListOf() }
                            .add(TimedSet(set.id, ws.workout.performedAt, set.orderIndex, weight * (1.0 + reps / EPLEY_REPS_DIVISOR)))
                    }
                }
            }
            val records = HashSet<String>()
            byExercise.values.forEach { history ->
                history.sortWith(compareBy({ it.at }, { it.order }))
                var best = Double.NEGATIVE_INFINITY
                history.forEach { timed ->
                    if (timed.oneRepMax > best) {
                        if (best != Double.NEGATIVE_INFINITY) records += timed.id
                        best = timed.oneRepMax
                    }
                }
            }
            return records
        }
    }
