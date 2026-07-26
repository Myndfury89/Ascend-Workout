package com.ascend.core.domain.progression

import com.ascend.core.model.Rank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RankCalculatorTest {

    private val calc = RankCalculator()

    @Test
    fun `low level starts at initiate`() {
        assertEquals(Rank.INITIATE, calc.rankFor(level = 1, lifetimeXp = 0))
    }

    @Test
    fun `level thresholds map to expected ranks`() {
        assertEquals(Rank.IRON, calc.rankFor(level = 5, lifetimeXp = 0))
        assertEquals(Rank.BRONZE, calc.rankFor(level = 10, lifetimeXp = 0))
        assertEquals(Rank.MYTHIC, calc.rankFor(level = 120, lifetimeXp = 0))
    }

    @Test
    fun `lifetime xp nudges a borderline rank upward`() {
        // level 4 alone = Initiate; +2000 XP adds 2.0 score -> 6.0 -> Iron.
        assertEquals(Rank.INITIATE, calc.rankFor(level = 4, lifetimeXp = 0))
        assertEquals(Rank.IRON, calc.rankFor(level = 4, lifetimeXp = 2_000))
    }

    @Test
    fun `rank never decreases as level increases`() {
        var previousOrdinal = -1
        for (level in 1..130) {
            val ordinal = calc.rankFor(level, lifetimeXp = 0).ordinal
            assertTrue("rank should not decrease at level $level", ordinal >= previousOrdinal)
            previousOrdinal = ordinal
        }
    }
}
