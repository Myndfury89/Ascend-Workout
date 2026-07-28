package com.ascend.core.domain.training.variation

import com.ascend.core.model.AssistanceType
import com.ascend.core.model.ExerciseVariation
import com.ascend.core.model.ExerciseVariationGraph
import com.ascend.core.model.ExerciseVariationProgressionEdge
import com.ascend.core.model.VariationProgressionType

/**
 * The initial bodyweight variation graphs as **configurable seed data** — the single
 * tunable source, kept out of the engine (which reads a graph and never branches on an
 * id). Two starter graphs (push-up, pull-up). Both branch (multiple valid next
 * movements) and carry regression edges, so the graph is not a single linear chain.
 * New graphs (dips, squats, rows) can be added here — or as DB rows — with no engine
 * change.
 */
object ExerciseVariationCatalog {
    // ---- Push-up graph (ex-pushup) ----

    private val pushUps =
        listOf(
            variation("var-pushup-wall", "ex-pushup", "Wall Push-Up", 1, "Standing lean into a wall.", rom = 2),
            variation(
                "var-pushup-incline",
                "ex-pushup",
                "Incline Push-Up",
                2,
                "Hands elevated on a bench.",
                AssistanceType.INCLINE,
                45.0,
                rom = 3,
            ),
            variation("var-pushup-knee", "ex-pushup", "Knee Push-Up", 2, "From the knees.", AssistanceType.KNEE, 40.0, rom = 3),
            variation("var-pushup-standard", "ex-pushup", "Standard Push-Up", 3, "Full plank push-up.", rom = 4),
            variation(
                "var-pushup-decline",
                "ex-pushup",
                "Decline Push-Up",
                4,
                "Feet elevated.",
                rom = 4,
                externalLoad = true,
            ),
            variation("var-pushup-diamond", "ex-pushup", "Diamond Push-Up", 4, "Hands together under the chest.", rom = 4),
            variation("var-pushup-archer", "ex-pushup", "Archer Push-Up", 5, "Weight shifted to one arm.", rom = 5),
            variation(
                "var-pushup-assisted-onearm",
                "ex-pushup",
                "Assisted One-Arm Push-Up",
                6,
                "One arm with light support.",
                AssistanceType.PARTNER,
                20.0,
                rom = 5,
            ),
            variation("var-pushup-onearm", "ex-pushup", "One-Arm Push-Up", 7, "Full one-arm push-up.", rom = 5),
        )

    private val pushUpProgressions =
        listOf(
            advance("var-pushup-wall", "var-pushup-incline", reps = 12, sets = 2),
            advance("var-pushup-incline", "var-pushup-standard", reps = 12, sets = 3),
            advance("var-pushup-knee", "var-pushup-standard", reps = 12, sets = 3),
            advance("var-pushup-standard", "var-pushup-decline", reps = 12, sets = 3, rpe = 8.0),
            advance("var-pushup-standard", "var-pushup-diamond", reps = 12, sets = 3, rpe = 8.0),
            advance("var-pushup-decline", "var-pushup-archer", reps = 10, sets = 3, rpe = 8.0),
            advance("var-pushup-archer", "var-pushup-assisted-onearm", reps = 8, sets = 3, rpe = 8.0),
            // Base graph edges carry no class restriction — any class (or none) can progress
            // through them on performance readiness + safety alone.
            advance("var-pushup-assisted-onearm", "var-pushup-onearm", reps = 5, sets = 3, rpe = 8.0),
        )

    // ---- Pull-up graph (ex-pullup) ----

    private val pullUps =
        listOf(
            variation(
                "var-pullup-machine",
                "ex-pullup",
                "Machine-Assisted Pull-Up",
                1,
                "Assisted pull-up machine.",
                AssistanceType.MACHINE,
                45.0,
                rom = 4,
            ),
            variation(
                "var-pullup-heavyband",
                "ex-pullup",
                "Heavy Band-Assisted Pull-Up",
                2,
                "Thick band assistance.",
                AssistanceType.BAND,
                30.0,
                rom = 4,
            ),
            variation(
                "var-pullup-lightband",
                "ex-pullup",
                "Light Band-Assisted Pull-Up",
                3,
                "Thin band assistance.",
                AssistanceType.BAND,
                12.0,
                rom = 4,
            ),
            variation("var-pullup-bodyweight", "ex-pullup", "Bodyweight Pull-Up", 4, "Unassisted pull-up.", rom = 4),
            variation("var-pullup-c2b", "ex-pullup", "Chest-to-Bar Pull-Up", 5, "Pull to chest contact.", rom = 5),
            variation("var-pullup-weighted", "ex-pullup", "Weighted Pull-Up", 6, "Added external load.", rom = 4, externalLoad = true),
        )

    private val pullUpProgressions =
        listOf(
            advance("var-pullup-machine", "var-pullup-heavyband", reps = 8, sets = 3),
            advance("var-pullup-heavyband", "var-pullup-lightband", reps = 8, sets = 3),
            advance("var-pullup-lightband", "var-pullup-bodyweight", reps = 6, sets = 3, rpe = 8.0),
            advance("var-pullup-bodyweight", "var-pullup-c2b", reps = 8, sets = 3, rpe = 8.0),
            // Bodyweight can branch straight to weighted work (an alternate path to chest-to-bar).
            advance("var-pullup-bodyweight", "var-pullup-weighted", reps = 8, sets = 3, rpe = 8.0),
            advance("var-pullup-c2b", "var-pullup-weighted", reps = 8, sets = 3, rpe = 8.0),
        )

    /** Every seeded variation across all graphs (stable ids → idempotent upsert). */
    val ALL_VARIATIONS: List<ExerciseVariation> = pushUps + pullUps

    /**
     * Every seeded edge. Each advance edge is mirrored by a lenient regression edge so a
     * struggling athlete always has a safe way back down the graph.
     */
    val ALL_EDGES: List<ExerciseVariationProgressionEdge> =
        (pushUpProgressions + pullUpProgressions).flatMap { listOf(it, regressionOf(it)) }

    fun graphFor(exerciseId: String): ExerciseVariationGraph =
        ExerciseVariationGraph(
            exerciseId = exerciseId,
            variations = ALL_VARIATIONS.filter { it.exerciseId == exerciseId },
            edges = ALL_EDGES.filter { edgeBelongsTo(it, exerciseId) },
        )

    private fun edgeBelongsTo(
        edge: ExerciseVariationProgressionEdge,
        exerciseId: String,
    ): Boolean = ALL_VARIATIONS.any { it.id == edge.sourceVariationId && it.exerciseId == exerciseId }

    // ---- builders ----

    private fun variation(
        id: String,
        exerciseId: String,
        name: String,
        tier: Int,
        description: String,
        assistanceType: AssistanceType = AssistanceType.NONE,
        assistanceValue: Double? = null,
        rom: Int = 4,
        externalLoad: Boolean = false,
    ) = ExerciseVariation(
        id = id,
        exerciseId = exerciseId,
        name = name,
        description = description,
        difficultyTier = tier,
        variationTags = listOf("BODYWEIGHT"),
        assistanceType = assistanceType,
        assistanceValue = assistanceValue,
        externalLoadSupported = externalLoad,
        rangeOfMotionLevel = rom,
        tempoProfile = "2-0-1",
    )

    private fun advance(
        source: String,
        destination: String,
        reps: Int,
        sets: Int,
        rpe: Double? = null,
        classUnlock: String? = null,
    ) = ExerciseVariationProgressionEdge(
        id = "edge-$source->$destination",
        sourceVariationId = source,
        destinationVariationId = destination,
        progressionType = VariationProgressionType.ADVANCE,
        minimumSuccessfulExposures = 2,
        minimumCompletedReps = reps,
        minimumCompletedSets = sets,
        maximumRpe = rpe,
        requiredTempoControl = true,
        classUnlockRequirement = classUnlock,
        safetyNotes = "Advance only with full range of motion and no pain.",
    )

    /** A regression is a safety valve — no rep/RPE bar to clear, just the reverse path. */
    private fun regressionOf(edge: ExerciseVariationProgressionEdge) =
        ExerciseVariationProgressionEdge(
            id = "edge-${edge.destinationVariationId}<-${edge.sourceVariationId}",
            sourceVariationId = edge.destinationVariationId,
            destinationVariationId = edge.sourceVariationId,
            progressionType = VariationProgressionType.REGRESS,
            minimumSuccessfulExposures = 0,
            minimumCompletedReps = 0,
            minimumCompletedSets = 0,
            requiredTempoControl = false,
            safetyNotes = "Regress to rebuild quality reps.",
        )
}
