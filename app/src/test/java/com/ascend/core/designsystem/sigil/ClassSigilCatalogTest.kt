package com.ascend.core.designsystem.sigil

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassSigilCatalogTest {
    @Test
    fun `catalog has exactly one spec per class key`() {
        assertEquals(ClassKey.entries.toSet(), ClassSigilCatalog.all.map { it.key }.toSet())
        assertEquals(ClassKey.entries.size, ClassSigilCatalog.all.size)
        ClassKey.entries.forEach { assertEquals(it, ClassSigilCatalog.of(it).key) }
    }

    @Test
    fun `every shape stays inside the sigil radius and is well formed`() {
        ClassSigilCatalog.all.flatMap { it.geometry.central + it.geometry.coreAnchor }.forEach { shape ->
            when (shape) {
                is SigilShape.Polygon -> assertTrue(shape.radius in 0f..1f && (shape.sides ?: 3) >= 3)
                is SigilShape.Star ->
                    assertTrue(shape.inner < shape.outer && shape.outer <= 1f && (shape.points ?: 3) >= 3)
                is SigilShape.Circle -> assertTrue(shape.radius in 0f..1f)
                is SigilShape.Rosette -> assertTrue(shape.petals > 0 && shape.offset + shape.petal <= 1f)
            }
        }
    }

    @Test
    fun `core anchors are small and every class has a central structure`() {
        ClassSigilCatalog.all.forEach { spec ->
            assertTrue(spec.geometry.central.isNotEmpty())
            val anchor = spec.geometry.coreAnchor
            val size =
                when (anchor) {
                    is SigilShape.Polygon -> anchor.radius
                    is SigilShape.Star -> anchor.outer
                    is SigilShape.Circle -> anchor.radius
                    is SigilShape.Rosette -> anchor.offset + anchor.petal
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
    fun `warm accent applies only where defined`() {
        val p = SigilPalette(accent = Color.Red, warmAccent = Color.Blue)
        assertEquals(Color.Red, p.accentFor(warm = false))
        assertEquals(Color.Blue, p.accentFor(warm = true))
        assertEquals(Color.Green, SigilPalette(Color.Green).accentFor(warm = true))
    }
}
