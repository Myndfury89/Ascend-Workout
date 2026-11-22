package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.sigil.ClassKey
import com.ascend.core.designsystem.sigil.ClassSigilCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

/** Guards that the data catalog reproduces the pre-refactor `accentBlend` colour choices exactly. */
class ClassSigilSpecParityTest {
    @Test
    fun `every variant maps to the class key of the same name`() {
        StatusClassVariant.entries.forEach { assertEquals(it.name, it.classKey().name) }
        assertEquals(StatusClassVariant.entries.size, StatusClassVariant.entries.map { it.classKey() }.toSet().size)
        StatusClassVariant.entries.forEach { assertEquals(it, it.classKey().prototypeVariantOrNull()) }
    }

    @Test
    fun `classes without a prototype variant ride on neutral but keep their own key`() {
        val withoutVariant = ClassKey.entries.filter { it.prototypeVariantOrNull() == null }
        assertEquals(listOf(ClassKey.FIGHTER, ClassKey.RANGER, ClassKey.GUARDIAN), withoutVariant)
        withoutVariant.forEach { assertEquals(StatusClassVariant.NEUTRAL, it.statusVariant()) }
    }

    @Test
    fun `palette matches the original accentBlend`() {
        val c = ClassSigilCatalog
        assertEquals(StatusPalette.ember, c.Berserker.palette.accentFor(warm = true))
        assertEquals(StatusPalette.violetBright, c.Berserker.palette.accentFor(warm = false))
        assertEquals(StatusPalette.cyanSoft, c.Monk.palette.accentFor(warm = true))
        assertEquals(StatusPalette.cyanSoft, c.Monk.palette.accentFor(warm = false))
        assertEquals(StatusPalette.violet, c.Mage.palette.accentFor(warm = true))
        assertEquals(StatusPalette.violet, c.Mage.palette.accentFor(warm = false))
        assertEquals(StatusPalette.infoLine, c.Neutral.palette.accentFor(warm = true))
        assertEquals(StatusPalette.infoLine, c.Neutral.palette.accentFor(warm = false))
    }
}
