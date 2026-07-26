package com.ascend.core.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val calc = StreakCalculator()
    private val day = LocalDate.of(2026, 7, 25)

    @Test
    fun `first ever activity starts a streak of one`() {
        val result = calc.onActivity(currentStreak = 0, longestStreak = 0, lastActiveDay = null, today = day)
        assertEquals(1, result.activeStreak)
        assertEquals(1, result.longestStreak)
        assertTrue(result.incremented)
    }

    @Test
    fun `consecutive day increments and can set a new longest`() {
        val result = calc.onActivity(currentStreak = 4, longestStreak = 4, lastActiveDay = day.minusDays(1), today = day)
        assertEquals(5, result.activeStreak)
        assertEquals(5, result.longestStreak)
        assertTrue(result.incremented)
    }

    @Test
    fun `same day does not change the streak`() {
        val result = calc.onActivity(currentStreak = 3, longestStreak = 7, lastActiveDay = day, today = day)
        assertEquals(3, result.activeStreak)
        assertEquals(7, result.longestStreak)
        assertFalse(result.incremented)
    }

    @Test
    fun `a missed day resets active streak but preserves longest`() {
        val result = calc.onActivity(currentStreak = 9, longestStreak = 12, lastActiveDay = day.minusDays(2), today = day)
        assertEquals(1, result.activeStreak)
        assertEquals(12, result.longestStreak) // history is never lost
        assertTrue(result.reset)
    }
}
