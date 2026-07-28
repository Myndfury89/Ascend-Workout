package com.ascend.core.data.repository

import com.ascend.core.data.mapper.toDomain
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.dao.ExerciseVariationDao
import com.ascend.core.domain.repository.ExerciseVariationGraphRepository
import com.ascend.core.domain.training.variation.ExerciseVariationCatalog
import com.ascend.core.model.ExerciseVariationGraph
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseVariationGraphRepositoryImpl
    @Inject
    constructor(
        private val dao: ExerciseVariationDao,
    ) : ExerciseVariationGraphRepository {
        override suspend fun graphFor(exerciseId: String): ExerciseVariationGraph =
            ExerciseVariationGraph(
                exerciseId = exerciseId,
                variations = dao.variationsForExercise(exerciseId).map { it.toDomain() },
                edges = dao.edgesForExercise(exerciseId).map { it.toDomain() },
            )

        override suspend fun seedBuiltInGraphs() {
            if (dao.variationCount() > 0) return
            val now = System.currentTimeMillis()
            dao.upsertVariations(ExerciseVariationCatalog.ALL_VARIATIONS.map { it.toEntity(now) })
            dao.upsertEdges(ExerciseVariationCatalog.ALL_EDGES.map { it.toEntity() })
        }
    }
