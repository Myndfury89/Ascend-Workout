package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.relation.SetWithExercise
import com.ascend.core.database.relation.WorkoutWithSets
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Exercise
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.Workout
import com.ascend.core.model.WorkoutSet
import com.ascend.core.model.WorkoutStatus

private inline fun <reified T : Enum<T>> String?.toEnumOrNull(): T? = this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T = toEnumOrNull<T>() ?: default

fun ExerciseEntity.toDomain(): Exercise =
    Exercise(
        id = id,
        name = name,
        category = category,
        primaryAttribute = primaryAttribute.toEnum(AttributeType.STRENGTH),
        measurementType = measurementType.toEnum(ObjectiveType.REPETITIONS),
        defaultUnit = defaultUnit,
        isWeighted = isWeighted,
        preferredSetSize = preferredSetSize,
        tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(),
        isBuiltIn = isBuiltIn,
    )

fun SetWithExercise.toDomain(): WorkoutSet =
    WorkoutSet(
        id = set.id,
        workoutId = set.workoutId,
        exerciseId = set.exerciseId,
        exerciseName = exercise?.name ?: "Exercise",
        primaryAttribute = exercise?.primaryAttribute.toEnumOrNull<AttributeType>(),
        orderIndex = set.orderIndex,
        reps = set.reps,
        weight = set.weight,
        durationSeconds = set.durationSeconds,
        distance = set.distance,
        volume = set.volume,
        unit = set.unit,
        completedAt = set.completedAt,
    )

fun WorkoutWithSets.toDomain(): Workout =
    Workout(
        id = workout.id,
        userId = workout.userId,
        title = workout.title,
        notes = workout.notes,
        difficulty = workout.difficulty.toEnum(Difficulty.MODERATE),
        status = workout.status.toEnum(WorkoutStatus.IN_PROGRESS),
        performedAt = workout.performedAt,
        durationSeconds = workout.durationSeconds,
        sets = sets.sortedBy { it.set.orderIndex }.map { it.toDomain() },
    )
