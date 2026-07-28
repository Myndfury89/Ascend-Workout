package com.ascend.core.domain.quest.interval

import com.ascend.core.model.IntervalDistributionStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalGenerationTest {
    private val distribution = QuestIntervalDistributionCalculator()
    private val generator = QuestIntervalGenerator(distribution)
    private val validator = IntervalScheduleValidator()

    @Test
    fun `equal distribution splits evenly and folds the remainder`() {
        assertEquals(listOf(50, 50, 50, 50), distribution.distribute(200, 4, IntervalDistributionStrategy.EQUAL))
        val uneven = distribution.distribute(200, 3, IntervalDistributionStrategy.EQUAL)
        assertEquals(listOf(67, 67, 66), uneven)
        assertEquals(200, uneven.sum())
    }

    @Test
    fun `preferred-set-size distribution uses whole sets with a folded remainder`() {
        assertEquals(List(8) { 25 }, distribution.distribute(200, 0, IntervalDistributionStrategy.PREFERRED_SET_SIZE, 25))
        assertEquals(listOf(50, 50, 25), distribution.distribute(125, 0, IntervalDistributionStrategy.PREFERRED_SET_SIZE, 50))
    }

    @Test
    fun `front and back loaded distributions weight the ends but still sum to total`() {
        val front = distribution.distribute(200, 4, IntervalDistributionStrategy.FRONT_LOADED)
        assertEquals(200, front.sum())
        assertTrue("front-loaded descends", front.first() > front.last())

        val back = distribution.distribute(200, 4, IntervalDistributionStrategy.BACK_LOADED)
        assertEquals(200, back.sum())
        assertTrue("back-loaded ascends", back.last() > back.first())
    }

    @Test
    fun `suggested schedule tiles the window into non-overlapping intervals summing to the target`() {
        val window = 12L * 60 * 60 * 1000 // 12 hours
        val intervals =
            generator.generate(
                IntervalGenerationRequest(
                    total = 200,
                    strategy = IntervalDistributionStrategy.PREFERRED_SET_SIZE,
                    windowStart = 0,
                    windowEnd = window,
                    preferredSetSize = 25,
                ),
            )
        assertEquals(8, intervals.size)
        assertEquals(200, intervals.sumOf { it.target })
        // Contiguous, ascending, within the window.
        assertEquals(0L, intervals.first().scheduledStart)
        assertEquals(window, intervals.last().scheduledEnd)
        intervals.zipWithNext().forEach { (a, b) -> assertEquals(a.scheduledEnd, b.scheduledStart) }

        val validation = validator.validate(intervals, dailyTarget = 200)
        assertTrue(validation.isValid)
        assertEquals(0, validation.unassigned)
    }

    @Test
    fun `fixed intervals honor explicit custom times`() {
        val times = listOf(0L to 100L, 100L to 200L, 200L to 300L, 300L to 400L)
        val intervals =
            generator.generate(
                IntervalGenerationRequest(
                    total = 200,
                    strategy = IntervalDistributionStrategy.EQUAL,
                    windowStart = 0,
                    windowEnd = 400,
                    customTimes = times,
                ),
            )
        assertEquals(times.map { it.first }, intervals.map { it.scheduledStart })
        assertEquals(200, intervals.sumOf { it.target })
    }

    @Test
    fun `validator flags overlaps, bad windows, and reports unassigned amount`() {
        val overlapping =
            listOf(
                GeneratedInterval(0, 0, 150, 50),
                GeneratedInterval(1, 100, 200, 50),
            )
        assertTrue(validator.validate(overlapping, 100).issues.contains(IntervalIssue.OVERLAP))

        val badWindow = listOf(GeneratedInterval(0, 200, 100, 50))
        assertTrue(validator.validate(badWindow, 50).issues.contains(IntervalIssue.END_BEFORE_START))

        // Under-assigned: intervals cover only 120 of a 200 daily target.
        val partial = listOf(GeneratedInterval(0, 0, 100, 60), GeneratedInterval(1, 100, 200, 60))
        val result = validator.validate(partial, 200)
        assertFalse(result.issues.contains(IntervalIssue.OVERLAP))
        assertEquals(80, result.unassigned)
    }
}
