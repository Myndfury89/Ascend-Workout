package com.ascend.core.domain.training

import com.ascend.core.model.QuestOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentBaselineCalculatorTest {
    private val calc = RecentBaselineCalculator()

    private fun outcome(
        target: Int,
        completed: Int,
        createdAt: Long,
    ) = QuestOutcome("q$createdAt", target, completed, "COMPLETED", createdAt)

    @Test
    fun `empty history has no baseline`() {
        assertNull(calc.calculate(emptyList()))
    }

    @Test
    fun `baseline uses the median target so a single outlier does not dominate`() {
        // Newest first: three 100s and one 300 outlier.
        val outcomes =
            listOf(
                outcome(100, 100, 4),
                outcome(100, 100, 3),
                outcome(100, 100, 2),
                outcome(300, 300, 1),
            )
        val baseline = calc.calculate(outcomes)!!
        assertEquals(100, baseline.representativeTarget)
        assertEquals(1.0, baseline.completionConsistency, 1e-9)
        assertEquals(4, baseline.sampleCount)
    }

    @Test
    fun `consistency reflects fully-completed fraction`() {
        val outcomes =
            listOf(
                outcome(100, 100, 3),
                outcome(100, 60, 2),
                outcome(100, 100, 1),
            )
        val baseline = calc.calculate(outcomes)!!
        assertEquals(2.0 / 3.0, baseline.completionConsistency, 1e-9)
        assertTrue(baseline.averageCompletedAmount < 100)
    }
}
