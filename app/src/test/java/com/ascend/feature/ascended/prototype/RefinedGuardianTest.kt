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

/** The CP3 Guardian silhouette-refinement contract (pure). Only Guardian is refined so far. */
class RefinedGuardianTest {
    private fun refined(base: BodyBase = BodyBase.MALE) =
        ClassSilhouetteGeometry.build(AscendedClass.GUARDIAN, base, SilhouetteFidelity.REFINED).shapes

    private fun blockout(base: BodyBase = BodyBase.MALE) =
        ClassSilhouetteGeometry.build(AscendedClass.GUARDIAN, base, SilhouetteFidelity.BLOCKOUT).shapes

    @Test
    fun `all seven classes have a refined silhouette`() {
        AscendedClass.entries.forEach {
            assertTrue("$it should be refined", ClassSilhouetteGeometry.hasRefined(it))
        }
    }

    @Test
    fun `the refined Guardian is more developed than the blockout`() {
        assertTrue("refined has more shapes than the blockout", refined().size > blockout().size)
        assertTrue("refined Guardian is a developed figure", refined().size >= 18)
    }

    @Test
    fun `the refined Guardian keeps the fortress read — broad, tall, shield-bearing`() {
        val b = boundsOf(refined())
        val assassin = boundsOf(ClassSilhouetteGeometry.build(AscendedClass.ASSASSIN, BodyBase.MALE).shapes)
        assertTrue("broad", b.aspect > assassin.aspect)
        assertTrue("tall", b.height > 0.8f)
    }

    @Test
    fun `the refined Guardian resolves its masses — distinct mantle, torso, layered pauldrons, separated legs, shield, helm`() {
        val names = refined().map { it.name }.toSet()
        // Mantle distinct from the breastplate (torso mass), not one stacked block.
        assertTrue(names.containsAll(setOf("mantleBack", "breastplate")))
        // Layered pauldrons (cap + lame), not a single rectangle.
        assertTrue(names.containsAll(setOf("pauldronL", "pauldronLlame", "pauldronR", "pauldronRlame")))
        // Two separated greaves (stance + negative space), plus visible right arm.
        assertTrue(names.containsAll(setOf("legL", "legR", "armR")))
        // A crowned great-helm and an integrated shield.
        assertTrue(names.containsAll(setOf("helm", "crown", "shield", "shieldBoss")))
    }

    @Test
    fun `male and female refined Guardians share the identity but adapt proportions`() {
        val male = refined(BodyBase.MALE)
        val female = refined(BodyBase.FEMALE)
        assertEquals(male.map { it.name }, female.map { it.name })
        assertNotEquals(male, female)
    }
}
