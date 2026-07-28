package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestCheckpoint
import com.ascend.core.model.QuestInterval
import com.ascend.core.model.QuestIntervalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointAndStepTest {
    private val evaluator = QuestCheckpointEvaluator()
    private val stepResolver = StepIntervalProgressResolver()

    private fun checkpoint(
        id: String,
        target: Double,
        dueAt: Long,
    ) = QuestCheckpoint(id, "q1", id, target, dueAt, true, QuestIntervalStatus.PENDING, null)

    private fun interval(
        id: String,
        start: Long,
        end: Long,
    ) = QuestInterval(id, "q1", id, start, end, 0.0, 0.0, false, QuestIntervalStatus.PENDING, 0, true, null)

    @Test
    fun `cumulative checkpoints report met, pending, and overdue`() {
        val checkpoints = listOf(checkpoint("c1", 50.0, 10), checkpoint("c2", 100.0, 20), checkpoint("c3", 150.0, 30))
        val result = evaluator.evaluate(checkpoints, dailyProgress = 100.0, now = 35)

        assertTrue(result[0].met) // 100 >= 50
        assertTrue(result[1].met) // 100 >= 100
        assertFalse(result[2].met) // 100 < 150
        assertTrue("c3 is past due and unmet", result[2].overdue)
        assertEquals(50.0, result[2].remaining, 0.0001)
    }

    @Test
    fun `cumulative validity requires strictly increasing targets`() {
        assertTrue(evaluator.isCumulativeValid(listOf(checkpoint("c1", 50.0, 1), checkpoint("c2", 100.0, 2))))
        assertFalse(evaluator.isCumulativeValid(listOf(checkpoint("c1", 50.0, 1), checkpoint("c2", 50.0, 2))))
    }

    @Test
    fun `step records resolve into the interval whose window contains their timestamp`() {
        val intervals = listOf(interval("morning", 0, 10), interval("noon", 10, 20), interval("evening", 20, 30))
        val records =
            listOf(
                StepRecord("r1", timestamp = 5, steps = 1000),
                StepRecord("r2", timestamp = 15, steps = 2000),
                StepRecord("r3", timestamp = 25, steps = 1500),
            )
        val resolved = stepResolver.resolve(records, intervals)
        assertEquals(1000, resolved["morning"])
        assertEquals(2000, resolved["noon"])
        assertEquals(1500, resolved["evening"])
        assertEquals(4500, stepResolver.dailyTotal(records))
    }

    @Test
    fun `duplicate imported step records are counted once`() {
        val intervals = listOf(interval("noon", 10, 20))
        // Both records share the same external id (a re-import).
        val records =
            listOf(
                StepRecord("rec-1", timestamp = 15, steps = 2000),
                StepRecord("rec-1", timestamp = 16, steps = 2000),
            )
        assertEquals(2000, stepResolver.resolve(records, intervals)["noon"])
        assertEquals(2000, stepResolver.dailyTotal(records))
    }

    @Test
    fun `interval assignment is epoch-based, so timezone and DST changes do not move boundaries`() {
        // A record's absolute timestamp assigns it to the same interval regardless of
        // wall-clock interpretation; a DST 25-hour day never shifts an epoch boundary.
        val intervals = listOf(interval("a", 1_000, 2_000), interval("b", 2_000, 3_000))
        val record = StepRecord("r", timestamp = 2_500, steps = 500)
        assertEquals(mapOf("b" to 500), stepResolver.resolve(listOf(record), intervals))
    }
}
