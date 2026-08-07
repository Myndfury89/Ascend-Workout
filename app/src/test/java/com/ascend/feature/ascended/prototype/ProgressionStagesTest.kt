package com.ascend.feature.ascended.prototype

import com.ascend.feature.ascended.prototype.body.ClassSilhouetteGeometry
import com.ascend.feature.ascended.prototype.body.SilhouetteFidelity
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cumulative evolution stages, equipment gating, stage aura, and Perception manifestation (pure). */
class ProgressionStagesTest {
    private val majorEquipment =
        setOf(
            "axeHandle", "axeHead", "axeEdge", "shield", "shieldRim", "shieldBoss",
            "staffShaft", "staffHead", "staffCore", "swordBlade", "bow", "bladeR", "bladeL", "focus", "focusRing",
        )

    private fun refined(
        cls: AscendedClass,
        stage: EvolutionStage,
        base: BodyBase = BodyBase.MALE,
    ) = ClassSilhouetteGeometry.build(cls, base, SilhouetteFidelity.REFINED, stage).shapes

    @Test
    fun `stages are cumulative — each class grows from Base to Mastered`() {
        AscendedClass.entries.forEach { cls ->
            val counts = EvolutionStage.entries.map { refined(cls, it).size }
            assertEquals("$cls stage counts must be non-decreasing", counts.sorted(), counts)
            assertTrue("$cls Base ($counts) must be simpler than Mastered", counts.first() < counts.last())
        }
    }

    @Test
    fun `the base stage carries no major weapon, shield, or staff`() {
        AscendedClass.entries.forEach { cls ->
            val baseNames = refined(cls, EvolutionStage.BASE).map { it.name }.toSet()
            val leaked = baseNames.intersect(majorEquipment)
            assertTrue("$cls Base leaked equipment: $leaked", leaked.isEmpty())
        }
    }

    @Test
    fun `the blockout figure is unaffected by the stage filter`() {
        AscendedClass.entries.forEach { cls ->
            val atBase = ClassSilhouetteGeometry.build(cls, BodyBase.MALE, SilhouetteFidelity.BLOCKOUT, EvolutionStage.BASE).shapes.size
            val atMastered =
                ClassSilhouetteGeometry.build(
                    cls,
                    BodyBase.MALE,
                    SilhouetteFidelity.BLOCKOUT,
                    EvolutionStage.MASTERED,
                ).shapes.size
            assertEquals("$cls blockout must ignore stage", atBase, atMastered)
        }
    }

    @Test
    fun `the aura grows with the stage`() {
        assertTrue(ClassSilhouetteGeometry.auraShapes(EvolutionStage.BASE).isEmpty())
        assertTrue(
            ClassSilhouetteGeometry.auraShapes(EvolutionStage.EARLY_GROWTH).size <
                ClassSilhouetteGeometry.auraShapes(EvolutionStage.MASTERED).size,
        )
        assertEquals(3, ClassSilhouetteGeometry.auraShapes(EvolutionStage.MASTERED).size)
    }

    @Test
    fun `the Perception manifestation strengthens from L1 to L10`() {
        val cls = AscendedClass.GUARDIAN
        assertTrue("off is silent", ClassSilhouetteGeometry.perceptionShapes(0, cls).isEmpty())
        val l1 = ClassSilhouetteGeometry.perceptionShapes(1, cls).size
        val l5 = ClassSilhouetteGeometry.perceptionShapes(5, cls).size
        val l10 = ClassSilhouetteGeometry.perceptionShapes(10, cls).size
        assertTrue("L1 < L5", l1 < l5)
        assertTrue("L5 < L10", l5 < l10)
    }
}
