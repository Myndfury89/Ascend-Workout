package com.ascend.feature.dashboard.prototype

import org.junit.Assert.assertEquals
import org.junit.Test

/** The refined-prototype warm treatment recolours Berserker only; production accents are untouched. */
class ClassAccentTest {
    @Test
    fun `warm mode makes Berserker a red-orange ember`() {
        assertEquals(StatusPalette.ember, classAccent(StatusClassVariant.BERSERKER, warm = true))
    }

    @Test
    fun `without warm mode Berserker keeps its production accent`() {
        assertEquals(StatusSigilVariant.BERSERKER.core, classAccent(StatusClassVariant.BERSERKER, warm = false))
    }

    @Test
    fun `Monk and Mage are unchanged even in warm mode`() {
        assertEquals(StatusSigilVariant.MONK.core, classAccent(StatusClassVariant.MONK, warm = true))
        assertEquals(StatusSigilVariant.MAGE.core, classAccent(StatusClassVariant.MAGE, warm = true))
        assertEquals(StatusSigilVariant.NEUTRAL.core, classAccent(StatusClassVariant.NEUTRAL, warm = true))
    }
}
