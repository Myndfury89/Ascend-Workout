package com.ascend.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ascend.core.database.entity.ExerciseVariationEdgeEntity
import com.ascend.core.database.entity.ExerciseVariationEntity

@Dao
interface ExerciseVariationDao {
    @Upsert
    suspend fun upsertVariations(entities: List<ExerciseVariationEntity>)

    @Upsert
    suspend fun upsertEdges(entities: List<ExerciseVariationEdgeEntity>)

    @Query("SELECT COUNT(*) FROM exercise_variation")
    suspend fun variationCount(): Int

    @Query("SELECT * FROM exercise_variation WHERE exerciseId = :exerciseId")
    suspend fun variationsForExercise(exerciseId: String): List<ExerciseVariationEntity>

    @Query("SELECT * FROM exercise_variation WHERE id = :id")
    suspend fun variation(id: String): ExerciseVariationEntity?

    /** Every edge whose source belongs to the exercise — enough to rebuild its graph. */
    @Query(
        "SELECT e.* FROM exercise_variation_edge e " +
            "JOIN exercise_variation v ON v.id = e.sourceVariationId " +
            "WHERE v.exerciseId = :exerciseId",
    )
    suspend fun edgesForExercise(exerciseId: String): List<ExerciseVariationEdgeEntity>
}
