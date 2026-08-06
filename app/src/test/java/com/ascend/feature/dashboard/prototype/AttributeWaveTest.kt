package com.ascend.feature.dashboard.prototype

import com.ascend.core.designsystem.motion.AscendVerbEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** CP3: each attribute's center-out wave has its own personality (duration + easing). */
class AttributeWaveTest {
    @Test
    fun `the handoff-pinned attribute waves have the exact timings`() {
        assertEquals("Strength — firmer/faster", 420, AttributeWaveCatalog.forIndex(0).durationMs)
        assertEquals("Endurance — longer sustained", 950, AttributeWaveCatalog.forIndex(1).durationMs)
        assertEquals("Discipline — precise clean", 520, AttributeWaveCatalog.forIndex(3).durationMs)
    }

    @Test
    fun `each attribute wave uses the character easing`() {
        assertEquals(AscendVerbEasing.lock, AttributeWaveCatalog.forIndex(0).easing) // snap-overshoot
        assertEquals(AscendVerbEasing.charge, AttributeWaveCatalog.forIndex(1).easing) // ease-in-out
        assertEquals(AscendVerbEasing.assemble, AttributeWaveCatalog.forIndex(3).easing) // expo-decelerate
    }

    @Test
    fun `Strength resolves faster than the sustained Endurance wave`() {
        assertTrue(AttributeWaveCatalog.forIndex(0).durationMs < AttributeWaveCatalog.forIndex(1).durationMs)
    }

    @Test
    fun `every attribute role maps to a profile and the wave reaches into the ring structure`() {
        MedallionRole.entries.filter { it != MedallionRole.PROFICIENCY }.forEach {
            assertTrue("${it.name} has a wave", AttributeWaveCatalog.forRole(it).durationMs > 0)
        }
        assertTrue(AttributeWaveCatalog.REACH_FRACTION in 0f..1f)
    }
}
