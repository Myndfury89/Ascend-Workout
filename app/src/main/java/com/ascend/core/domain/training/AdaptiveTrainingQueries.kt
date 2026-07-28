package com.ascend.core.domain.training

import com.ascend.core.domain.quest.interval.AdaptiveIntervalParams
import com.ascend.core.domain.quest.interval.AdaptiveIntervalRedistributionUseCase
import com.ascend.core.domain.repository.ApplyRecommendationResult
import com.ascend.core.domain.repository.ExerciseVariationGraphRepository
import com.ascend.core.domain.repository.ProgressionRecommendationRepository
import com.ascend.core.domain.training.ranking.ProgressionOptionRanker
import com.ascend.core.domain.training.ranking.RankingContext
import com.ascend.core.model.AdaptiveIntervalRecommendation
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ExerciseVariation
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionRecommendation
import com.ascend.core.model.RankedProgression
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The current movement plus every safe next / previous movement in its graph. */
data class VariationPath(
    val current: ExerciseVariation?,
    val nextOptions: List<ExerciseVariation>,
    val regressionOptions: List<ExerciseVariation>,
)

/**
 * A single, stable entry point for future Adaptive-Training UI. It exposes read models and
 * the accept / reject / apply / redistribute actions by delegating to the already-tested
 * repositories and engines — no UI is built here (that stays deferred), and no new
 * behaviour is introduced, so screens can be layered on later without touching the domain.
 */
class AdaptiveTrainingQueries
    @Inject
    constructor(
        private val recommendations: ProgressionRecommendationRepository,
        private val variationGraphs: ExerciseVariationGraphRepository,
        private val ranker: ProgressionOptionRanker,
        private val redistribution: AdaptiveIntervalRedistributionUseCase,
    ) {
        // ---- read models ----

        fun observePendingRecommendations(userId: String): Flow<List<ProgressionRecommendation>> = recommendations.observePending(userId)

        suspend fun activePrescription(
            userId: String,
            exerciseId: String,
        ): ExercisePrescription? = recommendations.activePrescription(userId, exerciseId)

        /** The current variation and its safe next / previous movements for a UI path view. */
        suspend fun variationPath(
            exerciseId: String,
            currentVariationId: String,
        ): VariationPath {
            val graph = variationGraphs.graphFor(exerciseId)
            return VariationPath(
                current = graph.variation(currentVariationId),
                nextOptions = graph.advanceEdges(currentVariationId).mapNotNull { graph.variation(it.destinationVariationId) },
                regressionOptions = graph.regressEdges(currentVariationId).mapNotNull { graph.variation(it.destinationVariationId) },
            )
        }

        /** Rank pre-generated safe candidates into a primary + alternatives for display. */
        fun rankOptions(
            candidates: List<ProgressionCandidate>,
            context: RankingContext,
        ): RankedProgression = ranker.rank(candidates, context)

        suspend fun pendingIntervalRedistribution(
            questId: String,
            params: AdaptiveIntervalParams,
        ): AdaptiveIntervalRecommendation = redistribution.recommend(questId, params)

        // ---- actions (delegated, already idempotent) ----

        suspend fun acceptRecommendation(id: String): Boolean = recommendations.accept(id)

        suspend fun rejectRecommendation(id: String): Boolean = recommendations.reject(id)

        suspend fun applyRecommendation(id: String): ApplyRecommendationResult = recommendations.apply(id)

        suspend fun markCompleted(
            id: String,
            successful: Boolean,
        ): Boolean = recommendations.markCompleted(id, successful)

        suspend fun applyRedistribution(recommendation: AdaptiveIntervalRecommendation) = redistribution.applyTargets(recommendation)
    }
