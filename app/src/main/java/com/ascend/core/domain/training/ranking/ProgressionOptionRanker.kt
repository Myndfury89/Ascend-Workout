package com.ascend.core.domain.training.ranking

import com.ascend.core.model.ClassProgressionPreference
import com.ascend.core.model.FatigueCost
import com.ascend.core.model.ProgressionCandidate
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.RankedCandidate
import com.ascend.core.model.RankedProgression
import com.ascend.core.model.TrainingReadinessState
import javax.inject.Inject

/** Tunable ranking weights. Class influence only reorders **already-safe** options. */
data class RankingConfig(
    val favoredRankBonus: Double = 1.0,
    val discouragedPenalty: Double = 1.5,
    val secondaryInfluence: Double = 0.4,
    val fatiguePenalty: Double = 0.2,
    val goalBonus: Double = 0.8,
    val preferenceBonus: Double = 0.6,
    val favoredTagBonus: Double = 0.3,
)

/** What the user is training toward, in ranking terms (all optional — absent is neutral). */
data class RankingContext(
    val primaryClassId: String? = null,
    val secondaryClassId: String? = null,
    val goalDimensions: Set<ProgressionDimension> = emptySet(),
    val preferredDimensions: Set<ProgressionDimension> = emptySet(),
    val activityTags: Set<String> = emptySet(),
    val readinessState: TrainingReadinessState = TrainingReadinessState.MAINTAIN,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
)

/**
 * Ranks the safe candidate progressions for one activity into a single primary plus ordered
 * alternatives. Ranking rules:
 * - **Safety and readiness are never touched** — only already-safe changes are ranked, so a
 *   class can never promote an unsafe option.
 * - The primary class biases the order; the secondary class biases it at a **lower**,
 *   configurable influence.
 * - Off-class options are only *pushed down*, never removed — cross-training stays available.
 * - With **no class**, ranking is neutral (fatigue + goal only) and no class influence is
 *   attributed.
 */
class ProgressionOptionRanker
    @Inject
    constructor(
        private val resolver: ClassProgressionPreferenceResolver,
        private val config: RankingConfig,
    ) {
        constructor(resolver: ClassProgressionPreferenceResolver) : this(resolver, RankingConfig())

        fun rank(
            candidates: List<ProgressionCandidate>,
            context: RankingContext,
        ): RankedProgression {
            val safe = candidates.filter { it.isChange && it.isSafe }
            if (safe.isEmpty()) {
                return RankedProgression(null, emptyList(), context.safetyState, context.readinessState)
            }
            val primaryPref = resolver.preferenceFor(context.primaryClassId)
            val secondaryPref = resolver.preferenceFor(context.secondaryClassId)

            val scored =
                safe.map { candidate ->
                    val (score, reasons, classInfluence) = score(candidate, primaryPref, secondaryPref, context)
                    Triple(candidate, score, Pair(reasons, classInfluence))
                }.sortedWith(
                    compareByDescending<Triple<ProgressionCandidate, Double, Pair<List<String>, String?>>> { it.second }
                        .thenBy { it.first.dimension.ordinal },
                )

            val ranked =
                scored.mapIndexed { index, (candidate, score, meta) ->
                    RankedCandidate(
                        rank = index + 1,
                        candidate = candidate,
                        score = score,
                        rankingReasons = meta.first,
                        classInfluence = meta.second,
                    )
                }
            return RankedProgression(ranked.first(), ranked.drop(1), context.safetyState, context.readinessState)
        }

        private fun score(
            candidate: ProgressionCandidate,
            primaryPref: ClassProgressionPreference?,
            secondaryPref: ClassProgressionPreference?,
            context: RankingContext,
        ): Triple<Double, List<String>, String?> {
            val reasons = mutableListOf<String>()
            var score = 0.0

            // Cheaper (less fatiguing) options score higher, all else equal.
            score -= config.fatiguePenalty * fatigueOrdinal(candidate.fatigueCost)

            if (candidate.dimension in context.goalDimensions) {
                score += config.goalBonus
                reasons += "Matches your goal"
            }
            if (candidate.dimension in context.preferredDimensions) {
                score += config.preferenceBonus
                reasons += "Matches your preference"
            }

            val primaryInfluence = classContribution(candidate, primaryPref, 1.0, reasons)
            score += primaryInfluence.first
            val secondaryInfluence = classContribution(candidate, secondaryPref, config.secondaryInfluence, reasons)
            score += secondaryInfluence.first

            val classInfluence = primaryInfluence.second ?: secondaryInfluence.second
            return Triple(score, reasons, classInfluence)
        }

        private fun classContribution(
            candidate: ProgressionCandidate,
            pref: ClassProgressionPreference?,
            weight: Double,
            reasons: MutableList<String>,
        ): Pair<Double, String?> {
            if (pref == null) return 0.0 to null
            var delta = 0.0
            var influence: String? = null

            val favoredIndex = pref.favoredDimensions.indexOf(candidate.dimension)
            if (favoredIndex >= 0) {
                // Earlier in the preference list → bigger bonus.
                val rankBonus = config.favoredRankBonus * (pref.favoredDimensions.size - favoredIndex)
                delta += weight * rankBonus
                influence = "${pref.classId} favours ${candidate.dimension.name.lowercase()}"
                reasons += influence
            }
            if (candidate.dimension in pref.discouragedDimensions ||
                candidate.recommendationType in pref.discouragedRecommendationTypes
            ) {
                delta -= weight * config.discouragedPenalty
                influence = influence ?: "${pref.classId} de-prioritises this option"
                reasons += "${pref.classId} de-prioritises ${candidate.dimension.name.lowercase()}"
            }
            return delta to influence
        }

        private fun fatigueOrdinal(cost: FatigueCost): Int =
            when (cost) {
                FatigueCost.NONE -> 0
                FatigueCost.LOW -> 1
                FatigueCost.MODERATE -> 2
                FatigueCost.HIGH -> 3
            }
    }
