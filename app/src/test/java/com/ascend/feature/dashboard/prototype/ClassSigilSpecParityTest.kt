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
        assertEquals(ClassKey.entries.size, StatusClassVariant.entries.map { it.classKey() }.toSet().size)
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
