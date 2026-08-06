package com.ascend.feature.ascended.prototype

import com.ascend.feature.ascended.prototype.body.MannequinGeometry
import com.ascend.feature.ascended.prototype.model.AttachPoint
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import com.ascend.feature.ascended.prototype.model.FakeAscended
import com.ascend.feature.ascended.prototype.model.MuscleRegionId
import com.ascend.feature.ascended.prototype.model.RegionSide
import com.ascend.feature.ascended.prototype.model.ambientRunning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The "Your Ascended" base mannequin geometry + base-state contract (pure, no Compose). */
class MannequinGeometryTest {
    @Test
    fun `male and female use separate base geometry`() {
        assertNotEquals(MannequinGeometry.build(BodyBase.MALE), MannequinGeometry.build(BodyBase.FEMALE))
    }

    @Test
    fun `male has broader shoulders and the female stronger hip rhythm`() {
        val male = MannequinGeometry.proportionsFor(BodyBase.MALE)
        val female = MannequinGeometry.proportionsFor(BodyBase.FEMALE)
        assertTrue("male shoulders broader", male.shoulderHalf > female.shoulderHalf)
        assertTrue("female hips wider", female.hipHalf > male.hipHalf)
        assertTrue("female waist narrower", female.waistHalf < male.waistHalf)
    }

    @Test
    fun `every muscle region is individually addressable`() {
        BodyBase.entries.forEach { base ->
            val regions = MannequinGeometry.build(base)
            assertEquals(MuscleRegionId.entries.toSet(), regions.map { it.id }.toSet())
            regions.forEach { assertTrue("${it.id}/${it.side} is a real polygon", it.polygon.size >= 3) }
        }
    }

    @Test
    fun `paired regions have a distinct left and right, central regions appear once`() {
        val regions = MannequinGeometry.build(BodyBase.MALE)
        // Deltoid is a paired region: exactly one LEFT and one RIGHT.
        val deltoids = regions.filter { it.id == MuscleRegionId.DELTOID }
        assertEquals(setOf(RegionSide.LEFT, RegionSide.RIGHT), deltoids.map { it.side }.toSet())
        // Head is central: exactly one CENTER instance.
        val heads = regions.filter { it.id == MuscleRegionId.HEAD }
        assertEquals(1, heads.size)
        assertEquals(RegionSide.CENTER, heads.first().side)
    }

    @Test
    fun `left regions mirror the right across the vertical axis`() {
        val regions = MannequinGeometry.build(BodyBase.MALE)
        val right = regions.first { it.id == MuscleRegionId.THIGH && it.side == RegionSide.RIGHT }
        val left = regions.first { it.id == MuscleRegionId.THIGH && it.side == RegionSide.LEFT }
        right.polygon.forEachIndexed { i, r ->
            assertEquals(1f - r.x, left.polygon[i].x, 1e-5f)
            assertEquals(r.y, left.polygon[i].y, 1e-5f)
        }
    }

    @Test
    fun `overlay attach points move with the body base`() {
        val male = MannequinGeometry.attachPoints(BodyBase.MALE)
        val female = MannequinGeometry.attachPoints(BodyBase.FEMALE)
        assertEquals(AttachPoint.entries.toSet(), male.keys)
        assertNotEquals(male[AttachPoint.SHOULDER_RIGHT], female[AttachPoint.SHOULDER_RIGHT])
    }

    @Test
    fun `the base state contains no weapon, artifact, armour, strong aura, or class`() {
        BodyBase.entries.forEach { base ->
            val s = FakeAscended.base(base)
            assertNull(s.ascendedClass)
            assertEquals(EvolutionStage.BASE, s.stage)
            assertEquals(0f, s.auraIntensity, 0f)
            assertEquals(0, s.equipmentTier)
            assertTrue(s.skillLevels.isEmpty())
            assertFalse(s.hasStrongAura)
            assertFalse(s.hasEquipment)
            assertFalse(s.hasClassIdentity)
        }
    }

    @Test
    fun `reduced motion stops ambient movement`() {
        val s = FakeAscended.base(BodyBase.FEMALE)
        assertTrue(ambientRunning(s))
        assertFalse(ambientRunning(s.copy(reducedMotion = true)))
    }
}
