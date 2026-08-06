package com.ascend.feature.ascended.prototype

import com.ascend.feature.ascended.prototype.body.ClassSilhouetteGeometry
import com.ascend.feature.ascended.prototype.body.SilhouetteFidelity
import com.ascend.feature.ascended.prototype.body.boundsOf
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The full CP3 refinement pass: every class is refined beyond its CP2 blockout (pure). */
class RefinedSilhouettesTest {
    private val headLike = setOf("head", "helm", "hood", "hatCone")

    private fun refined(
        cls: AscendedClass,
        base: BodyBase = BodyBase.MALE,
    ) = ClassSilhouetteGeometry.build(cls, base, SilhouetteFidelity.REFINED).shapes

    private fun blockout(
        cls: AscendedClass,
        base: BodyBase = BodyBase.MALE,
    ) = ClassSilhouetteGeometry.build(cls, base, SilhouetteFidelity.BLOCKOUT).shapes

    @Test
    fun `every class is more developed when refined than as a blockout`() {
        AscendedClass.entries.forEach { cls ->
            assertTrue(
                "$cls refined (${refined(cls).size}) should exceed blockout (${blockout(cls).size})",
                refined(cls).size > blockout(cls).size,
            )
        }
    }

    @Test
    fun `every refined class still reads as a body, not a lone prop`() {
        AscendedClass.entries.forEach { cls ->
            val names = refined(cls).map { it.name }.toSet()
            assertTrue("$cls refined must have a head/helm/hood", names.any { it in headLike })
        }
    }

    @Test
    fun `every refined class preserves its identity across body bases while adapting proportions`() {
        AscendedClass.entries.forEach { cls ->
            val male = refined(cls, BodyBase.MALE)
            val female = refined(cls, BodyBase.FEMALE)
            assertEquals("$cls shape set differs by base", male.map { it.name }, female.map { it.name })
            assertNotEquals("$cls geometry identical across bases", male, female)
        }
    }

    @Test
    fun `the refined proportion reads persist — Berserker broad, Magician tall, Guardian broad`() {
        val berserker = boundsOf(refined(AscendedClass.BERSERKER))
        val magician = boundsOf(refined(AscendedClass.MAGICIAN))
        val fighter = boundsOf(refined(AscendedClass.FIGHTER))
        val guardian = boundsOf(refined(AscendedClass.GUARDIAN))
        assertTrue("Berserker broader than Fighter", berserker.aspect > fighter.aspect)
        assertTrue("Guardian broader than Fighter", guardian.aspect > fighter.aspect)
        assertTrue("Magician taller than Berserker", magician.height > berserker.height)
    }

    @Test
    fun `the body-proportion hierarchy is fidelity-independent`() {
        // nominalShoulderHalf anchors the massing for both blockout and refined figures.
        val sh = { cls: AscendedClass -> ClassSilhouetteGeometry.nominalShoulderHalf(cls) }
        assertTrue(sh(AscendedClass.BERSERKER) > sh(AscendedClass.FIGHTER))
        assertTrue(sh(AscendedClass.FIGHTER) > sh(AscendedClass.MAGICIAN))
        assertTrue(sh(AscendedClass.MAGICIAN) > sh(AscendedClass.ASSASSIN))
    }
}
