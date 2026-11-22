package com.ascend.core.designsystem.sigil

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

private fun ContourPoint.inUnitDisc() = hypot(x, y) <= 1f

private fun ContourSegment.inUnitDisc() =
    when (this) {
        is ContourSegment.Line -> to.inUnitDisc()
        is ContourSegment.Arc -> radius > 0f && hypot(center.x, center.y) + radius <= 1f
    }

private fun SigilShape.Contour.isInsideUnitDisc(): Boolean {
    val segmentsInside = segments.all { it.inUnitDisc() }
    return segments.isNotEmpty() && start.inUnitDisc() && segmentsInside
}

class ClassSigilCatalogTest {
    @Test
    fun `catalog has exactly one spec per class key`() {
        assertEquals(ClassKey.entries.toSet(), ClassSigilCatalog.all.map { it.key }.toSet())
        assertEquals(ClassKey.entries.size, ClassSigilCatalog.all.size)
        ClassKey.entries.forEach { assertEquals(it, ClassSigilCatalog.of(it).key) }
    }

    @Test
    fun `every shape stays inside the sigil radius and is well formed`() {
        ClassSigilCatalog.all
            .flatMap { it.geometry.central + listOfNotNull(it.geometry.coreAnchor) }
            .forEach(::assertWellFormed)
    }

    private fun assertWellFormed(shape: SigilShape) {
        when (shape) {
            is SigilShape.Polygon -> assertTrue(shape.radius in 0f..1f && (shape.sides ?: 3) >= 3)
            is SigilShape.Star ->
                assertTrue(shape.inner < shape.outer && shape.outer <= 1f && (shape.points ?: 3) >= 3)
            is SigilShape.Circle -> assertTrue(shape.radius in 0f..1f)
            is SigilShape.Rosette -> assertTrue(shape.petals > 0 && shape.offset + shape.petal <= 1f)
            is SigilShape.Chevron -> assertChevronWellFormed(shape)
            is SigilShape.Shield -> assertShieldWellFormed(shape)
            is SigilShape.Contour -> assertTrue(shape.isInsideUnitDisc())
        }
    }

    private fun assertChevronWellFormed(shape: SigilShape.Chevron) =
        assertTrue(shape.width > 0f && shape.depth in 0f..shape.tipRadius && shape.tipRadius <= 1f)

    private fun assertShieldWellFormed(shape: SigilShape.Shield) {
        assertTrue(shape.width in 0f..1f && shape.height in 0f..1f)
    }

    @Test
    fun `core anchors are small and every class has a central structure`() {
        ClassSigilCatalog.all.forEach { spec ->
            assertTrue(spec.geometry.central.isNotEmpty())
            val anchor = spec.geometry.coreAnchor ?: return@forEach // Assassin: approved mark has no core.
            val size =
                when (anchor) {
                    is SigilShape.Polygon -> anchor.radius
                    is SigilShape.Star -> anchor.outer
                    is SigilShape.Circle -> anchor.radius
                    is SigilShape.Rosette -> anchor.offset + anchor.petal
                    is SigilShape.Chevron -> anchor.tipRadius
                    is SigilShape.Shield -> maxOf(anchor.width, anchor.height) / 2f
                    is SigilShape.Contour -> error("${spec.key}: a contour is not a core anchor")
                }
            assertTrue("${spec.key} anchor too large", size <= 0.12f)
        }
    }

    @Test
    fun `class silhouettes are distinct`() {
        assertEquals(ClassKey.entries.size, ClassSigilCatalog.all.map { it.geometry }.toSet().size)
    }

    @Test
    fun `values match the original inline renderer`() {
        assertEquals(SigilShape.Star(5, 0.38f, 0.10f), ClassSigilCatalog.Berserker.geometry.central[0])
        assertEquals(SigilShape.Polygon(5, 0.14f, -54f), ClassSigilCatalog.Berserker.geometry.central[1])
        assertEquals(SigilShape.Star(4, 0.11f, 0.028f), ClassSigilCatalog.Berserker.geometry.coreAnchor)
        assertEquals(SigilShape.Polygon(6, 0.085f), ClassSigilCatalog.Monk.geometry.coreAnchor)
        assertEquals(SigilShape.Rosette(6, 0.17f, 0.17f), ClassSigilCatalog.Mage.geometry.central[0])
        assertEquals(SigilShape.Circle(0.075f), ClassSigilCatalog.Mage.geometry.coreAnchor)
        assertEquals(SigilShape.Circle(0.05f), ClassSigilCatalog.Neutral.geometry.coreAnchor)
    }

    @Test
    fun `fighter ranger guardian match the approved A geometry`() {
        assertEquals(
            listOf(SigilShape.Polygon(8, 0.30f), SigilShape.Star(8, 0.34f, 0.24f)),
            ClassSigilCatalog.Fighter.geometry.central,
        )
        assertEquals(SigilShape.Polygon(8, 0.08f), ClassSigilCatalog.Fighter.geometry.coreAnchor)
        assertEquals(
            listOf(-90f, 30f, 150f).map { SigilShape.Chevron(0.30f, 0.18f, 0.34f, it) } + SigilShape.Polygon(3, 0.12f),
            ClassSigilCatalog.Ranger.geometry.central,
        )
        assertEquals(SigilShape.Chevron(0.08f, 0.05f, 0.055f), ClassSigilCatalog.Ranger.geometry.coreAnchor)
        assertEquals(
            listOf(SigilShape.Shield(0.42f, 0.50f), SigilShape.Polygon(6, 0.20f)),
            ClassSigilCatalog.Guardian.geometry.central,
        )
        assertEquals(SigilShape.Circle(0.07f), ClassSigilCatalog.Guardian.geometry.coreAnchor)
    }

    @Test
    fun `fighter ranger guardian use the approved accents with no warm variant`() {
        assertEquals(Color(0xFFB7C5D8), ClassSigilCatalog.Fighter.palette.accent)
        assertEquals(Color(0xFF5ECF9A), ClassSigilCatalog.Ranger.palette.accent)
        assertEquals(Color(0xFFE0C36A), ClassSigilCatalog.Guardian.palette.accent)
        listOf(ClassSigilCatalog.Fighter, ClassSigilCatalog.Ranger, ClassSigilCatalog.Guardian).forEach {
            assertEquals(it.palette.accent, it.palette.accentFor(warm = true))
        }
    }

    @Test
    fun `warm accent applies only where defined`() {
        val p = SigilPalette(accent = Color.Red, warmAccent = Color.Blue)
        assertEquals(Color.Red, p.accentFor(warm = false))
        assertEquals(Color.Blue, p.accentFor(warm = true))
        assertEquals(Color.Green, SigilPalette(Color.Green).accentFor(warm = true))
    }
}
