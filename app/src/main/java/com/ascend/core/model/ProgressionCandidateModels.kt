package com.ascend.core.model

/*
 * A single, safe progression option produced by one calculator. Many candidates may be
 * generated for one activity; readiness/safety removes the unsafe ones, and the ranker
 * (class + goal + fatigue) orders what remains into a primary plus alternatives. A
 * candidate that is not a change (maintain / request-more-data) is carried too so the
 * caller always has an explainable answer.
 */

/** The training variable a candidate moves. Used for de-duplication, ranking, and rewards. */
enum class ProgressionDimension {
    LOAD,
    REPS,
    SETS,
    REST,
    ASSISTANCE,
    TEMPO,
    RANGE_OF_MOTION,
    VARIATION,
    EXTERNAL_LOAD,
    CARDIO_DURATION,
    CARDIO_DISTANCE,
    CARDIO_PACE,
    CARDIO_INCLINE,
    CARDIO_RESISTANCE,
    CARDIO_INTERVAL,
    DAILY_QUEST_TARGET,
    MAINTAIN,
    DELOAD,
    REGRESSION,
}

/** A relative, ordinal estimate of how much fatigue a change adds. Lower is cheaper. */
enum class FatigueCost {
    NONE,
    LOW,
    MODERATE,
    HIGH,
}

data class ProgressionCandidate(
    val recommendationType: ProgressionRecommendationType,
    val dimension: ProgressionDimension,
    val summary: String,
    val evidence: List<String> = emptyList(),
    val proposed: ExercisePrescription? = null,
    val proposedTarget: Int? = null,
    val proposedVariationId: String? = null,
    val fatigueCost: FatigueCost = FatigueCost.LOW,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
    val requiresMoreData: Boolean = false,
) {
    /** A real change to the prescription (not a hold or a data request). */
    val isChange: Boolean
        get() =
            !requiresMoreData &&
                recommendationType != ProgressionRecommendationType.MAINTAIN_PRESCRIPTION

    /** Safe enough to offer to the user (safety never lets an unsafe option through). */
    val isSafe: Boolean
        get() = safetyState == ProgressionSafetyState.OK || safetyState == ProgressionSafetyState.CAUTION
}

/**
 * A ranked progression decision for one activity: exactly one primary recommendation and
 * the remaining safe alternatives, each annotated with why it placed where it did. The
 * whole set survives so a future UI can offer "choose an alternative" without recomputing.
 */
data class RankedProgression(
    val primary: RankedCandidate?,
    val alternatives: List<RankedCandidate>,
    val safetyState: ProgressionSafetyState,
    val readinessState: TrainingReadinessState,
) {
    val hasRecommendation: Boolean get() = primary != null
    val all: List<RankedCandidate> get() = listOfNotNull(primary) + alternatives
}

/** One candidate with its rank and the ranking rationale (class influence made explicit). */
data class RankedCandidate(
    val rank: Int,
    val candidate: ProgressionCandidate,
    val score: Double,
    val rankingReasons: List<String>,
    val classInfluence: String?,
)
