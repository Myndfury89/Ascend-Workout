package com.ascend.core.domain.progression

import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class AttributeProgressCalculatorTest {

    private val calc = AttributeProgressCalculator()

    @Test
    fun `push-up quest grants both strength and discipline`() {
        // volume 200 * 0.15 * 1.0 = 30 strength; discipline base 10.
        val deltas = calc.forQuestCompletion(AttributeType.STRENGTH, volume = 200.0)
        assertEquals(30L, deltas[AttributeType.STRENGTH])
        assertEquals(10L, deltas[AttributeType.DISCIPLINE])
    }

    @Test
    fun `primary volume is capped`() {
        val deltas = calc.forQuestCompletion(AttributeType.STRENGTH, volume = 100_000.0)
        assertEquals(200L, deltas[AttributeType.STRENGTH]) // volumeCap
    }

    @Test
    fun `difficulty increases both awards`() {
        val deltas = calc.forQuestCompletion(AttributeType.STRENGTH, volume = 200.0, difficulty = Difficulty.HARD)
        // 200 * 0.15 * 1.3 = 39 ; discipline 10 * 1.3 = 13
        assertEquals(39L, deltas[AttributeType.STRENGTH])
        assertEquals(13L, deltas[AttributeType.DISCIPLINE])
    }

    @Test
    fun `discipline-primary quest merges into a single entry`() {
        // primary 100 * 0.15 = 15, plus discipline base 10 = 25 in one key.
        val deltas = calc.forQuestCompletion(AttributeType.DISCIPLINE, volume = 100.0)
        assertEquals(1, deltas.size)
        assertEquals(25L, deltas[AttributeType.DISCIPLINE])
    }
}
