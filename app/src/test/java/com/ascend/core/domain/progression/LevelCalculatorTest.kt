package com.ascend.core.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.roundToLong

class LevelCalculatorTest {

    private val calc = LevelCalculator()

    @Test
    fun `xp to reach next level matches round(200 x level^1_35)`() {
        // Level 1 -> 2 : round(200 * 1^1.35) = 200
        assertEquals(200L, calc.xpToReachNextLevel(1))
        // Level 2 -> 3 : round(200 * 2^1.35) = 510
        assertEquals(510L, calc.xpToReachNextLevel(2))
        // Level 10 -> 11 : compare to the same formula
        val expected10 = (200.0 * 10.0.pow(1.35)).roundToLong()
        assertEquals(expected10, calc.xpToReachNextLevel(10))
    }

    @Test
    fun `xp curve is strictly increasing`() {
        var prev = 0L
        for (level in 1..50) {
            val needed = calc.xpToReachNextLevel(level)
            assertTrue("level $level should require more than level ${level - 1}", needed > prev)
            prev = needed
        }
    }

    @Test
    fun `cumulative xp for level 1 is zero and level 2 equals first step`() {
        assertEquals(0L, calc.cumulativeXpForLevel(1))
        assertEquals(calc.xpToReachNextLevel(1), calc.cumulativeXpForLevel(2))
        assertEquals(
            calc.xpToReachNextLevel(1) + calc.xpToReachNextLevel(2),
            calc.cumulativeXpForLevel(3),
        )
    }

    @Test
    fun `resolve returns level 1 with no xp`() {
        val state = calc.resolve(0L)
        assertEquals(1, state.level)
        assertEquals(0L, state.currentLevelXp)
        assertEquals(200L, state.xpToNextLevel)
    }

    @Test
    fun `resolve places xp correctly within a level`() {
        // 250 total XP: 200 gets to level 2, 50 remains toward level 3 (needs 510).
        val state = calc.resolve(250L)
        assertEquals(2, state.level)
        assertEquals(50L, state.currentLevelXp)
        assertEquals(510L, state.xpToNextLevel)
    }

    @Test
    fun `resolve is consistent with cumulative at exact boundaries`() {
        for (level in 1..30) {
            val boundary = calc.cumulativeXpForLevel(level)
            val state = calc.resolve(boundary)
            assertEquals("boundary of level $level", level, state.level)
            assertEquals(0L, state.currentLevelXp)
        }
    }

    @Test
    fun `progress fraction is within 0 and 1`() {
        val state = calc.resolve(250L)
        assertTrue(state.progressFraction in 0f..1f)
    }
}
