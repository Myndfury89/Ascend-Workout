package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestInterval
import com.ascend.core.model.QuestIntervalStatus
import com.ascend.core.model.RedistributionPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalProgressTest {
    private val progress = QuestIntervalProgressCalculator()
    private val redistribution = QuestIntervalRedistributionEngine()
    private val reminders = QuestReminderPolicy()

    private fun interval(
        id: String,
        start: Long,
        end: Long,
        target: Double,
        current: Double,
        status: QuestIntervalStatus = QuestIntervalStatus.PENDING,
    ) = QuestInterval(id, "q1", id, start, end, target, current, false, status, 0, true, null)

    @Test
    fun `interval status reflects progress and the scheduled window`() {
        assertEquals(QuestIntervalStatus.PENDING, progress.statusFor(0.0, 50.0, now = 5, scheduledEnd = 10))
        assertEquals(QuestIntervalStatus.IN_PROGRESS, progress.statusFor(30.0, 50.0, now = 5, scheduledEnd = 10))
        assertEquals(QuestIntervalStatus.COMPLETED, progress.statusFor(50.0, 50.0, now = 5, scheduledEnd = 10))
        assertEquals(QuestIntervalStatus.PARTIAL, progress.statusFor(30.0, 50.0, now = 20, scheduledEnd = 10))
        assertEquals(QuestIntervalStatus.MISSED, progress.statusFor(0.0, 50.0, now = 20, scheduledEnd = 10))
    }

    @Test
    fun `daily progress sums intervals and can be met before all intervals expire`() {
        val intervals =
            listOf(
                interval("morning", 0, 10, 50.0, 50.0),
                interval("noon", 10, 20, 50.0, 50.0),
                interval("evening", 20, 30, 50.0, 50.0),
                interval("night", 30, 40, 50.0, 50.0),
            )
        assertEquals(200.0, progress.dailyProgress(intervals), 0.0001)
        assertEquals(0.0, progress.dailyRemaining(200.0, intervals), 0.0001)
        assertTrue("daily target met even though the last window hasn't passed", progress.dailyMet(200.0, intervals))
    }

    @Test
    fun `even redistribution spreads the leftover across future intervals`() {
        val result = redistribution.redistribute(20, listOf(50, 50), RedistributionPreference.EVEN)
        assertEquals(listOf(60, 60), result.newTargets)
        assertEquals(0, result.unassigned)
    }

    @Test
    fun `heavier-final and lighter-next weight later intervals`() {
        assertEquals(
            listOf(50, 50, 70),
            redistribution.redistribute(20, listOf(50, 50, 50), RedistributionPreference.HEAVIER_FINAL).newTargets,
        )
        val lighter = redistribution.redistribute(30, listOf(50, 50), RedistributionPreference.LIGHTER_NEXT)
        assertTrue("the later interval takes more", lighter.newTargets.last() > lighter.newTargets.first())
    }

    @Test
    fun `preserve keeps the plan and leaves the remainder unassigned`() {
        val result = redistribution.redistribute(20, listOf(50, 50), RedistributionPreference.PRESERVE_ORIGINAL)
        assertEquals(listOf(50, 50), result.newTargets)
        assertEquals(20, result.unassigned)
    }

    @Test
    fun `redistribution respects the max set size, returning the overflow`() {
        val result = redistribution.redistribute(40, listOf(50), RedistributionPreference.EVEN, maxSetSize = 60)
        assertEquals(listOf(60), result.newTargets)
        assertEquals(30, result.unassigned) // 50 + 40 = 90, capped at 60
    }

    @Test
    fun `reminders are suppressed when done, skipped, disabled, or in quiet hours`() {
        val active = interval("noon", 0, 100, 50.0, 20.0, QuestIntervalStatus.IN_PROGRESS)
        assertTrue(reminders.shouldRemind(active, now = 10, questComplete = false, notificationsEnabled = true))

        assertFalse(reminders.shouldRemind(active, now = 10, questComplete = true, notificationsEnabled = true))
        assertFalse(reminders.shouldRemind(active, now = 10, questComplete = false, notificationsEnabled = false))
        val done = interval("noon", 0, 100, 50.0, 50.0, QuestIntervalStatus.COMPLETED)
        assertFalse(reminders.shouldRemind(done, now = 10, questComplete = false, notificationsEnabled = true))
        assertFalse(
            reminders.shouldRemind(active, now = 50, questComplete = false, notificationsEnabled = true, quietHours = listOf(40L to 60L)),
        )
    }
}
