package com.ascend.feature.dashboard.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CP-B: the outer instrumentation is an addressable, shared-origin layer set — static rings +
 * isolated medallions + the two LIVE XP rings kept separate (never flattened into static artwork).
 */
class SigilInstrumentationTest {
    private fun geometry(
        variant: StatusClassVariant,
        showProficiency: Boolean,
    ) = buildOrnateGeometry(
        SigilGeometryKey(
            RankTier.GOLD,
            variant,
            medallionCount = MedallionGlyph.attributeGlyphs.size + if (showProficiency) 1 else 0,
            simplified = false,
            minimal = false,
            classDistinct = true,
        ),
        radiusPx = 240f,
    )

    @Test
    fun `the two XP rings are live and kept separate from the static structural rings`() {
        val inst = geometry(StatusClassVariant.BERSERKER, showProficiency = true).instrumentation
        assertEquals("two live progress rings", 2, inst.liveRings.size)
        assertEquals("two static structural rings", 2, inst.structuralRings.size)
        assertTrue(inst.ring(SigilRingRole.PLAYER_XP)!!.live)
        assertTrue(inst.ring(SigilRingRole.CLASS_XP)!!.live)
        assertTrue("structural primary is static", !inst.ring(SigilRingRole.STRUCTURAL_PRIMARY)!!.live)
        // Player XP and Class XP register to different radii (position-distinguishable).
        assertTrue(inst.ring(SigilRingRole.PLAYER_XP)!!.radiusFraction != inst.ring(SigilRingRole.CLASS_XP)!!.radiusFraction)
    }

    @Test
    fun `each attribute medallion plus the proficiency medallion is an isolated addressable layer`() {
        val inst = geometry(StatusClassVariant.MONK, showProficiency = true).instrumentation
        listOf(
            MedallionRole.STRENGTH,
            MedallionRole.ENDURANCE,
            MedallionRole.AGILITY,
            MedallionRole.DISCIPLINE,
            MedallionRole.RECOVERY,
            MedallionRole.PROFICIENCY,
        ).forEach { role ->
            assertNotNull("medallion $role is addressable", inst.medallion(role))
        }
        assertEquals(6, inst.medallions.size)
        val prof = inst.medallion(MedallionRole.PROFICIENCY)!!
        assertTrue(prof.isProficiency)
        assertEquals("proficiency uses its own pulse channel", -1, prof.pulseIndex)
        // The Monk proficiency glyph is isolated on this layer.
        assertEquals(MedallionGlyph.BODY_RINGLET, prof.glyph)
    }

    @Test
    fun `a class with no proficiency shown exposes five attribute medallions and no proficiency layer`() {
        val inst = geometry(StatusClassVariant.NEUTRAL, showProficiency = false).instrumentation
        assertEquals(5, inst.medallions.size)
        assertNull(inst.medallion(MedallionRole.PROFICIENCY))
        assertEquals(0, inst.medallion(MedallionRole.STRENGTH)!!.pulseIndex)
    }

    @Test
    fun `every instrumentation layer registers to the shared centre origin`() {
        val inst = geometry(StatusClassVariant.MAGICIAN, showProficiency = true).instrumentation
        // Medallions sit on the mid-outer medallion ring (~0.7 r), i.e. off-centre but bounded.
        inst.medallions.forEach {
            val dist = kotlin.math.hypot(it.center.x.toDouble(), it.center.y.toDouble())
            assertTrue("medallion within the sigil radius", dist in 1.0..240.0)
        }
        // Ring radii are fractions of the sigil radius, so they scale from the same origin.
        inst.rings.forEach { assertTrue(it.radiusFraction in 0f..1f) }
        assertEquals(150f, SigilLayout.ORIGIN)
    }
}
