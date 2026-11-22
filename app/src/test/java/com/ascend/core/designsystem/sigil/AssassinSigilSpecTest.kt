package com.ascend.core.designsystem.sigil

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Guards the approved Assassin mark (candidate A "Contained", Nightshade accent, approved 11/21):
 * values must match assassin-sigil/GEOMETRY.md (24-unit art box, centre 12, catalog radius 20.5).
 */
class AssassinSigilSpecTest {
    private val spec = ClassSigilCatalog.Assassin
    private val crescent = spec.geometry.central[0] as SigilShape.Contour
    private val dagger = spec.geometry.central[1] as SigilShape.Contour

    private fun toArt(p: ContourPoint) = Pair(p.x * ART_R + 12f, p.y * ART_R + 12f)

    private fun assertArt(
        x: Float,
        y: Float,
        p: ContourPoint,
    ) {
        val (ax, ay) = toArt(p)
        assertEquals(x, ax, EPS)
        assertEquals(y, ay, EPS)
    }

    @Test
    fun `Nightshade accent, no warm swap, no core anchor`() {
        assertEquals(Color(0xFFD10D8B), spec.palette.accentFor(warm = false))
        assertEquals(Color(0xFFD10D8B), spec.palette.accentFor(warm = true))
        assertNull(spec.geometry.coreAnchor)
        assertEquals(2, spec.geometry.central.size)
    }

    @Test
    fun `dagger never rotates away from pointing down`() {
        assertEquals(0f, spec.motion.rotationFactor)
        assertEquals(1.05f, spec.motion.strokeScale)
    }

    @Test
    fun `crescent matches GEOMETRY md candidate A`() {
        assertArt(4.9584f, 7.7406f, crescent.start)
        val outer = crescent.segments[0] as ContourSegment.Arc
        val inner = crescent.segments[1] as ContourSegment.Arc
        assertArt(12f, 12.85f, outer.center)
        assertEquals(8.70f, outer.radius * ART_R, EPS)
        assertEquals(-144.0349f, outer.startDeg)
        assertEquals(-251.9302f, outer.sweepDeg)
        assertArt(12f, 10.60f, inner.center)
        assertEquals(7.60f, inner.radius * ART_R, EPS)
        assertEquals(-22.1011f, inner.startDeg)
        assertEquals(224.2023f, inner.sweepDeg)
        // The outer arc ends on horn 2 and the inner arc returns to horn 1 (closed, no gap).
        assertOnArcEnd(outer, 19.0416f, 7.7406f)
        assertOnArcEnd(inner, 4.9584f, 7.7406f)
    }

    @Test
    fun `dagger is symmetric, points down, and sits inside the crescent hollow`() {
        assertArt(12f, 16.05f, dagger.start)
        val pommel = dagger.segments.filterIsInstance<ContourSegment.Arc>().single()
        val pommelEnd = ContourPoint(pommel.center.x - pommel.radius, pommel.center.y)
        val lines = dagger.segments.filterIsInstance<ContourSegment.Line>().map { it.to }
        assertEquals(13, lines.size)
        val pts = listOf(dagger.start) + lines + pommelEnd
        // Mirror symmetry about x = 0 (the art box x = 12).
        val xs = pts.map { it.x }.sorted()
        xs.indices.forEach { assertEquals(-xs[it], xs[xs.lastIndex - it], 1e-5f) }
        // Tip is the lowest point (largest +y).
        assertEquals(dagger.start.y, pts.maxOf { it.y })
        // Contained: every dagger vertex is inside the inner (hollow) circle.
        val inner = crescent.segments[1] as ContourSegment.Arc
        pts.forEach { p -> assertTrue(hypot(p.x - inner.center.x, p.y - inner.center.y) < inner.radius) }
        assertArt(12f, 7.45f, pommel.center)
        assertEquals(1.05f, pommel.radius * ART_R, EPS)
        assertEquals(-180f, pommel.sweepDeg)
    }

    private fun assertOnArcEnd(
        arc: ContourSegment.Arc,
        x: Float,
        y: Float,
    ) {
        val end = Math.toRadians((arc.startDeg + arc.sweepDeg).toDouble())
        val (cx, cy) = toArt(arc.center)
        val r = arc.radius * ART_R
        assertEquals(x, (cx + cos(end) * r).toFloat(), 1e-3f)
        assertEquals(y, (cy + sin(end) * r).toFloat(), 1e-3f)
    }

    private companion object {
        const val ART_R = 20.5f
        const val EPS = 1e-4f
    }
}
