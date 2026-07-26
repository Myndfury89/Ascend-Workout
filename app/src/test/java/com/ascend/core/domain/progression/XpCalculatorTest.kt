package com.ascend.core.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Test

class XpCalculatorTest {

    private val calc = XpCalculator()

    @Test
    fun `minimal workout awards base plus minimum intensity`() {
        // base 100 + duration 0 + intensityMin 10 = 110
        assertEquals(110L, calc.workoutXp(durationMinutes = 0))
    }

    @Test
    fun `duration contribution is capped at 90 minutes`() {
        // base 100 + capped 90 + intensityMin 10 = 200
        assertEquals(200L, calc.workoutXp(durationMinutes = 120))
    }

    @Test
    fun `full workout stacks all sources`() {
        // 100 + 30 + 50 + 100 + 25 + 50 = 355
        val xp = calc.workoutXp(
            durationMinutes = 30,
            intensity = 1f,
            volumeScore = 1f,
            isPersonalRecord = true,
            consistencyEligible = true,
        )
        assertEquals(355L, xp)
    }

    @Test
    fun `expedition multiplier scales total`() {
        val single = calc.workoutXp(durationMinutes = 30, intensity = 1f)
        val doubled = calc.workoutXp(durationMinutes = 30, intensity = 1f, expeditionMultiplier = 2.0)
        assertEquals(single * 2, doubled)
    }

    @Test
    fun `completed quest awards full base with no over-completion`() {
        val result = calc.questXp(baseReward = 350, completionFraction = 1.0)
        assertEquals(350L, result.base)
        assertEquals(0L, result.overCompletionBonus)
        assertEquals(350L, result.total)
    }

    @Test
    fun `partial quest is proportional when enabled and zero when disabled`() {
        assertEquals(175L, calc.questXp(350, completionFraction = 0.5, partialEnabled = true).total)
        assertEquals(0L, calc.questXp(350, completionFraction = 0.5, partialEnabled = false).total)
    }

    @Test
    fun `over-completion bonus is small and capped`() {
        // countedOver = min(1.0, 0.5) = 0.5 -> bonus = round(350 * 0.5 * 0.2) = 35
        val doubled = calc.questXp(350, completionFraction = 1.0, overCompletionFraction = 1.0)
        assertEquals(35L, doubled.overCompletionBonus)
        assertEquals(385L, doubled.total)

        // Extreme over-completion cannot exceed the cap (still 35).
        val extreme = calc.questXp(350, completionFraction = 1.0, overCompletionFraction = 10.0)
        assertEquals(35L, extreme.overCompletionBonus)
    }
}
