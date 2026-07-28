package com.ascend.core.model

/*
 * The bodyweight / calisthenics progression layer. Progression between movements is
 * data-driven — a directed graph of variations and edges — never inferred from an
 * exercise's name. Each exercise (push-up, pull-up, …) owns its own graph, which may
 * branch (multiple valid next movements) and regress.
 */

/** How a variation is made easier, if at all. Plain data so new kinds need no engine change. */
enum class AssistanceType {
    NONE,
    INCLINE,
    KNEE,
    BAND,
    MACHINE,
    PARTNER,
}

/** The direction of a variation edge. The graph carries both so regression is first-class. */
enum class VariationProgressionType {
    ADVANCE,
    REGRESS,
}

/**
 * One node in an exercise's variation graph (e.g. "Standard Push-Up"). [difficultyTier]
 * only orders siblings for display/sanity; readiness is never derived from it.
 */
data class ExerciseVariation(
    val id: String,
    val exerciseId: String,
    val name: String,
    val description: String,
    val difficultyTier: Int,
    val variationTags: List<String>,
    val assistanceType: AssistanceType,
    val assistanceValue: Double?,
    val externalLoadSupported: Boolean,
    val rangeOfMotionLevel: Int,
    val tempoProfile: String,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)

/**
 * A directed transition between two variations with the **real-performance** gate that
 * must be met to take it. A [classUnlockRequirement], when present, is an *additional*
 * gate — never a substitute for the performance requirements (a class unlock alone can
 * never create readiness).
 */
data class ExerciseVariationProgressionEdge(
    val id: String,
    val sourceVariationId: String,
    val destinationVariationId: String,
    val progressionType: VariationProgressionType,
    val minimumSuccessfulExposures: Int,
    val minimumCompletedReps: Int,
    val minimumCompletedSets: Int,
    val maximumRpe: Double? = null,
    val minimumRir: Int? = null,
    val maximumAssistanceValue: Double? = null,
    val requiredRangeOfMotion: Int? = null,
    val requiredTempoControl: Boolean = false,
    val classUnlockRequirement: String? = null,
    val safetyNotes: String? = null,
    val enabled: Boolean = true,
)

/**
 * An exercise's full variation graph. Pure and side-effect free so the progression
 * engine can be unit-tested without a database. Only [enabled] variations/edges are
 * traversable.
 */
data class ExerciseVariationGraph(
    val exerciseId: String,
    val variations: List<ExerciseVariation>,
    val edges: List<ExerciseVariationProgressionEdge>,
) {
    fun variation(id: String): ExerciseVariation? = variations.firstOrNull { it.id == id && it.enabled }

    /** Enabled outgoing edges of a given direction from [variationId] (whose endpoints both exist). */
    fun outgoing(
        variationId: String,
        direction: VariationProgressionType,
    ): List<ExerciseVariationProgressionEdge> =
        edges.filter {
            it.enabled &&
                it.progressionType == direction &&
                it.sourceVariationId == variationId &&
                variation(it.destinationVariationId) != null
        }

    fun advanceEdges(variationId: String): List<ExerciseVariationProgressionEdge> = outgoing(variationId, VariationProgressionType.ADVANCE)

    fun regressEdges(variationId: String): List<ExerciseVariationProgressionEdge> = outgoing(variationId, VariationProgressionType.REGRESS)

    /** True when [destinationId] is reachable from [sourceId] via a single enabled edge. */
    fun hasEdge(
        sourceId: String,
        destinationId: String,
        direction: VariationProgressionType,
    ): Boolean = outgoing(sourceId, direction).any { it.destinationVariationId == destinationId }
}

/**
 * A recent-performance summary for a single variation, distilled from real sessions.
 * Optional signals stay nullable — a missing RPE/RIR simply can't *satisfy* a gate that
 * requires it, and never fabricates readiness. Used to test variation edges.
 */
data class VariationReadinessInput(
    val successfulExposures: Int,
    val minCompletedReps: Int,
    val completedSets: Int,
    val maxRpe: Double? = null,
    val minRir: Int? = null,
    val currentAssistanceValue: Double? = null,
    val rangeOfMotionLevel: Int? = null,
    val tempoControlled: Boolean = false,
    val consecutiveFailures: Int = 0,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
    val unlockedClassRequirements: Set<String> = emptySet(),
)

/** The result of testing one edge against real performance, with the reasons on both sides. */
data class EdgeEligibility(
    val edge: ExerciseVariationProgressionEdge,
    val destination: ExerciseVariation?,
    val eligible: Boolean,
    val metRequirements: List<String>,
    val unmetRequirements: List<String>,
)
