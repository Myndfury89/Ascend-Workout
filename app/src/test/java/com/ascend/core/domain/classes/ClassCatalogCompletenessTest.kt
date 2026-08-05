package com.ascend.core.domain.classes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the rule from the assessment: a class is only production-selectable when its definition is
 * complete. If a future class is seeded without full data, one of these fails before it can ship.
 */
class ClassCatalogCompletenessTest {
    private val selectable = ClassCatalog.ALL

    @Test
    fun `every selectable class has a complete definition`() {
        selectable.forEach { d ->
            assertTrue("${d.id} id", d.id.isNotBlank())
            assertTrue("${d.id} name", d.name.isNotBlank())
            assertTrue("${d.id} title", d.classTitle.isNotBlank())
            assertTrue("${d.id} description", d.description.isNotBlank())
            assertTrue("${d.id} fitness identity", d.fitnessIdentity.isNotBlank())
            assertTrue("${d.id} categories", d.favoredWorkoutCategories.isNotEmpty())
            assertTrue("${d.id} enabled", d.enabled)
        }
    }

    @Test
    fun `every selectable class has affinity tags`() {
        selectable.forEach { assertTrue("${it.id} has favored tags", it.favoredTags.isNotEmpty()) }
    }

    @Test
    fun `every selectable class has a unique proficiency definition`() {
        selectable.forEach {
            assertTrue("${it.id} proficiency key", it.uniqueProficiencyKey.isNotBlank())
            assertTrue("${it.id} proficiency name", it.uniqueProficiencyName.isNotBlank())
        }
        val keys = selectable.map { it.uniqueProficiencyKey }
        assertTrue("proficiency keys are unique", keys.size == keys.toSet().size)
    }

    @Test
    fun `every selectable class has class-xp behavior and attribute affinities`() {
        selectable.forEach {
            assertTrue("${it.id} favored xp multiplier", it.favoredClassXpMultiplier > 0.0)
            assertTrue("${it.id} neutral xp multiplier", it.neutralClassXpMultiplier > 0.0)
            assertTrue("${it.id} attribute multipliers", it.attributeMultipliers.isNotEmpty())
            assertTrue("${it.id} primary attributes", it.primaryAttributes.isNotEmpty())
        }
    }

    @Test
    fun `every selectable class has a recommendation and quest-priority preference entry`() {
        selectable.forEach {
            assertFalse(
                "${it.id} has a progression-preference entry",
                com.ascend.core.domain.training.ranking.ClassProgressionPreferenceCatalog.byId(it.id) == null,
            )
        }
    }
}
