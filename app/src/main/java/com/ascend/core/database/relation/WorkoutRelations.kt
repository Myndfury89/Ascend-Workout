package com.ascend.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity

data class SetWithExercise(
    @Embedded val set: WorkoutSetEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity?,
)

data class WorkoutWithSets(
    @Embedded val workout: WorkoutEntity,
    @Relation(
        entity = WorkoutSetEntity::class,
        parentColumn = "id",
        entityColumn = "workoutId",
    )
    val sets: List<SetWithExercise>,
)
