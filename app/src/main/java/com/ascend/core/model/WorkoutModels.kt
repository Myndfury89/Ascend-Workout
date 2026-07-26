package com.ascend.core.model

/** Lifecycle status of a logged workout session. */
enum class WorkoutStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

/** A movement in the exercise library. */
data class Exercise(
    val id: String,
    val name: String,
    val category: String,
    val primaryAttribute: AttributeType,
    val measurementType: ObjectiveType,
    val defaultUnit: String,
    val isWeighted: Boolean,
    val preferredSetSize: Int?,
    val tags: Set<String>,
    val isBuiltIn: Boolean,
)

/** A single set within a workout. */
data class WorkoutSet(
    val id: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val primaryAttribute: AttributeType?,
    val orderIndex: Int,
    val reps: Int?,
    val weight: Double?,
    val durationSeconds: Long?,
    val distance: Double?,
    val volume: Double,
    val unit: String,
    val completedAt: Long,
)

/** A logged training session composed of [sets]. */
data class Workout(
    val id: String,
    val userId: String,
    val title: String,
    val notes: String?,
    val difficulty: Difficulty,
    val status: WorkoutStatus,
    val performedAt: Long,
    val durationSeconds: Long?,
    val sets: List<WorkoutSet>,
) {
    val setCount: Int get() = sets.size
    val totalVolume: Double get() = sets.sumOf { it.volume }
    val isCompleted: Boolean get() = status == WorkoutStatus.COMPLETED

    /** Total training volume per primary attribute, used to grow attributes. */
    val volumeByAttribute: Map<AttributeType, Double>
        get() =
            sets
                .filter { it.primaryAttribute != null }
                .groupBy { it.primaryAttribute!! }
                .mapValues { (_, group) -> group.sumOf { it.volume } }
}
