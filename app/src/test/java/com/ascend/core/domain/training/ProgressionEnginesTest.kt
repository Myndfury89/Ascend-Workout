package com.ascend.core.domain.training

import com.ascend.core.domain.usecase.SeedQuestTemplatesUseCase
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionStrategy
import com.ascend.core.model.QuestOutcome
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.SetPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionEnginesTest {
    private val exerciseEngine = ExerciseProgressionEngine(TrainingReadinessCalculator(), LoadProgressionCalculator())
    private val questEngine =
        QuestProgressionEngine(RecentBaselineCalculator(), TrainingReadinessCalculator(), DailyQuestTargetProgressionCalculator())

    private val pushups = SeedQuestTemplatesUseCase.CATALOG.first { it.id == "tmpl-pushups" } // 25..500, step 5, preferred 25
    private val bench =
        ExercisePrescription(
            id = "p1", userId = "u1", exerciseId = "ex-bench", progressionStrategy = ProgressionStrategy.DOUBLE_PROGRESSION,
            targetSets = 3, minimumReps = 8, maximumReps = 10, targetWeight = 135.0, targetRestSeconds = 120,
        )

    private fun session(
        reps: List<Int>,
        t: Long,
    ) = SessionPerformance("ex-bench", "p1", reps.map { SetPerformance(it, 135.0) }, completedAt = t)

    private fun outcome(
        target: Int,
        completed: Int,
        t: Long,
    ) = QuestOutcome("q$t", target, completed, "COMPLETED", t)

    // ---- Slice A: bench double progression ----

    @Test
    fun `bench double progression recommends the smallest load increase once all sets hit the top`() {
        val sessions =
            listOf(
                session(listOf(8, 8, 8), 0),
                session(listOf(9, 8, 8), 1),
                session(listOf(10, 9, 8), 2),
                session(listOf(10, 10, 10), 3),
            )
        val rec = exerciseEngine.recommend("u1", bench, sessions)
        assertEquals(ProgressionRecommendationType.INCREASE_WEIGHT, rec.recommendationType)
        assertEquals(140.0, rec.proposed!!.targetWeight!!, 1e-9) // 135 + smallest (5)
        assertTrue("a change must be confirmed", rec.requiresConfirmation)
    }

    @Test
    fun `below the top of the range recommends rep progression, not load`() {
        val sessions = listOf(session(listOf(8, 8, 8), 0), session(listOf(10, 9, 8), 1))
        val rec = exerciseEngine.recommend("u1", bench, sessions)
        assertEquals(ProgressionRecommendationType.INCREASE_REPS, rec.recommendationType)
        assertNull(rec.proposed)
    }

    // ---- Slice C: daily quest baseline ----

    @Test
    fun `consistent quest completion recommends a capped target increase`() {
        val outcomes = listOf(outcome(100, 100, 3), outcome(100, 100, 2), outcome(100, 100, 1))
        val rec = questEngine.recommend("u1", pushups, outcomes)
        assertEquals(ProgressionRecommendationType.INCREASE_DAILY_QUEST_TARGET, rec.recommendationType)
        assertEquals(125, rec.proposedTarget) // 100 + preferred set size (25)
    }

    @Test
    fun `target increase is capped at the template maximum`() {
        val outcomes = listOf(outcome(490, 490, 3), outcome(490, 490, 2), outcome(490, 490, 1))
        assertEquals(500, questEngine.recommend("u1", pushups, outcomes).proposedTarget) // capped at 500
    }

    @Test
    fun `borderline completion maintains the target`() {
        // 2 of 3 fully complete -> consistency 0.67 (between reduce 0.5 and increase 0.8).
        val outcomes = listOf(outcome(100, 100, 3), outcome(100, 70, 2), outcome(100, 100, 1))
        assertEquals(ProgressionRecommendationType.MAINTAIN_PRESCRIPTION, questEngine.recommend("u1", pushups, outcomes).recommendationType)
    }

    @Test
    fun `repeated partial completion recommends a reduction floored at the minimum`() {
        val outcomes = listOf(outcome(30, 10, 3), outcome(30, 12, 2), outcome(30, 8, 1))
        val rec = questEngine.recommend("u1", pushups, outcomes)
        assertEquals(ProgressionRecommendationType.REDUCE_DAILY_QUEST_TARGET, rec.recommendationType)
        assertEquals(25, rec.proposedTarget) // 30 - 25 = 5, floored to template minimum 25
    }
}
