package com.ascend.core.domain.repository

import com.ascend.core.model.ExerciseVariationGraph

/**
 * Loads an exercise's persisted variation graph and seeds the built-in graphs. The
 * engine consumes a pure [ExerciseVariationGraph]; the DB is only how the graph is stored
 * and (later) user-extended.
 */
interface ExerciseVariationGraphRepository {
    /** The stored graph for an exercise (empty graph if none). */
    suspend fun graphFor(exerciseId: String): ExerciseVariationGraph

    /** Seeds the built-in variation graphs idempotently (stable ids → upsert). */
    suspend fun seedBuiltInGraphs()
}
