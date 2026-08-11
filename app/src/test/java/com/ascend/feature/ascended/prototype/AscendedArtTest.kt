package com.ascend.feature.ascended.prototype

import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The figure-art naming convention that maps class + body base (+ stage) to a drawable name. */
class AscendedArtTest {
    @Test
    fun `the base figure name is ascended underscore class underscore base`() {
        assertEquals("ascended_guardian_male", AscendedArt.figureResourceName(AscendedClass.GUARDIAN, BodyBase.MALE))
        assertEquals("ascended_monk_female", AscendedArt.figureResourceName(AscendedClass.MONK, BodyBase.FEMALE))
    }

    @Test
    fun `the base stage carries no suffix, later stages do`() {
        val stem = AscendedArt.figureResourceName(AscendedClass.MAGE, BodyBase.FEMALE)
        assertEquals(stem, AscendedArt.figureResourceName(AscendedClass.MAGE, BodyBase.FEMALE, EvolutionStage.BASE))
        assertEquals("${stem}_mastered", AscendedArt.figureResourceName(AscendedClass.MAGE, BodyBase.FEMALE, EvolutionStage.MASTERED))
        assertEquals("${stem}_early", AscendedArt.figureResourceName(AscendedClass.MAGE, BodyBase.FEMALE, EvolutionStage.EARLY_GROWTH))
    }

    @Test
    fun `the neutral base-body name is ascended_base_base`() {
        assertEquals("ascended_base_male", AscendedArt.baseBodyResourceName(BodyBase.MALE))
        assertEquals("ascended_base_female", AscendedArt.baseBodyResourceName(BodyBase.FEMALE))
    }

    @Test
    fun `every class and body base yields a unique, resource-safe base name`() {
        val names =
            AscendedClass.entries.flatMap { cls ->
                BodyBase.entries.map { AscendedArt.figureResourceName(cls, it) }
            }
        assertEquals("all 14 names unique", names.size, names.toSet().size)
        names.forEach { name ->
            assertTrue("$name must be lowercase resource-safe", name.matches(Regex("[a-z0-9_]+")))
        }
    }
}
