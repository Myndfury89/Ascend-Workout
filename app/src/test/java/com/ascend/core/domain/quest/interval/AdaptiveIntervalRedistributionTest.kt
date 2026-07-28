package com.ascend.core.domain.quest.interval

import com.ascend.core.model.AdaptiveIntervalAction
import com.ascend.core.model.AdaptiveIntervalContext
import com.ascend.core.model.FutureInterval
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.RedistributionPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveIntervalRedistributionTest {
    private val engine = QuestIntervalRedistributionEngine()

    // 200/day in four 50s; morning done 30/50 -> 20 leftover, three 50s ahead.
    private fun context(
        dailyTarget: Int = 200,
        progress: Int = 30,
        leftover: Int = 20,
        futures: List<FutureInterval> = listOf(future("a", 50, 10), future("b", 50, 20), future("c", 50, 30)),
        preference: RedistributionPreference = RedistributionPreference.EVEN,
        timeRemaining: Long = 1000,
        quietHours: List<Pair<Long, Long>> = emptyList(),
        maxSetSize: Int = Int.MAX_VALUE,
        maxIntervalTarget: Int = Int.MAX_VALUE,
        fatigue: Int? = null,
        auto: Boolean = false,
        safety: ProgressionSafetyState = ProgressionSafetyState.OK,
        now: Long = 5,
    ) = AdaptiveIntervalContext(
        dailyTarget = dailyTarget, currentDailyProgress = progress, leftover = leftover, futureIntervals = futures,
        timeRemainingMillis = timeRemaining, quietHours = quietHours, maximumSetSize = maxSetSize,
        maximumIntervalTarget = maxIntervalTarget, fatigue = fatigue, redistributionPreference = preference,
        autoSafeAdaptationEnabled = auto, safetyState = safety, now = now,
    )

    private fun future(
        id: String,
        target: Int,
        start: Long,
    ) = FutureInterval(id, target, start)

    @Test
    fun `even redistribution spreads the leftover across future intervals`() {
        val rec = engine.adaptiveRecommend(context(preference = RedistributionPreference.EVEN))
        assertEquals(AdaptiveIntervalAction.REDISTRIBUTE_EVENLY, rec.action)
        assertEquals(20, rec.proposedTargets.values.sum() - 3 * 50)
    }

    @Test
    fun `lighter next weights the leftover toward later intervals`() {
        val rec = engine.adaptiveRecommend(context(preference = RedistributionPreference.LIGHTER_NEXT))
        assertEquals(AdaptiveIntervalAction.LIGHTER_NEXT_INTERVAL, rec.action)
    }

    @Test
    fun `heavier final loads the leftover onto the last interval`() {
        val rec = engine.adaptiveRecommend(context(preference = RedistributionPreference.HEAVIER_FINAL))
        assertEquals(AdaptiveIntervalAction.HEAVIER_FINAL_INTERVAL, rec.action)
        assertEquals(70, rec.proposedTargets["c"])
    }

    @Test
    fun `preserve original leaves the remainder flexible`() {
        val rec = engine.adaptiveRecommend(context(preference = RedistributionPreference.PRESERVE_ORIGINAL))
        assertEquals(AdaptiveIntervalAction.PRESERVE_FUTURE_LEAVE_FLEXIBLE, rec.action)
        assertEquals(20, rec.leftoverFlexible)
    }

    @Test
    fun `no future interval adds a new one when time remains`() {
        val rec = engine.adaptiveRecommend(context(futures = emptyList(), timeRemaining = 1000))
        assertEquals(AdaptiveIntervalAction.ADD_NEW_INTERVAL, rec.action)
        assertEquals(20, rec.newIntervalTarget)
    }

    @Test
    fun `future intervals inside quiet hours convert to flexible`() {
        val quiet = listOf(0L to 100L)
        val rec = engine.adaptiveRecommend(context(quietHours = quiet))
        assertEquals(AdaptiveIntervalAction.CONVERT_TO_FLEXIBLE, rec.action)
        assertEquals(20, rec.leftoverFlexible)
    }

    @Test
    fun `being ahead of plan reduces the upcoming interval sizes`() {
        val rec =
            engine.adaptiveRecommend(
                context(dailyTarget = 100, progress = 80, leftover = 0, futures = listOf(future("a", 30, 10), future("b", 30, 20))),
            )
        assertEquals(AdaptiveIntervalAction.REDUCE_INTERVAL_SIZES, rec.action)
        assertTrue(rec.proposedTargets.values.sum() <= 20)
    }

    @Test
    fun `out of time with nothing scheduled reduces today's total`() {
        val rec = engine.adaptiveRecommend(context(futures = emptyList(), timeRemaining = 0))
        assertEquals(AdaptiveIntervalAction.REDUCE_DAILY_TOTAL, rec.action)
        assertEquals(30, rec.proposedDailyTotal)
    }

    @Test
    fun `a quiet-hours interval is skipped and the others take the work`() {
        // 'a' starts inside quiet hours; work should go to b and c only.
        val rec = engine.adaptiveRecommend(context(quietHours = listOf(5L to 15L)))
        assertFalse(rec.proposedTargets.containsKey("a"))
        assertTrue(rec.constraintNotes.any { it.contains("quiet hours") })
    }

    @Test
    fun `the max set size caps how much one interval grows`() {
        val rec = engine.adaptiveRecommend(context(maxSetSize = 5))
        assertTrue(rec.proposedTargets.values.all { it <= 55 })
        assertTrue(rec.leftoverFlexible > 0)
        assertTrue(rec.constraintNotes.any { it.contains("set size") })
    }

    @Test
    fun `the max interval target caps an interval's total`() {
        val rec = engine.adaptiveRecommend(context(maxIntervalTarget = 55))
        assertTrue(rec.proposedTargets.values.all { it <= 55 })
        assertTrue(rec.constraintNotes.any { it.contains("interval target") })
    }

    @Test
    fun `high fatigue blocks aggressive redistribution`() {
        val rec = engine.adaptiveRecommend(context(fatigue = 9, auto = true))
        assertEquals(AdaptiveIntervalAction.PRESERVE_FUTURE_LEAVE_FLEXIBLE, rec.action)
        assertFalse(rec.autoApplied)
    }

    @Test
    fun `automatic adaptation requires opt-in`() {
        assertFalse(engine.adaptiveRecommend(context(auto = false)).autoApplied)
        val auto = engine.adaptiveRecommend(context(auto = true))
        assertTrue(auto.autoApplied)
        assertFalse(auto.requiresConfirmation)
    }

    @Test
    fun `automatic adaptation never increases the daily total`() {
        val rec = engine.adaptiveRecommend(context(auto = true))
        // Redistribution only moves owed work — it never raises the daily objective.
        assertNull(rec.proposedDailyTotal)
        assertEquals(20, rec.proposedTargets.values.sum() - 3 * 50)
    }

    @Test
    fun `completing the daily total overrides a missed interval`() {
        val rec = engine.adaptiveRecommend(context(progress = 200, leftover = 20))
        assertEquals(AdaptiveIntervalAction.MAINTAIN_PLAN, rec.action)
        assertFalse(rec.requiresConfirmation)
    }

    @Test
    fun `a safety flag defers to the user`() {
        val rec = engine.adaptiveRecommend(context(safety = ProgressionSafetyState.BLOCKED))
        assertEquals(AdaptiveIntervalAction.REQUEST_USER_CHOICE, rec.action)
        assertFalse(rec.autoApplied)
    }
}
