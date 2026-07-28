package com.ascend.core.domain.training

import com.ascend.core.model.CardioMode
import com.ascend.core.model.CardioPerformanceSummary
import com.ascend.core.model.CardioPrescription
import com.ascend.core.model.ProgressionDimension
import com.ascend.core.model.ProgressionRecommendationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardioProgressionTest {
    private val cardio = CardioProgressionCalculator()
    private val intervals = IntervalProgressionCalculator()

    private val steady =
        CardioPrescription(
            id = "c1",
            userId = "u1",
            exerciseId = "ex-run",
            mode = CardioMode.STEADY_STATE,
            targetDurationSeconds = 1800,
            incline = 1.0,
            resistance = 2.0,
        )
    private val run =
        CardioPrescription(
            id = "c2",
            userId = "u1",
            exerciseId = "ex-run",
            mode = CardioMode.DISTANCE,
            targetDistanceMeters = 3200.0,
            targetPaceSecondsPerKm = 330.0,
        )
    private val pacing =
        CardioPrescription(
            id = "c3",
            userId = "u1",
            exerciseId = "ex-run",
            mode = CardioMode.PACE_WORK,
            targetPaceSecondsPerKm = 330.0,
            targetDistanceMeters = 3200.0,
        )
    private val interval =
        CardioPrescription(
            id = "c4",
            userId = "u1",
            exerciseId = "ex-row",
            mode = CardioMode.INTERVAL,
            intervalCount = 6,
            workIntervalSeconds = 60,
            restIntervalSeconds = 90,
            targetDurationSeconds = 900,
        )

    private fun summary(
        effort: Int?,
        completed: Boolean = true,
        hr: Int? = null,
    ) = CardioPerformanceSummary(
        exerciseId = "ex-run",
        actualDurationSeconds = 1800,
        perceivedEffort = effort,
        averageHeartRate = hr,
        completed = completed,
    )

    @Test
    fun `steady-state duration progresses after consistent comfortable sessions`() {
        val c = cardio.recommend(steady, listOf(summary(6), summary(6)))
        assertEquals(ProgressionRecommendationType.INCREASE_DURATION, c.recommendationType)
    }

    @Test
    fun `a recommendation is made even without heart-rate data`() {
        val c = cardio.recommend(steady, listOf(summary(6, hr = null), summary(5, hr = null)))
        assertEquals(ProgressionRecommendationType.INCREASE_DURATION, c.recommendationType)
    }

    @Test
    fun `distance progresses for a run`() {
        assertEquals(
            ProgressionRecommendationType.INCREASE_DISTANCE,
            cardio.recommend(run, listOf(summary(6), summary(6))).recommendationType,
        )
    }

    @Test
    fun `pace work improves the pace target`() {
        assertEquals(
            ProgressionRecommendationType.INCREASE_PACE,
            cardio.recommend(pacing, listOf(summary(6), summary(6))).recommendationType,
        )
    }

    @Test
    fun `resistance and incline are offered as alternatives on a machine`() {
        val dims = cardio.candidates(steady, listOf(summary(6), summary(6))).map { it.dimension }
        assertTrue(dims.contains(ProgressionDimension.CARDIO_RESISTANCE))
        assertTrue(dims.contains(ProgressionDimension.CARDIO_INCLINE))
    }

    @Test
    fun `a borderline history maintains the session`() {
        assertEquals(ProgressionRecommendationType.MAINTAIN_PRESCRIPTION, cardio.recommend(steady, listOf(summary(6))).recommendationType)
    }

    @Test
    fun `repeated struggle triggers a deload`() {
        assertEquals(
            ProgressionRecommendationType.DELOAD,
            cardio.recommend(steady, listOf(summary(9), summary(10, completed = false))).recommendationType,
        )
    }

    @Test
    fun `duration and intensity are never escalated in one recommendation`() {
        val candidates = cardio.candidates(steady, listOf(summary(6), summary(6)))
        // Each candidate touches exactly one cardio dimension; the primary is a single variable.
        assertEquals(ProgressionDimension.CARDIO_DURATION, candidates.first().dimension)
        assertEquals(candidates.size, candidates.map { it.dimension }.distinct().size)
    }

    // ---- intervals ----

    @Test
    fun `an interval is added after consistent rounds`() {
        assertEquals(
            ProgressionRecommendationType.ADD_INTERVAL,
            intervals.recommend(interval, listOf(summary(6), summary(6))).recommendationType,
        )
    }

    @Test
    fun `rest trimming is a separate option from adding an interval`() {
        val types = intervals.candidates(interval, listOf(summary(6), summary(6))).map { it.recommendationType }
        assertTrue(types.contains(ProgressionRecommendationType.ADD_INTERVAL))
        assertTrue(types.contains(ProgressionRecommendationType.CHANGE_WORK_REST_RATIO))
    }

    @Test
    fun `interval count is reduced after repeated struggle`() {
        assertEquals(
            ProgressionRecommendationType.REDUCE_INTERVAL,
            intervals.recommend(interval, listOf(summary(9), summary(10, completed = false))).recommendationType,
        )
    }
}
