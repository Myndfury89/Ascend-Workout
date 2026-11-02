package com.ascend.core.designsystem.sigil

import com.ascend.core.designsystem.motion.MotionVerb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SigilStateTest {
    @Test
    fun `states map to the agreed motion verbs`() {
        assertNull(SigilState.Idle.verb)
        assertEquals(MotionVerb.LOCK, SigilState.Selected.verb)
        assertEquals(MotionVerb.CHARGE, SigilState.Charging.verb)
        assertEquals(MotionVerb.ASCEND, SigilState.LevelUp.verb)
        assertEquals(MotionVerb.ASSEMBLE, SigilState.Reveal.verb)
        assertNull(SigilState.Locked.verb)
    }

    @Test
    fun `reduced motion stops every loop`() {
        SigilState.entries.forEach { assertFalse("$it drifts under reduced motion", it.drifts(reducedMotion = true)) }
    }

    @Test
    fun `only Locked is static with full motion`() {
        SigilState.entries.forEach { assertEquals(it != SigilState.Locked, it.drifts(reducedMotion = false)) }
    }

    @Test
    fun `locked is dimmed and desaturated, every other state is full strength`() {
        val locked = SigilState.Locked.settledFrame()
        assertEquals(LOCKED_ALPHA, locked.alpha)
        assertEquals(LOCKED_SATURATION, locked.saturation)
        (SigilState.entries - SigilState.Locked).forEach {
            assertEquals(1f, it.settledFrame().alpha)
            assertEquals(1f, it.settledFrame().saturation)
        }
    }

    @Test
    fun `every state settles fully assembled with no wave`() {
        SigilState.entries.forEach {
            assertEquals(1f, it.settledFrame().assembly)
            assertEquals(0f, it.settledFrame().wave)
        }
    }

    @Test
    fun `verbs start from a distinct frame and stateless changes do not`() {
        assertEquals(0f, SigilState.Reveal.entryFrame().assembly)
        assertEquals(1f, SigilState.Selected.entryFrame().glow)
        assertTrue(SigilState.LevelUp.entryFrame().newRankLayer && SigilState.LevelUp.settledFrame().newRankLayer)
        assertEquals(SigilState.Idle.settledFrame(), SigilState.Idle.entryFrame())
        assertEquals(SigilState.Locked.settledFrame(), SigilState.Locked.entryFrame())
    }

    @Test
    fun `charging holds glow`() {
        assertEquals(CHARGE_GLOW, SigilState.Charging.settledFrame().glow)
        assertEquals(0f, SigilState.Idle.settledFrame().glow)
    }

    @Test
    fun `catalog carries the original motion profile values`() {
        assertEquals(ClassMotionProfile(0.77f, 1.2f), ClassSigilCatalog.Berserker.motion)
        assertEquals(ClassMotionProfile(0.95f, 1.0f), ClassSigilCatalog.Monk.motion)
        assertEquals(ClassMotionProfile(-1.18f, 0.85f), ClassSigilCatalog.Mage.motion)
        assertEquals(ClassMotionProfile(1.0f, 1.0f), ClassSigilCatalog.Neutral.motion)
    }
}
