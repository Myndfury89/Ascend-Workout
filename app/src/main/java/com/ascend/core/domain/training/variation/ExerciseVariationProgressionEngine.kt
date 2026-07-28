package com.ascend.core.domain.training.variation

import com.ascend.core.model.EdgeEligibility
import com.ascend.core.model.ExerciseVariationGraph
import com.ascend.core.model.ExerciseVariationProgressionEdge
import com.ascend.core.model.FatigueCost
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.VariationReadinessInput
import javax.inject.Inject

/**
 * Traverses an exercise's variation graph using **real performance**. An advance is only
 * eligible when every performance gate on the edge is met; a satisfied
 * `classUnlockRequirement` is one extra AND-gate, never a shortcut — a class unlock alone
 * can never make an advance eligible. Optional signals (RPE/RIR) only ever *block*, never
 * fabricate readiness, so missing data is safe. Regression is first-class: repeated
 * failure surfaces a safe way back down the graph.
 */
class ExerciseVariationProgressionEngine
    @Inject
    constructor() {
        /** Every advance edge from [currentVariationId], each labelled eligible or not with reasons. */
        fun evaluateAdvances(
            graph: ExerciseVariationGraph,
            currentVariationId: String,
            input: VariationReadinessInput,
        ): List<EdgeEligibility> = graph.advanceEdges(currentVariationId).map { evaluateEdge(graph, it, input) }

        fun evaluateRegressions(
            graph: ExerciseVariationGraph,
            currentVariationId: String,
            input: VariationReadinessInput,
        ): List<EdgeEligibility> = graph.regressEdges(currentVariationId).map { evaluateEdge(graph, it, input) }

        /** The safe advance candidates (may be several — the graph can branch). */
        fun advanceCandidates(
            graph: ExerciseVariationGraph,
            currentVariationId: String,
            input: VariationReadinessInput,
        ): List<ProgressionCandidate> =
            evaluateAdvances(graph, currentVariationId, input)
                .filter { it.eligible }
                .mapNotNull { eligibility ->
                    val dest = eligibility.destination ?: return@mapNotNull null
                    ProgressionCandidate(
                        recommendationType = ProgressionRecommendationType.ADVANCE_VARIATION,
                        dimension = ProgressionDimension.VARIATION,
                        summary = "Advance to ${dest.name}",
                        evidence = eligibility.metRequirements,
                        proposedVariationId = dest.id,
                        fatigueCost = FatigueCost.HIGH,
                    )
                }

        /**
         * A regression candidate when performance has repeatedly broken down. [minFailures]
         * (real failed sessions) gates it — regression is never triggered by one bad day.
         */
        fun regressionCandidate(
            graph: ExerciseVariationGraph,
            currentVariationId: String,
            input: VariationReadinessInput,
            minFailures: Int = 2,
        ): ProgressionCandidate? {
            if (input.consecutiveFailures < minFailures) return null
            val edge = graph.regressEdges(currentVariationId).firstOrNull() ?: return null
            val dest = graph.variation(edge.destinationVariationId) ?: return null
            return ProgressionCandidate(
                recommendationType = ProgressionRecommendationType.REGRESS_VARIATION,
                dimension = ProgressionDimension.REGRESSION,
                summary = "Regress to ${dest.name} to rebuild quality reps",
                evidence = listOf("${input.consecutiveFailures} recent sessions fell short"),
                proposedVariationId = dest.id,
                fatigueCost = FatigueCost.NONE,
            )
        }

        private fun evaluateEdge(
            graph: ExerciseVariationGraph,
            edge: ExerciseVariationProgressionEdge,
            input: VariationReadinessInput,
        ): EdgeEligibility {
            val dest = graph.variation(edge.destinationVariationId)
            val met = mutableListOf<String>()
            val unmet = mutableListOf<String>()

            // Safety always wins — a blocked/stop state makes no advance eligible.
            if (input.safetyState == ProgressionSafetyState.BLOCKED ||
                input.safetyState == ProgressionSafetyState.STOP_AND_SEEK_GUIDANCE
            ) {
                unmet += "Safety state blocks progression"
            }

            check(met, unmet, input.successfulExposures >= edge.minimumSuccessfulExposures) {
                "exposures ${input.successfulExposures}/${edge.minimumSuccessfulExposures}"
            }
            check(met, unmet, input.minCompletedReps >= edge.minimumCompletedReps) {
                "reps ${input.minCompletedReps}/${edge.minimumCompletedReps}"
            }
            check(met, unmet, input.completedSets >= edge.minimumCompletedSets) {
                "sets ${input.completedSets}/${edge.minimumCompletedSets}"
            }
            // Optional signals: a gate only fails when the signal is present and misses it.
            edge.maximumRpe?.let { max ->
                check(met, unmet, input.maxRpe == null || input.maxRpe <= max) { "RPE ${input.maxRpe} <= $max" }
            }
            edge.minimumRir?.let { min ->
                check(met, unmet, input.minRir == null || input.minRir >= min) { "RIR ${input.minRir} >= $min" }
            }
            edge.maximumAssistanceValue?.let { max ->
                check(met, unmet, (input.currentAssistanceValue ?: 0.0) <= max) {
                    "assistance ${input.currentAssistanceValue ?: 0.0} <= $max"
                }
            }
            edge.requiredRangeOfMotion?.let { min ->
                check(met, unmet, input.rangeOfMotionLevel != null && input.rangeOfMotionLevel >= min) {
                    "range of motion ${input.rangeOfMotionLevel}/$min"
                }
            }
            if (edge.requiredTempoControl) {
                check(met, unmet, input.tempoControlled) { "tempo control" }
            }
            // A class unlock is an *additional* gate, only reached because it never stands alone.
            edge.classUnlockRequirement?.let { req ->
                check(met, unmet, req in input.unlockedClassRequirements) { "class unlock '$req'" }
            }

            return EdgeEligibility(
                edge = edge,
                destination = dest,
                eligible = unmet.isEmpty() && dest != null,
                metRequirements = met,
                unmetRequirements = unmet,
            )
        }

        private inline fun check(
            met: MutableList<String>,
            unmet: MutableList<String>,
            condition: Boolean,
            label: () -> String,
        ) {
            if (condition) met += label() else unmet += label()
        }
    }
