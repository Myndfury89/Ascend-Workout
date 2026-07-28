package com.ascend.core.domain.training

import com.ascend.core.domain.training.variation.ExerciseVariationCatalog
import com.ascend.core.domain.training.variation.ExerciseVariationProgressionEngine
import com.ascend.core.model.ExerciseVariation
import com.ascend.core.model.ExerciseVariationGraph
import com.ascend.core.model.ExerciseVariationProgressionEdge
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.VariationProgressionType
import com.ascend.core.model.VariationReadinessInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseVariationGraphTest {
    private val engine = ExerciseVariationProgressionEngine()
    private val pushUps = ExerciseVariationCatalog.graphFor("ex-pushup")
    private val pullUps = ExerciseVariationCatalog.graphFor("ex-pullup")

    // Meets every gate on the standard push-up advance edges (reps 12, sets 3, RPE <= 8, tempo).
    private fun strong(
        exposures: Int = 2,
        reps: Int = 12,
        sets: Int = 3,
        rpe: Double? = 7.0,
        rir: Int? = null,
        assistance: Double? = null,
        rom: Int? = 5,
        tempo: Boolean = true,
        failures: Int = 0,
        safety: ProgressionSafetyState = ProgressionSafetyState.OK,
        unlocks: Set<String> = emptySet(),
    ) = VariationReadinessInput(
        successfulExposures = exposures, minCompletedReps = reps, completedSets = sets, maxRpe = rpe,
        minRir = rir, currentAssistanceValue = assistance, rangeOfMotionLevel = rom, tempoControlled = tempo,
        consecutiveFailures = failures, safetyState = safety, unlockedClassRequirements = unlocks,
    )

    @Test
    fun `a valid progression edge is eligible when performance meets it`() {
        val advances = engine.advanceCandidates(pushUps, "var-pushup-standard", strong())
        assertTrue(advances.any { it.proposedVariationId == "var-pushup-decline" })
    }

    @Test
    fun `an exercise offers multiple valid next paths, not a single chain`() {
        val destinations = engine.advanceCandidates(pushUps, "var-pushup-standard", strong()).map { it.proposedVariationId }
        assertTrue("standard should branch to decline", destinations.contains("var-pushup-decline"))
        assertTrue("standard should branch to diamond", destinations.contains("var-pushup-diamond"))
    }

    @Test
    fun `a nonexistent edge is not eligible`() {
        // The top of the graph has no outgoing advances.
        assertTrue(engine.advanceCandidates(pushUps, "var-pushup-onearm", strong()).isEmpty())
        assertFalse(pushUps.hasEdge("var-pushup-standard", "var-pushup-onearm", VariationProgressionType.ADVANCE))
    }

    @Test
    fun `a regression edge exists and is offered after repeated failure`() {
        val regress = engine.regressionCandidate(pushUps, "var-pushup-standard", strong(failures = 2))
        assertNotNull(regress)
        assertTrue(pushUps.regressEdges("var-pushup-standard").isNotEmpty())
    }

    @Test
    fun `a single failure does not trigger a regression`() {
        assertEquals(null, engine.regressionCandidate(pushUps, "var-pushup-standard", strong(failures = 1)))
    }

    @Test
    fun `a disabled edge is never traversable`() {
        val a = variation("a")
        val b = variation("b")
        val graph =
            ExerciseVariationGraph(
                "ex-x",
                listOf(a, b),
                listOf(edge("a", "b", enabled = false)),
            )
        assertTrue(engine.advanceCandidates(graph, "a", strong()).isEmpty())
    }

    @Test
    fun `insufficient exposures block progression`() {
        assertTrue(engine.advanceCandidates(pushUps, "var-pushup-standard", strong(exposures = 1)).isEmpty())
    }

    @Test
    fun `an unmet rep threshold blocks progression`() {
        assertTrue(engine.advanceCandidates(pushUps, "var-pushup-standard", strong(reps = 10)).isEmpty())
    }

    @Test
    fun `too high an RPE blocks progression`() {
        assertTrue(engine.advanceCandidates(pushUps, "var-pushup-standard", strong(rpe = 9.5)).isEmpty())
    }

    @Test
    fun `too low an RIR blocks progression`() {
        val graph = ExerciseVariationGraph("ex-x", listOf(variation("a"), variation("b")), listOf(edge("a", "b", minRir = 2)))
        assertTrue(engine.advanceCandidates(graph, "a", strong(rir = 0)).isEmpty())
        assertTrue("a met RIR passes", engine.advanceCandidates(graph, "a", strong(rir = 3)).isNotEmpty())
    }

    @Test
    fun `an unmet assistance threshold blocks progression`() {
        val graph = ExerciseVariationGraph("ex-x", listOf(variation("a"), variation("b")), listOf(edge("a", "b", maxAssist = 10.0)))
        assertTrue(engine.advanceCandidates(graph, "a", strong(assistance = 25.0)).isEmpty())
        assertTrue(engine.advanceCandidates(graph, "a", strong(assistance = 5.0)).isNotEmpty())
    }

    @Test
    fun `a safety flag blocks progression regardless of performance`() {
        assertTrue(engine.advanceCandidates(pushUps, "var-pushup-standard", strong(safety = ProgressionSafetyState.BLOCKED)).isEmpty())
    }

    @Test
    fun `a class unlock alone never creates readiness`() {
        // A (custom) class-gated edge: requires the 'monk' unlock AND reps>=8, sets>=3.
        val gated =
            edge("a", "b", enabled = true).copy(
                minimumCompletedReps = 8,
                minimumCompletedSets = 3,
                classUnlockRequirement = "monk",
            )
        val graph = ExerciseVariationGraph("ex-x", listOf(variation("a"), variation("b")), listOf(gated))

        // Unlock satisfied but performance not met -> still ineligible.
        assertTrue(engine.advanceCandidates(graph, "a", strong(reps = 2, sets = 1, unlocks = setOf("monk"))).isEmpty())
        // Performance met but the unlock missing -> ineligible (the class gate holds).
        assertTrue(engine.advanceCandidates(graph, "a", strong(reps = 8, sets = 3, unlocks = emptySet())).isEmpty())
        // Both together -> eligible.
        val both = strong(reps = 8, sets = 3, unlocks = setOf("monk"))
        assertTrue(engine.advanceCandidates(graph, "a", both).isNotEmpty())
    }

    @Test
    fun `every seeded push-up and pull-up edge is unrestricted by class`() {
        val baseEdges =
            ExerciseVariationCatalog.graphFor("ex-pushup").edges +
                ExerciseVariationCatalog.graphFor("ex-pullup").edges
        assertTrue("base graphs must have edges", baseEdges.isNotEmpty())
        assertTrue(
            "no base push-up/pull-up edge may carry a class unlock requirement",
            baseEdges.all { it.classUnlockRequirement == null },
        )
    }

    @Test
    fun `a classless user can still reach the top of the push-up graph on performance alone`() {
        // The final push-up edge (assisted one-arm -> one-arm) must be reachable with no class.
        val ready = strong(reps = 8, sets = 3, unlocks = emptySet())
        val advances = engine.advanceCandidates(pushUps, "var-pushup-assisted-onearm", ready)
        assertTrue(advances.any { it.proposedVariationId == "var-pushup-onearm" })
    }

    // ---- pull-up graph paths ----

    @Test
    fun `pull-up graph advances machine to band to bodyweight to weighted`() {
        assertTrue(destinations("var-pullup-machine").contains("var-pullup-heavyband"))
        assertTrue(destinations("var-pullup-heavyband").contains("var-pullup-lightband"))
        assertTrue(destinations("var-pullup-lightband", reps = 6).contains("var-pullup-bodyweight"))
        assertTrue(destinations("var-pullup-bodyweight").contains("var-pullup-weighted"))
    }

    @Test
    fun `a pull-up regression is available when performance declines`() {
        assertNotNull(engine.regressionCandidate(pullUps, "var-pullup-bodyweight", strong(failures = 2)))
    }

    private fun destinations(
        from: String,
        reps: Int = 8,
    ) = engine.advanceCandidates(pullUps, from, strong(reps = reps)).map { it.proposedVariationId }

    private fun variation(id: String) =
        ExerciseVariation(id, "ex-x", id, "", 1, emptyList(), com.ascend.core.model.AssistanceType.NONE, null, false, 5, "", true)

    private fun edge(
        from: String,
        to: String,
        enabled: Boolean = true,
        minRir: Int? = null,
        maxAssist: Double? = null,
    ) = ExerciseVariationProgressionEdge(
        id = "$from-$to", sourceVariationId = from, destinationVariationId = to,
        progressionType = VariationProgressionType.ADVANCE, minimumSuccessfulExposures = 1,
        minimumCompletedReps = 1, minimumCompletedSets = 1, minimumRir = minRir, maximumAssistanceValue = maxAssist,
        requiredTempoControl = false, enabled = enabled,
    )
}
