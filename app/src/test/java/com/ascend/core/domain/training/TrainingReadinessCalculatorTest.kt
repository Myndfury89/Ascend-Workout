package com.ascend.core.domain.training

import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.ProgressionStrategy
import com.ascend.core.model.ReadinessCheckIn
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.SetPerformance
import com.ascend.core.model.TrainingReadinessState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingReadinessCalculatorTest {
    private val calc = TrainingReadinessCalculator()

    private val bench =
        ExercisePrescription(
            id = "p1", userId = "u1", exerciseId = "ex-bench", progressionStrategy = ProgressionStrategy.DOUBLE_PROGRESSION,
            targetSets = 3, minimumReps = 8, maximumReps = 10, targetWeight = 135.0, targetRestSeconds = 120,
        )

    private fun session(
        reps: List<Int>,
        rpe: Double? = null,
        effort: Int? = null,
        failedIndex: Int? = null,
        t: Long = 0,
    ) = SessionPerformance(
        exerciseId = "ex-bench",
        prescriptionId = "p1",
        sets = reps.mapIndexed { i, r -> SetPerformance(reps = r, weight = 135.0, failed = i == failedIndex, rpe = rpe) },
        perceivedEffort = effort,
        completedAt = t,
    )

    private fun history(vararg reps: List<Int>) = reps.mapIndexed { i, r -> session(r, t = i.toLong()) }

    @Test
    fun `no sessions is insufficient data`() {
        assertEquals(TrainingReadinessState.INSUFFICIENT_DATA, calc.evaluateResistance(emptyList(), bench).state)
    }

    @Test
    fun `all sets at top of range with enough exposure is ready for load`() {
        val sessions = history(listOf(8, 8, 8), listOf(9, 8, 8), listOf(10, 9, 8), listOf(10, 10, 10))
        assertEquals(TrainingReadinessState.READY_FOR_LOAD_PROGRESSION, calc.evaluateResistance(sessions, bench).state)
    }

    @Test
    fun `below top of range but stable is ready for rep progression`() {
        val sessions = history(listOf(8, 8, 8), listOf(10, 9, 8))
        assertEquals(TrainingReadinessState.READY_FOR_REP_PROGRESSION, calc.evaluateResistance(sessions, bench).state)
    }

    @Test
    fun `one strong set does not trigger load progression`() {
        val sessions = history(listOf(12, 8, 8))
        assertEquals(TrainingReadinessState.READY_FOR_REP_PROGRESSION, calc.evaluateResistance(sessions, bench).state)
    }

    @Test
    fun `a failed rep blocks load progression`() {
        val sessions = listOf(session(listOf(10, 10, 10), failedIndex = 2, t = 1))
        assertEquals(TrainingReadinessState.MAINTAIN, calc.evaluateResistance(sessions, bench).state)
    }

    @Test
    fun `high RPE at the top range blocks load progression`() {
        val sessions = history(listOf(8, 8, 8), listOf(9, 9, 9)) + session(listOf(10, 10, 10), rpe = 10.0, t = 2)
        val readiness = calc.evaluateResistance(sessions, bench)
        assertEquals(TrainingReadinessState.MAINTAIN, readiness.state)
        assertTrue(readiness.limitingSignals.any { it.contains("Effort") })
    }

    @Test
    fun `low data requires an extra exposure before load progression`() {
        // Only one session at the top range -> needs a second exposure for a new user.
        val sessions = listOf(session(listOf(10, 10, 10), t = 0))
        assertEquals(TrainingReadinessState.MAINTAIN, calc.evaluateResistance(sessions, bench).state)
    }

    @Test
    fun `reported pain blocks progression regardless of performance`() {
        val sessions = history(listOf(10, 10, 10), listOf(10, 10, 10))
        val readiness = calc.evaluateResistance(sessions, bench, ReadinessCheckIn(painReported = true))
        assertEquals(ProgressionSafetyState.BLOCKED, readiness.safetyState)
        assertEquals(TrainingReadinessState.REGRESS, readiness.state)
    }

    @Test
    fun `a serious symptom recommends stopping and seeking guidance`() {
        val readiness =
            calc.evaluateResistance(history(listOf(10, 10, 10)), bench, ReadinessCheckIn(seriousSymptomReported = true))
        assertEquals(ProgressionSafetyState.STOP_AND_SEEK_GUIDANCE, readiness.safetyState)
    }
}
