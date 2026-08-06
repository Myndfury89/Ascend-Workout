package com.ascend.feature.dashboard.prototype

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CP2 class differentiation as pure data + geometry invariants. Guarantees each implemented class
 * has a distinct silhouette, core anchor, stroke emphasis, and rotation feel — by shape, not colour —
 * and that the class-distinct geometry is opt-in (the legacy build the production screen uses is
 * untouched: no core anchor, no per-variant central shape).
 */
class ClassSigilGeometryTest {
    private val implemented =
        listOf(StatusClassVariant.BERSERKER, StatusClassVariant.MONK, StatusClassVariant.MAGICIAN)

    @Test
    fun `each implemented class has a distinct central structure and core anchor`() {
        val centrals = implemented.map { ClassSigilGeometryCatalog.forVariant(it).central }
        val anchors = implemented.map { ClassSigilGeometryCatalog.forVariant(it).anchor }
        assertEquals("central silhouettes are all distinct", centrals.size, centrals.toSet().size)
        assertEquals("core anchors are all distinct", anchors.size, anchors.toSet().size)
    }

    @Test
    fun `the handoff shape language is honoured per class`() {
        assertEquals(CentralStructure.FIVE_POINT_STAR, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.BERSERKER).central)
        assertEquals(CoreAnchorShape.SPIKE_STAR_4, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.BERSERKER).anchor)
        assertEquals(CentralStructure.HEXAGRAM, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MONK).central)
        assertEquals(CoreAnchorShape.HEXAGON, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MONK).anchor)
        assertEquals(CentralStructure.ROSETTE, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MAGICIAN).central)
        assertEquals(CoreAnchorShape.CIRCLE, ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MAGICIAN).anchor)
    }

    @Test
    fun `rotation feel differentiates speed and direction`() {
        val b = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.BERSERKER).rotationFactor
        val m = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MONK).rotationFactor
        val g = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MAGICIAN).rotationFactor
        // Berserker slowest, Monk moderate, Magician fastest — and Magician reverses direction.
        assertTrue("Berserker drifts slowest", kotlin.math.abs(b) < kotlin.math.abs(m))
        assertTrue("Magician drifts fastest", kotlin.math.abs(m) < kotlin.math.abs(g))
        assertTrue("Magician reverses direction", g < 0f)
    }

    @Test
    fun `stroke emphasis is heaviest for Berserker and lightest for Magician`() {
        val b = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.BERSERKER).strokeScale
        val m = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MONK).strokeScale
        val g = ClassSigilGeometryCatalog.forVariant(StatusClassVariant.MAGICIAN).strokeScale
        assertTrue(b > m)
        assertTrue(m > g)
    }

    private fun geometry(
        variant: StatusClassVariant,
        classDistinct: Boolean,
        rank: RankTier = RankTier.INITIATE,
    ) = buildOrnateGeometry(
        SigilGeometryKey(
            rank,
            variant,
            medallionCount = 5,
            simplified = false,
            minimal = false,
            classDistinct = classDistinct,
        ),
        radiusPx = 240f,
    )

    @Test
    fun `class-distinct geometry yields a different central silhouette per class`() {
        // At the base rank the central structure is the whole inner geometry, so its path count
        // reflects the distinct silhouette (star=2, hexagram=3, rosette=5).
        val counts = implemented.map { geometry(it, classDistinct = true).innerPaths.size }
        assertEquals("each class draws a distinct central silhouette", counts.size, counts.toSet().size)
    }

    @Test
    fun `the core anchor is present only in the class-distinct geometry`() {
        assertNotNull("class-distinct build has a core anchor", geometry(StatusClassVariant.BERSERKER, classDistinct = true).coreAnchor)
        assertNull("legacy build has no core anchor", geometry(StatusClassVariant.BERSERKER, classDistinct = false).coreAnchor)
    }

    @Test
    fun `the class-distinct build exposes a middle group with a support ring and central structure`() {
        implemented.forEach { variant ->
            val mg = geometry(variant, classDistinct = true).middleGroup
            assertNotNull("$variant has a middle group", mg)
            assertTrue("$variant middle group has central structure", mg!!.central.isNotEmpty())
        }
        assertNull("legacy build has no middle group", geometry(StatusClassVariant.BERSERKER, classDistinct = false).middleGroup)
    }

    @Test
    fun `the core anchor is a separate layer, not part of the rotating middle group`() {
        val geo = geometry(StatusClassVariant.BERSERKER, classDistinct = true)
        assertNotNull(geo.coreAnchor)
        // The anchor Path is not one of the middle group's central paths — it never rotates with them.
        assertTrue(geo.middleGroup!!.central.none { it === geo.coreAnchor })
    }

    @Test
    fun `rank complexity varies independently of class`() {
        // Same class, higher rank -> more rank overlay detail (data-driven), central silhouette unchanged.
        val monkLow = geometry(StatusClassVariant.MONK, classDistinct = true, rank = RankTier.INITIATE)
        val monkHigh = geometry(StatusClassVariant.MONK, classDistinct = true, rank = RankTier.MYTHIC)
        assertTrue("rank raises overlay detail", monkHigh.rankOverlay.size > monkLow.rankOverlay.size)
        assertEquals("class silhouette is rank-invariant", monkLow.middleGroup!!.central.size, monkHigh.middleGroup!!.central.size)

        // Same rank, different class -> same rank overlay count, different central silhouette.
        val berserkerHigh = geometry(StatusClassVariant.BERSERKER, classDistinct = true, rank = RankTier.MYTHIC)
        assertEquals("rank overlay is class-invariant", monkHigh.rankOverlay.size, berserkerHigh.rankOverlay.size)
        assertNotEquals(
            "central silhouette is class-specific",
            monkHigh.middleGroup!!.central.size,
            berserkerHigh.middleGroup!!.central.size,
        )
    }

    @Test
    fun `the legacy geometry is variant-independent so the production screen is unchanged`() {
        // With classDistinct off (the production default) the inner geometry is rank-driven only,
        // identical across classes — nothing about the current screen shifts.
        val b = geometry(StatusClassVariant.BERSERKER, classDistinct = false).innerPaths.size
        val g = geometry(StatusClassVariant.MAGICIAN, classDistinct = false).innerPaths.size
        assertEquals(b, g)
        // And it genuinely differs from the class-distinct build.
        assertNotEquals(geometry(StatusClassVariant.MAGICIAN, classDistinct = true).innerPaths.size, g)
    }
}
