package com.ascend.feature.ascended.prototype

import com.ascend.feature.ascended.prototype.body.ClassSilhouetteGeometry
import com.ascend.feature.ascended.prototype.body.boundsOf
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The seven-class flat silhouette shape-language contract (pure, no Compose). */
class ClassSilhouetteTest {
    private val headLike = setOf("head", "helm", "hood", "hatCone")

    @Test
    fun `all seven classes build for both body bases`() {
        AscendedClass.entries.forEach { cls ->
            BodyBase.entries.forEach { base ->
                assertTrue("$cls/$base has shapes", ClassSilhouetteGeometry.build(cls, base).shapes.isNotEmpty())
            }
        }
    }

    @Test
    fun `each class uses a small number of large shapes (8 to 16)`() {
        AscendedClass.entries.forEach { cls ->
            val n = ClassSilhouetteGeometry.build(cls, BodyBase.MALE).shapes.size
            assertTrue("$cls has $n shapes, expected 8..16", n in 8..16)
        }
    }

    @Test
    fun `the proportion hierarchy makes classes distinct by shoulder width`() {
        fun sh(cls: AscendedClass) = ClassSilhouetteGeometry.nominalShoulderHalf(cls)
        // Berserker widest, Assassin narrowest, with the documented ordering between.
        assertTrue(sh(AscendedClass.BERSERKER) > sh(AscendedClass.GUARDIAN))
        assertTrue(sh(AscendedClass.GUARDIAN) > sh(AscendedClass.FIGHTER))
        assertTrue(sh(AscendedClass.FIGHTER) > sh(AscendedClass.RANGER))
        assertTrue(sh(AscendedClass.RANGER) > sh(AscendedClass.MONK))
        assertTrue(sh(AscendedClass.MONK) > sh(AscendedClass.MAGICIAN))
        assertTrue(sh(AscendedClass.MAGICIAN) > sh(AscendedClass.ASSASSIN))
    }

    @Test
    fun `male and female share a class identity but adapt proportions`() {
        AscendedClass.entries.forEach { cls ->
            val male = ClassSilhouetteGeometry.build(cls, BodyBase.MALE).shapes
            val female = ClassSilhouetteGeometry.build(cls, BodyBase.FEMALE).shapes
            // Same signature shapes (identity preserved) …
            assertEquals("$cls shape set differs by base", male.map { it.name }, female.map { it.name })
            // … but the geometry adapts (proportions differ).
            assertNotEquals("$cls geometry identical across bases", male, female)
        }
    }

    @Test
    fun `no class relies on a weapon alone — each has a head, helm, or hood`() {
        AscendedClass.entries.forEach { cls ->
            val names = ClassSilhouetteGeometry.build(cls, BodyBase.MALE).shapes.map { it.name }.toSet()
            assertTrue("$cls must read as a body, not just a prop", names.any { it in headLike })
        }
    }

    @Test
    fun `overall silhouettes differ — Berserker broad, Mage tall, Assassin narrow`() {
        val berserker = boundsOf(ClassSilhouetteGeometry.build(AscendedClass.BERSERKER, BodyBase.MALE).shapes)
        val mage = boundsOf(ClassSilhouetteGeometry.build(AscendedClass.MAGICIAN, BodyBase.MALE).shapes)
        val assassin = boundsOf(ClassSilhouetteGeometry.build(AscendedClass.ASSASSIN, BodyBase.MALE).shapes)
        // Berserker reads much broader than Assassin.
        assertTrue("Berserker broader than Assassin", berserker.aspect > assassin.aspect)
        // Mage reads taller than the compact Berserker.
        assertTrue("Mage taller than Berserker", mage.height > berserker.height)
    }
}
