package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.geometry.Offset
import com.ascend.core.designsystem.sigil.ClassKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * The Chevron and Shield primitives (PR 10b) as pure point math. Expected points are the approved
 * Ranger A / Guardian A reference coordinates: a 24 dp viewport, sigil radius 20.5, centre (12, 12).
 */
class SigilPrimitiveGeometryTest {
    private val r = 20.5f
    private val centre = Offset(12f, 12f)

    private fun List<Offset>.inViewport() = map { it + centre }

    private fun assertPoints(
        expected: List<Offset>,
        actual: List<Offset>,
    ) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (e, a) ->
            assertEquals("x of $a", e.x, a.x, TOL)
            assertEquals("y of $a", e.y, a.y, TOL)
        }
    }

    @Test
    fun `chevron points match the approved Ranger A arms`() {
        fun arm(deg: Float) = chevronPoints(r * 0.30f, r * 0.18f, r * 0.34f, deg).inViewport()
        assertPoints(listOf(Offset(15.0750f, 8.7200f), Offset(12.0000f, 5.0300f), Offset(8.9250f, 8.7200f)), arm(-90f))
        assertPoints(
            listOf(Offset(13.3031f, 16.3030f), Offset(18.0362f, 15.4850f), Offset(16.3781f, 10.9770f)),
            arm(30f),
        )
        assertPoints(
            listOf(Offset(7.6219f, 10.9770f), Offset(5.9638f, 15.4850f), Offset(10.6969f, 16.3030f)),
            arm(150f),
        )
    }

    @Test
    fun `chevron core matches the approved Ranger A core`() {
        val core = chevronPoints(r * 0.08f, r * 0.05f, r * 0.055f, -90f).inViewport()
        assertPoints(listOf(Offset(12.8200f, 11.8975f), Offset(12.0000f, 10.8725f), Offset(11.1800f, 11.8975f)), core)
    }

    @Test
    fun `chevron tip sits at tipRadius and arms are symmetric about the axis`() {
        val (a, tip, b) = chevronPoints(width = 6f, depth = 2f, tipRadius = 10f, rotationDeg = 17f)
        assertEquals(10f, hypot(tip.x, tip.y), TOL)
        assertEquals(hypot(a.x - tip.x, a.y - tip.y), hypot(b.x - tip.x, b.y - tip.y), TOL)
        assertEquals(6f, hypot(a.x - b.x, a.y - b.y), TOL)
    }

    @Test
    fun `shield points match the approved Guardian A outline`() {
        val shield = shieldPoints(r * 0.42f, r * 0.50f).inViewport()
        assertPoints(
            listOf(
                Offset(8.0394f, 6.8750f),
                Offset(15.9606f, 6.8750f),
                Offset(16.3050f, 12.5125f),
                Offset(12.0000f, 17.1250f),
                Offset(7.6950f, 12.5125f),
            ),
            shield,
        )
    }

    @Test
    fun `shield is mirror symmetric with a flat top and a single bottom point`() {
        val p = shieldPoints(width = 10f, height = 12f)
        assertEquals(p[0].y, p[1].y, TOL)
        assertEquals(-p[0].x, p[1].x, TOL)
        assertEquals(-p[4].x, p[2].x, TOL)
        assertEquals(0f, p[3].x, TOL)
        assertEquals(6f, p[3].y, TOL)
        assertTrue("waist is wider than the top", p[2].x > p[1].x)
    }

    @Test
    fun `new classes build class geometry with one path per central shape and a core anchor`() {
        mapOf(ClassKey.FIGHTER to 2, ClassKey.RANGER to 4, ClassKey.GUARDIAN to 2).forEach { (key, centralCount) ->
            val geo =
                buildOrnateGeometry(
                    SigilGeometryKey(
                        rankTier = RankTier.GOLD,
                        variant = key.statusVariant(),
                        medallionCount = 5,
                        simplified = false,
                        minimal = false,
                        classDistinct = true,
                        classKey = key,
                    ),
                    radiusPx = 200f,
                )
            assertEquals("$key central", centralCount, geo.middleGroup!!.central.size)
            assertTrue("$key core anchor", geo.coreAnchor != null)
        }
    }

    @Test
    fun `new classes use the shared line style and no proficiency glyph yet`() {
        listOf(ClassKey.FIGHTER, ClassKey.RANGER, ClassKey.GUARDIAN).forEach {
            val style = SigilClassStyleCatalog.styleFor(it)
            assertEquals(SigilClassStyleCatalog.styleFor(StatusClassVariant.NEUTRAL), style)
            assertNull(style.proficiencyGlyph)
        }
    }

    @Test
    fun `existing variants keep their own class key and style`() {
        StatusClassVariant.entries.forEach { v ->
            val state = OrnateSigilState(RankTier.GOLD, v, 1f, null, -1, showProficiency = false)
            assertEquals(v.classKey(), state.classKey)
            assertEquals(SigilClassStyleCatalog.styleFor(v), SigilClassStyleCatalog.styleFor(state.classKey))
        }
    }

    private companion object {
        const val TOL = 1e-3f
    }
}
