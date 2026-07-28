package com.ascend.core.domain.training

import com.ascend.core.domain.training.variation.ExerciseVariationProgressionEngine
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ExerciseVariationGraph
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ReadinessCheckIn
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.VariationReadinessInput
import javax.inject.Inject

/** Everything the bodyweight engine needs for one activity, gathered so callers pass one object. */
data class BodyweightProgressionContext(
    val prescription: ExercisePrescription,
    val sessions: List<SessionPerformance>,
    val checkIn: ReadinessCheckIn = ReadinessCheckIn(),
    val graph: ExerciseVariationGraph? = null,
    val currentVariationId: String? = null,
    val variationInput: VariationReadinessInput? = null,
    val hasVariationControl: Boolean = true,
    val isHeavyStrength: Boolean = false,
    val externalLoadSupported: Boolean = false,
)

/**
 * Generates the full set of **safe** bodyweight/calisthenics candidate progressions for a
 * single activity — one candidate per training variable. It deliberately does **not**
 * fuse several aggressive changes into one recommendation (advancing a variation *and*
 * adding a set *and* cutting rest, say): each candidate moves exactly one dimension, and
 * the ranker (a later stage) still surfaces only one primary. This keeps "one primary
 * variable at a time" true by construction.
 */
class BodyweightProgressionCalculator
    @Inject
    constructor(
        private val repCalculator: RepProgressionCalculator,
        private val setCalculator: SetProgressionCalculator,
        private val restCalculator: RestProgressionCalculator,
        private val assistanceCalculator: AssistanceProgressionCalculator,
        private val externalLoadCalculator: ExternalLoadProgressionCalculator,
        private val tempoCalculator: TempoProgressionCalculator,
        private val variationEngine: ExerciseVariationProgressionEngine,
    ) {
        /** All candidates (changes and holds), one per dimension, most-conservative variable first. */
        fun candidates(context: BodyweightProgressionContext): List<ProgressionCandidate> {
            val p = context.prescription
            val out = mutableListOf<ProgressionCandidate>()

            out += repCalculator.evaluate(p, context.sessions, context.checkIn)
            out += setCalculator.evaluate(p, context.sessions, context.checkIn)
            out += restCalculator.evaluate(p, context.sessions, context.checkIn, context.isHeavyStrength)
            out += assistanceCalculator.evaluate(p, context.sessions, context.checkIn)
            out += externalLoadCalculator.evaluate(p, context.sessions, context.checkIn, context.externalLoadSupported)
            out += tempoCalculator.candidates(p, context.sessions, context.checkIn, context.hasVariationControl)

            if (context.graph != null && context.currentVariationId != null && context.variationInput != null) {
                out += variationEngine.advanceCandidates(context.graph, context.currentVariationId, context.variationInput)
                variationEngine.regressionCandidate(context.graph, context.currentVariationId, context.variationInput)
                    ?.let { out += it }
            }
            return out
        }

        /**
         * The safe, actionable changes — one per dimension. When advancing the variation is
         * available it is the dominant stimulus, so tempo/range-of-motion tweaks are dropped
         * (they don't stack with a variation advance by default). Each remaining candidate is
         * still atomic — one variable — so no aggressive changes are ever fused together.
         */
        fun safeChangeCandidates(context: BodyweightProgressionContext): List<ProgressionCandidate> {
            val all = candidates(context).filter { it.isChange && it.isSafe }.distinctBy { it.dimension }
            val hasVariationAdvance = all.any { it.dimension == ProgressionDimension.VARIATION }
            return if (hasVariationAdvance) {
                all.filterNot { it.dimension == ProgressionDimension.TEMPO || it.dimension == ProgressionDimension.RANGE_OF_MOTION }
            } else {
                all
            }
        }
    }
