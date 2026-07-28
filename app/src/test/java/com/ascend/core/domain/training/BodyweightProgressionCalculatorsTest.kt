package com.ascend.core.domain.training

import com.ascend.core.domain.training.variation.ExerciseVariationCatalog
import com.ascend.core.domain.training.variation.ExerciseVariationProgressionEngine
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionStrategy
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.SetPerformance
import com.ascend.core.model.VariationReadinessInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyweightProgressionCalculatorsTest {
    private val rep = RepProgressionCalculator()
    private val set = SetProgressionCalculator()
    private val rest = RestProgressionCalculator()
    private val assistance = AssistanceProgressionCalculator()
    private val externalLoad = ExternalLoadProgressionCalculator()
    private val tempo = TempoProgressionCalculator()
    private val orchestrator =
        BodyweightProgressionCalculator(rep, set, rest, assistance, externalLoad, tempo, ExerciseVariationProgressionEngine())

    private val pushUp =
        ExercisePrescription(
            id = "p-push", userId = "u1", exerciseId = "ex-pushup", progressionStrategy = ProgressionStrategy.VARIATION_PROGRESSION,
            targetSets = 3, minimumReps = 8, maximumReps = 12, targetRestSeconds = 90, variationId = "var-pushup-standard",
        )
    private val bandPullUp =
        ExercisePrescription(
            id = "p-pull", userId = "u1", exerciseId = "ex-pullup", progressionStrategy = ProgressionStrategy.VARIATION_PROGRESSION,
            targetSets = 3, minimumReps = 5, maximumReps = 8, targetRestSeconds = 120,
            variationId = "var-pullup-heavyband", assistanceValue = 30.0,
        )

    private fun session(
        reps: List<Int>,
        t: Long,
    ) = SessionPerformance("ex-pushup", null, reps.map { SetPerformance(it) }, completedAt = t)

    // ---- push-up rep / set / rest progression ----

    @Test
    fun `owning the top of the range increases the target reps`() {
        val c = rep.evaluate(pushUp, listOf(session(listOf(12, 12, 12), 0), session(listOf(12, 12, 12), 1)))
        assertEquals(ProgressionRecommendationType.INCREASE_REPS, c.recommendationType)
        assertTrue("range should shift up", c.proposed!!.maximumReps!! > 12)
    }

    @Test
    fun `repeated failed reps ease the target`() {
        val c = rep.evaluate(pushUp, listOf(session(listOf(7, 7, 7), 0), session(listOf(6, 6, 6), 1)))
        assertEquals(ProgressionRecommendationType.REDUCE_REPS, c.recommendationType)
    }

    @Test
    fun `a set is added only after several strong sessions`() {
        val strong = listOf(session(listOf(12, 12, 12), 0), session(listOf(12, 12, 12), 1), session(listOf(12, 12, 12), 2))
        val c = set.evaluate(pushUp, strong)
        assertEquals(ProgressionRecommendationType.ADD_SET, c.recommendationType)
        assertEquals(4, c.proposed!!.targetSets)
    }

    @Test
    fun `a set is not added after a single easy session`() {
        val c = set.evaluate(pushUp, listOf(session(listOf(12, 12, 12), 0)))
        assertEquals(ProgressionRecommendationType.REQUEST_MORE_DATA, c.recommendationType)
    }

    @Test
    fun `rest is reduced only when performance is stable`() {
        val c = rest.evaluate(pushUp, listOf(session(listOf(10, 10, 10), 0), session(listOf(10, 10, 10), 1)))
        assertEquals(ProgressionRecommendationType.REDUCE_REST, c.recommendationType)
        assertTrue(c.proposed!!.targetRestSeconds!! < 90)
    }

    @Test
    fun `rest is not reduced when output declines`() {
        val c = rest.evaluate(pushUp, listOf(session(listOf(12, 12, 12), 0), session(listOf(10, 10, 10), 1)))
        assertNotEquals(ProgressionRecommendationType.REDUCE_REST, c.recommendationType)
        assertEquals(ProgressionRecommendationType.INCREASE_REST, c.recommendationType)
    }

    @Test
    fun `heavy strength work keeps its rest rather than trimming it`() {
        val c = rest.evaluate(pushUp, listOf(session(listOf(10, 10, 10), 0), session(listOf(10, 10, 10), 1)), isHeavyStrength = true)
        assertNotEquals(ProgressionRecommendationType.REDUCE_REST, c.recommendationType)
    }

    // ---- assistance / external load ----

    @Test
    fun `assistance is reduced after strong sessions on a band`() {
        val strong = listOf(session(listOf(8, 8, 8), 0), session(listOf(8, 8, 8), 1))
        val c = assistance.evaluate(bandPullUp, strong)
        assertEquals(ProgressionRecommendationType.REDUCE_ASSISTANCE, c.recommendationType)
        assertTrue(c.proposed!!.assistanceValue!! < 30.0)
    }

    @Test
    fun `assistance is added back when performance declines`() {
        val weak = listOf(session(listOf(4, 4, 4), 0), session(listOf(3, 3, 3), 1))
        assertEquals(ProgressionRecommendationType.INCREASE_ASSISTANCE, assistance.evaluate(bandPullUp, weak).recommendationType)
    }

    @Test
    fun `external load is added only after bodyweight mastery`() {
        val mastered = listOf(session(listOf(12, 12, 12), 0), session(listOf(12, 12, 12), 1))
        assertEquals(
            ProgressionRecommendationType.ADD_EXTERNAL_LOAD,
            externalLoad.evaluate(pushUp, mastered, externalLoadSupported = true).recommendationType,
        )
        // Not supported by the variation -> hold.
        assertEquals(
            ProgressionRecommendationType.MAINTAIN_PRESCRIPTION,
            externalLoad.evaluate(pushUp, mastered, externalLoadSupported = false).recommendationType,
        )
    }

    // ---- tempo & range of motion ----

    @Test
    fun `tempo work is offered once the movement is controlled`() {
        val controlled = listOf(session(listOf(10, 10, 10), 0), session(listOf(10, 10, 10), 1))
        val candidates = tempo.candidates(pushUp, controlled, hasVariationControl = true)
        assertEquals(ProgressionRecommendationType.SLOW_ECCENTRIC, candidates.first().recommendationType)
        assertTrue(candidates.any { it.recommendationType == ProgressionRecommendationType.ADD_PAUSE })
        assertTrue(candidates.any { it.recommendationType == ProgressionRecommendationType.INCREASE_RANGE_OF_MOTION })
    }

    @Test
    fun `tempo is blocked without established control`() {
        val controlled = listOf(session(listOf(10, 10, 10), 0), session(listOf(10, 10, 10), 1))
        assertEquals(
            ProgressionRecommendationType.MAINTAIN_PRESCRIPTION,
            tempo.evaluate(pushUp, controlled, hasVariationControl = false).recommendationType,
        )
    }

    // ---- composition: one variable at a time, no stacking ----

    @Test
    fun `candidates never stack several aggressive changes and tempo yields to a variation advance`() {
        val strong = listOf(session(listOf(12, 12, 12), 0), session(listOf(12, 12, 12), 1), session(listOf(12, 12, 12), 2))
        val context =
            BodyweightProgressionContext(
                prescription = pushUp,
                sessions = strong,
                graph = ExerciseVariationCatalog.graphFor("ex-pushup"),
                currentVariationId = "var-pushup-standard",
                variationInput =
                    VariationReadinessInput(
                        successfulExposures = 2,
                        minCompletedReps = 12,
                        completedSets = 3,
                        maxRpe = 7.0,
                        rangeOfMotionLevel = 5,
                        tempoControlled = true,
                    ),
                hasVariationControl = true,
            )
        val safe = orchestrator.safeChangeCandidates(context)

        // Each candidate moves exactly one dimension — never a fused, stacked change.
        assertEquals(safe.size, safe.map { it.dimension }.distinct().size)
        assertTrue("a variation advance should be present", safe.any { it.dimension == ProgressionDimension.VARIATION })
        // Tempo does not stack with a variation advance by default.
        assertFalse(safe.any { it.dimension == ProgressionDimension.TEMPO })
        assertFalse(safe.any { it.dimension == ProgressionDimension.RANGE_OF_MOTION })
    }
}
