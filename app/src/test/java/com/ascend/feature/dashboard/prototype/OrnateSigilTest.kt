package com.ascend.feature.dashboard.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure tests for the Original Ornate Sigil System — data-driven config, glyphs, rings, layout. */
class OrnateSigilTest {
    @Test
    fun `rank complexity increases monotonically with tier`() {
        val scores = RankTier.entries.map { RankGeometryCatalog.configFor(it).complexityScore }
        scores.zipWithNext().forEach { (lower, higher) ->
            assertTrue("complexity must rise with rank ($lower !< $higher)", higher > lower)
        }
    }

    @Test
    fun `each rank maps to a distinct geometry configuration`() {
        val configs = RankTier.entries.map { RankGeometryCatalog.configFor(it) }
        assertEquals(RankTier.entries.size, configs.toSet().size)
        // Initiate is a minimal four-point construction; Mythic is maximal.
        assertEquals(4, RankGeometryCatalog.configFor(RankTier.INITIATE).basePolygonSides)
        assertTrue(RankGeometryCatalog.configFor(RankTier.MYTHIC).starLayers >= 4)
    }

    @Test
    fun `rank promotion increases geometry complexity`() {
        assertTrue(
            RankGeometryCatalog.configFor(RankTier.APEX).complexityScore >
                RankGeometryCatalog.configFor(RankTier.GOLD).complexityScore,
        )
    }

    @Test
    fun `there are exactly five original attribute glyphs from the local set`() {
        assertEquals(5, MedallionGlyph.attributeGlyphs.size)
        // Every attribute maps to a glyph that lives in the local closed enum.
        listOf("Strength", "Endurance", "Agility", "Discipline", "Recovery").forEach {
            assertTrue(MedallionGlyph.forAttribute(it) in MedallionGlyph.entries)
        }
    }

    @Test
    fun `five universal medallions are always present and the sixth appears only when enabled`() {
        val five = OrnateSigilState(RankTier.GOLD, StatusClassVariant.MONK, 0.5f, 0.4f, -1, showProficiency = false)
        assertEquals(5, five.medallionCount)
        val six = five.copy(showProficiency = true)
        assertEquals(6, six.medallionCount)
        // A secondary class never adds a seventh — count is capped at six by construction.
        assertTrue(six.medallionCount <= 6)
    }

    @Test
    fun `medallion angles are stable, five spread evenly and the sixth anchored at the bottom`() {
        assertEquals(5, medallionAngles(5, hasProficiency = false).size)
        val six = medallionAngles(6, hasProficiency = true)
        assertEquals(6, six.size)
        // The proficiency medallion (last) sits at the bottom anchor (+90 degrees = PI/2).
        assertEquals((Math.PI / 2).toFloat(), six.last(), 1e-3f)
    }

    @Test
    fun `class styles share the structure but differ in emphasis`() {
        val berserker = SigilClassStyleCatalog.styleFor(StatusClassVariant.BERSERKER)
        val monk = SigilClassStyleCatalog.styleFor(StatusClassVariant.MONK)
        val magician = SigilClassStyleCatalog.styleFor(StatusClassVariant.MAGICIAN)
        assertTrue("berserker heavier than monk", berserker.lineWeight > monk.lineWeight)
        assertTrue("magician uses arcs", magician.useArcs)
        assertTrue("berserker stronger radial emphasis", berserker.pointEmphasis > monk.pointEmphasis)
        assertNull("neutral has no proficiency glyph", SigilClassStyleCatalog.styleFor(StatusClassVariant.NEUTRAL).proficiencyGlyph)
        assertEquals(MedallionGlyph.FORCE_BURST, berserker.proficiencyGlyph)
    }

    @Test
    fun `geometry builder yields more inner paths and connectors for higher ranks`() {
        val initiate = buildOrnateGeometry(SigilGeometryKey(RankTier.INITIATE, StatusClassVariant.MONK, 5, false, false), 200f)
        val mythic = buildOrnateGeometry(SigilGeometryKey(RankTier.MYTHIC, StatusClassVariant.MONK, 5, false, false), 200f)
        assertTrue(mythic.innerPaths.size > initiate.innerPaths.size)
        assertEquals(RankGeometryCatalog.configFor(RankTier.MYTHIC).radialConnectors, mythic.connectors.size)
        // Six medallions when proficiency is included.
        val six = buildOrnateGeometry(SigilGeometryKey(RankTier.GOLD, StatusClassVariant.BERSERKER, 6, false, false), 200f)
        assertEquals(6, six.medallionCenters.size)
        assertEquals(5, six.proficiencyIndex)
    }

    @Test
    fun `minimal geometry drops the decorative border motif and simplified halves it`() {
        val full = buildOrnateGeometry(SigilGeometryKey(RankTier.MYTHIC, StatusClassVariant.MONK, 5, false, false), 200f)
        val simplified = buildOrnateGeometry(SigilGeometryKey(RankTier.MYTHIC, StatusClassVariant.MONK, 5, true, false), 200f)
        val minimal = buildOrnateGeometry(SigilGeometryKey(RankTier.MYTHIC, StatusClassVariant.MONK, 5, false, true), 200f)
        assertTrue("full motif has ticks", full.motifTicks > 0)
        assertTrue("simplified reduces the motif", simplified.motifTicks in 1 until full.motifTicks)
        assertEquals("minimal removes the motif", 0, minimal.motifTicks)
    }

    // ---- data → sigil mapping ----

    @Test
    fun `player level-up changes ring progress but not rank geometry`() {
        val standard = FakeStatusPrototype.dataFor(StatusPrototypeStateId.STANDARD, StatusClassVariant.MONK, reducedMotion = false)
        val levelUp = FakeStatusPrototype.dataFor(StatusPrototypeStateId.PLAYER_LEVEL_UP, StatusClassVariant.MONK, reducedMotion = false)
        assertEquals("rank geometry unchanged by an ordinary level-up", standard.rankTier, levelUp.rankTier)
        assertTrue("overflow XP fraction is low after level-up", levelUp.playerXpFraction < standard.playerXpFraction)
    }

    @Test
    fun `class level-up changes class ring but not player ring`() {
        val standard = FakeStatusPrototype.dataFor(StatusPrototypeStateId.STANDARD, StatusClassVariant.MONK, reducedMotion = false)
        val classUp = FakeStatusPrototype.dataFor(StatusPrototypeStateId.CLASS_LEVEL_UP, StatusClassVariant.MONK, reducedMotion = false)
        assertEquals("player ring unchanged", standard.playerXpFraction, classUp.playerXpFraction, 1e-6f)
        assertTrue("class ring reset lower", classUp.classXpFraction < standard.classXpFraction)
    }

    @Test
    fun `an attribute gain targets only the matching medallion`() {
        val attr = FakeStatusPrototype.dataFor(StatusPrototypeStateId.ATTRIBUTE_UP, StatusClassVariant.BERSERKER, reducedMotion = false)
        assertEquals(0, attr.activeMedallionIndex)
        val standard = FakeStatusPrototype.dataFor(StatusPrototypeStateId.STANDARD, StatusClassVariant.BERSERKER, reducedMotion = false)
        assertEquals(-1, standard.activeMedallionIndex)
    }

    @Test
    fun `rank-promotion state raises the rank tier`() {
        val promo = FakeStatusPrototype.dataFor(StatusPrototypeStateId.RANK_PROMOTION, StatusClassVariant.MAGICIAN, reducedMotion = false)
        assertEquals(RankTier.APEX, promo.rankTier)
    }

    @Test
    fun `the sigil description names rank, class, and progression percentages`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.MID_RANK, StatusClassVariant.MONK, reducedMotion = false)
        val desc = sigilDescription(data, 0.72f, 0.44f, 3)
        assertTrue(desc.contains("Silver-rank Monk"))
        assertTrue(desc.contains("72 percent"))
        assertTrue(desc.contains("44 percent"))
        assertTrue(desc.contains("Discipline recently increased"))
    }
}
