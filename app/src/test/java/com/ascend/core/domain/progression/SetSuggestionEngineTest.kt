package com.ascend.core.domain.progression

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetSuggestionEngineTest {

    private val engine = SetSuggestionEngine()

    @Test
    fun `preferred size produces even sets`() {
        val s = engine.suggest(SetSuggestionInput(remaining = 125, preferredSetSize = 25, minimumSetSize = 10))
        assertEquals(listOf(25, 25, 25, 25, 25), s.primary.sets)
        assertEquals(125, s.primary.total)
    }

    @Test
    fun `small remainder folds into last set when no max is configured`() {
        val s = engine.suggest(SetSuggestionInput(remaining = 125, preferredSetSize = 40, minimumSetSize = 10))
        assertEquals(listOf(40, 40, 45), s.primary.sets)
        assertEquals(125, s.primary.total)
    }

    @Test
    fun `never recommends a set larger than the configured maximum`() {
        val s = engine.suggest(
            SetSuggestionInput(remaining = 80, preferredSetSize = 25, minimumSetSize = 10, maximumSetSize = 50),
        )
        assertEquals(80, s.primary.total)
        val allPlans = listOf(s.primary) + s.alternatives
        allPlans.forEach { plan ->
            plan.sets.forEach { setSize -> assertTrue("set $setSize exceeds max", setSize <= 50) }
        }
    }

    @Test
    fun `unrestricted opt-in may exceed the configured maximum`() {
        val s = engine.suggest(
            SetSuggestionInput(
                remaining = 200, preferredSetSize = 200, minimumSetSize = 10,
                maximumSetSize = 50, allowUnrestricted = true,
            ),
        )
        assertEquals(listOf(200), s.primary.sets)
    }

    @Test
    fun `zero or negative remaining yields no sets`() {
        assertEquals(0, engine.suggest(SetSuggestionInput(remaining = 0, preferredSetSize = 25)).primary.setCount)
    }

    @Test
    fun `every plan totals the remaining amount`() {
        val s = engine.suggest(SetSuggestionInput(remaining = 137, preferredSetSize = 30, minimumSetSize = 10, maximumSetSize = 50))
        (listOf(s.primary) + s.alternatives).forEach { assertEquals(137, it.total) }
    }
}
