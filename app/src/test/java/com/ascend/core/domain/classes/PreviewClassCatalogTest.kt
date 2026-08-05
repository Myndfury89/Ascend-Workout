package com.ascend.core.domain.classes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Preview classes are directional presentation only — never selectable, never real definitions. */
class PreviewClassCatalogTest {
    @Test
    fun `the four planned paths are previewed`() {
        assertEquals(listOf("assassin", "fighter", "ranger", "guardian"), PreviewClassCatalog.ALL.map { it.id })
    }

    @Test
    fun `no preview id collides with an implemented class`() {
        val real = ClassCatalog.ALL.map { it.id }.toSet()
        assertTrue("previews must be disjoint from real classes", PreviewClassCatalog.PREVIEW_IDS.none { it in real })
    }

    @Test
    fun `implemented classes are not previews`() {
        ClassCatalog.ALL.forEach {
            assertFalse("${it.id} is implemented, not a preview", it.id in PreviewClassCatalog.PREVIEW_IDS)
        }
    }

    @Test
    fun `every preview carries a summary, styles, and a suggested proficiency label`() {
        PreviewClassCatalog.ALL.forEach { p ->
            assertTrue("${p.id} has an affinity summary", p.affinitySummary.startsWith("Trains "))
            assertTrue("${p.id} has primary styles", p.primaryStyles.isNotEmpty())
            assertTrue("${p.id} has a suggested proficiency", p.suggestedProficiencyName.isNotBlank())
        }
    }

    @Test
    fun `previews are absent from the seedable catalog so the engine never scores them`() {
        val seedIds = ClassCatalog.ALL.map { it.id }
        PreviewClassCatalog.ALL.forEach { assertFalse(it.id in seedIds) }
    }
}
