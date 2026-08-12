package com.ascend.feature.ascended

import com.ascend.feature.ascended.model.AscendedClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The class id -> production avatar mapping: only berserker/monk/mage; everything else -> base. */
class YourAscendedMappingTest {
    @Test
    fun `production classes map to their avatar`() {
        assertEquals(AscendedClass.BERSERKER, productionAvatarClassOf("berserker"))
        assertEquals(AscendedClass.MONK, productionAvatarClassOf("monk"))
        assertEquals(AscendedClass.MAGE, productionAvatarClassOf("mage"))
    }

    @Test
    fun `unbound resolves to the neutral base figure`() {
        assertNull(productionAvatarClassOf(null))
    }

    @Test
    fun `not-yet-shipped and unknown classes resolve to the neutral base figure`() {
        assertNull(productionAvatarClassOf("assassin"))
        assertNull(productionAvatarClassOf("fighter"))
        assertNull(productionAvatarClassOf("ranger"))
        assertNull(productionAvatarClassOf("guardian"))
        assertNull(productionAvatarClassOf("magician")) // retired id must not map after the rename
        assertNull(productionAvatarClassOf("totally-unknown"))
    }
}
