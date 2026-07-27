package com.ascend.core.domain.classes

import com.ascend.core.model.ActivityTags
import com.ascend.core.model.AttributeType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The class engine is pure and data‑driven — every assertion here flows from a
 * [com.ascend.core.model.ClassDefinition], never from a branch on the class id.
 */
class ClassProgressionCalculatorTest {
    private val calc = ClassProgressionCalculator()
    private val pushupTags = setOf(ActivityTags.BODYWEIGHT, ActivityTags.MUSCULAR_ENDURANCE)

    @Test
    fun `affinity is the favored fraction of the activity's tags`() {
        // Monk favors both push-up tags; Berserker favors neither.
        assertEquals(1.0, calc.affinity(pushupTags, ClassCatalog.MONK), 1e-9)
        assertEquals(0.0, calc.affinity(pushupTags, ClassCatalog.BERSERKER), 1e-9)
        // Untagged activity is always neutral.
        assertEquals(0.0, calc.affinity(emptySet(), ClassCatalog.MONK), 1e-9)
    }

    @Test
    fun `class xp multiplier interpolates neutral to favored by affinity`() {
        assertEquals(0.75, calc.classXpMultiplier(0.0, ClassCatalog.MONK), 1e-9)
        assertEquals(1.30, calc.classXpMultiplier(1.0, ClassCatalog.MONK), 1e-9)
        assertEquals(1.025, calc.classXpMultiplier(0.5, ClassCatalog.MONK), 1e-9)
    }

    @Test
    fun `class xp scales base player xp, never mutating it`() {
        // 350 * 0.6 * 1.30 = 273  (favored Monk); 350 * 0.6 * 0.75 = 157.5 -> 158 (non-favored).
        assertEquals(273L, calc.classXp(350, affinity = 1.0, ClassCatalog.MONK, allocation = 1.0))
        assertEquals(158L, calc.classXp(350, affinity = 0.0, ClassCatalog.BERSERKER, allocation = 1.0))
        // Secondary allocation halves it.
        assertEquals(137L, calc.classXp(350, affinity = 1.0, ClassCatalog.MONK, allocation = 0.5))
    }

    @Test
    fun `attribute scaling only reshapes the existing distribution`() {
        val base = linkedMapOf(AttributeType.STRENGTH to 30L, AttributeType.DISCIPLINE to 10L)

        val monk = calc.scaledAttributeProficiency(base, ClassCatalog.MONK)
        assertEquals(38L, monk[AttributeType.STRENGTH]) // 30 * 1.25 = 37.5 -> 38
        assertEquals(14L, monk[AttributeType.DISCIPLINE]) // 10 * 1.35 = 13.5 -> 14
        // No attribute the activity didn't already train is invented.
        assertEquals(setOf(AttributeType.STRENGTH, AttributeType.DISCIPLINE), monk.keys)

        val berserker = calc.scaledAttributeProficiency(base, ClassCatalog.BERSERKER)
        assertEquals(45L, berserker[AttributeType.STRENGTH]) // 30 * 1.50
        assertEquals(10L, berserker[AttributeType.DISCIPLINE]) // 10 * 1.00
    }

    @Test
    fun `a zero base attribute stays zero after the multiplier`() {
        // Base Agility 0, Monk Agility multiplier 1.20 -> still 0 (no proficiency invented).
        val base = linkedMapOf(AttributeType.STRENGTH to 20L, AttributeType.AGILITY to 0L)
        val scaled = calc.scaledAttributeProficiency(base, ClassCatalog.MONK)
        assertEquals(25L, scaled[AttributeType.STRENGTH]) // 20 * 1.25
        assertEquals(null, scaled[AttributeType.AGILITY]) // 0 * 1.20 = 0, not awarded
    }

    @Test
    fun `unique proficiency is driven by favored activities and zero when unfavored`() {
        // Favored (affinity 1): 40 * 1.0 * 1.30 * 1.5 = 78.
        assertEquals(78L, calc.uniqueProficiency(baseMagnitude = 40, affinity = 1.0, ClassCatalog.MONK, allocation = 1.0))
        // Unfavored push-ups for a Berserker -> nothing.
        assertEquals(0L, calc.uniqueProficiency(baseMagnitude = 40, affinity = 0.0, ClassCatalog.BERSERKER, allocation = 1.0))
    }
}
