package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.sigil.ClassKey
import com.ascend.core.designsystem.sigil.GlyphStroke
import com.ascend.core.designsystem.sigil.MedallionGlyph
import com.ascend.core.designsystem.sigil.MedallionRole
import com.ascend.core.designsystem.sigil.ProficiencyGlyphGeometry
import com.ascend.core.designsystem.sigil.RankTier
import com.ascend.core.designsystem.sigil.SigilClassStyleCatalog
import com.ascend.core.designsystem.sigil.SigilGeometryKey
import com.ascend.core.designsystem.sigil.buildOrnateGeometry
import com.ascend.core.designsystem.sigil.statusVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure tests for the per-class proficiency medallion glyphs (all seven classes). */
class ProficiencyMedallionGlyphTest {
    private val classes = ClassKey.entries.filter { it != ClassKey.NEUTRAL }

    private val newClassGlyphs =
        mapOf(
            ClassKey.ASSASSIN to MedallionGlyph.PRECISION_BRACKETS,
            ClassKey.FIGHTER to MedallionGlyph.COMBAT_PULSE,
            ClassKey.RANGER to MedallionGlyph.PURSUIT_TRAJECTORY,
            ClassKey.GUARDIAN to MedallionGlyph.FORTITUDE_RAMPART,
        )

    @Test
    fun `every class has its own proficiency glyph and neutral has none`() {
        val glyphs = classes.map { SigilClassStyleCatalog.styleFor(it).proficiencyGlyph }
        glyphs.forEachIndexed { i, g -> assertNotNull("${classes[i]} has a proficiency glyph", g) }
        assertEquals("no two classes share a proficiency glyph", classes.size, glyphs.toSet().size)
        assertNull(SigilClassStyleCatalog.styleFor(ClassKey.NEUTRAL).proficiencyGlyph)
    }

    @Test
    fun `proficiency glyphs never reuse an attribute glyph`() {
        classes.forEach {
            val glyph = SigilClassStyleCatalog.styleFor(it).proficiencyGlyph
            assertFalse("$it proficiency glyph is not an attribute glyph", glyph in MedallionGlyph.attributeGlyphs)
        }
    }

    @Test
    fun `existing class glyphs are unchanged`() {
        assertEquals(MedallionGlyph.FORCE_BURST, SigilClassStyleCatalog.styleFor(ClassKey.BERSERKER).proficiencyGlyph)
        assertEquals(MedallionGlyph.BODY_RINGLET, SigilClassStyleCatalog.styleFor(ClassKey.MONK).proficiencyGlyph)
        assertEquals(MedallionGlyph.ENERGY_FLOW, SigilClassStyleCatalog.styleFor(ClassKey.MAGE).proficiencyGlyph)
    }

    @Test
    fun `every new glyph has data-driven strokes that stay inside the glyph radius`() {
        assertEquals(newClassGlyphs.values.toSet(), ProficiencyGlyphGeometry.glyphs)
        newClassGlyphs.values.forEach { glyph ->
            val strokes = ProficiencyGlyphGeometry.strokesFor(glyph)
            assertTrue("$glyph has strokes", strokes.isNotEmpty())
            strokes.filterIsInstance<GlyphStroke.Polyline>().flatMap { it.points }.forEach { p ->
                assertTrue("$glyph point $p within 0.75u", p.getDistance() <= MAX_REACH)
            }
            strokes.filterIsInstance<GlyphStroke.Dot>().forEach { d ->
                assertTrue("$glyph dot within 0.75u", d.center.getDistance() <= MAX_REACH)
            }
        }
        assertTrue(ProficiencyGlyphGeometry.strokesFor(MedallionGlyph.FORCE_BURST).isEmpty())
    }

    @Test
    fun `rampart base is open and mirrored`() {
        val points = (ProficiencyGlyphGeometry.fortitudeRampart.single() as GlyphStroke.Polyline).points
        assertEquals(12, points.size)
        assertEquals(points.first().y, points.last().y, TOL)
        assertEquals(-points.first().x, points.last().x, TOL)
    }

    @Test
    fun `new classes expose a sixth proficiency medallion at the bottom anchor`() {
        newClassGlyphs.forEach { (key, glyph) ->
            val geo =
                buildOrnateGeometry(
                    SigilGeometryKey(
                        rankTier = RankTier.GOLD,
                        variant = key.statusVariant(),
                        medallionCount = 6,
                        simplified = false,
                        minimal = false,
                        classDistinct = true,
                        classKey = key,
                    ),
                    radiusPx = 200f,
                )
            val prof = geo.instrumentation.medallion(MedallionRole.PROFICIENCY)
            assertNotNull("$key proficiency layer", prof)
            assertEquals("$key glyph", glyph, prof!!.glyph)
            assertEquals(6, geo.instrumentation.medallions.size)
            // Bottom anchor: straight below the centre on the 0.7 r medallion ring.
            assertEquals(0f, prof.center.x, 1e-2f)
            assertEquals(200f * 0.7f, prof.center.y, 1e-2f)
        }
    }

    private companion object {
        const val TOL = 1e-4f
        const val MAX_REACH = 0.75f
    }
}
