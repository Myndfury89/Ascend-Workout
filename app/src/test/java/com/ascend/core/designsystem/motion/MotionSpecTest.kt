package com.ascend.core.designsystem.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionSpecTest {
    @Test
    fun `full motion preserves token durations and stagger`() {
        val motion = MotionSpec(reducedMotion = false)
        assertEquals(AscendMotionTokens.STANDARD, motion.duration(AscendMotionTokens.STANDARD))
        assertEquals(AscendMotionTokens.DRAMATIC, motion.duration(AscendMotionTokens.DRAMATIC))
        assertEquals(3 * AscendMotionTokens.STAGGER, motion.stagger(3))
    }

    @Test
    fun `reduced motion collapses every duration and stagger to zero`() {
        val motion = MotionSpec(reducedMotion = true)
        assertEquals(0, motion.duration(AscendMotionTokens.STANDARD))
        assertEquals(0, motion.duration(AscendMotionTokens.DRAMATIC))
        assertEquals(0, motion.stagger(4))
    }

    @Test
    fun `the four ceremonial verbs carry the handoff durations`() {
        assertEquals(850, MotionVerb.ASSEMBLE.durationMs)
        assertEquals(1100, MotionVerb.CHARGE.durationMs)
        assertEquals(380, MotionVerb.LOCK.durationMs)
        assertEquals(2200, MotionVerb.ASCEND.durationMs)
        // Reduced motion is a short linear cross-fade, never a hard cut.
        assertEquals(140, AscendVerb.REDUCED_MS)
        assertEquals(320, AscendVerb.RECOGNITION_CUE_MS)
    }

    @Test
    fun `each verb has a distinct easing`() {
        val easings = MotionVerb.entries.map { it.easing }
        assertEquals("verbs must not share an easing", easings.size, easings.toSet().size)
    }

    @Test
    fun `motion tiers order their duration windows and name a reference verb`() {
        assertEquals(MotionVerb.LOCK, MotionTier.EVERYDAY.referenceVerb)
        assertEquals(MotionVerb.CHARGE, MotionTier.REWARD.referenceVerb)
        assertEquals(MotionVerb.ASCEND, MotionTier.ASCENSION.referenceVerb)
        MotionTier.entries.zipWithNext().forEach { (lower, higher) ->
            assertTrue("tier windows ascend", higher.minMs >= lower.maxMs - 1)
        }
    }

    @Test
    fun `verb speed profile matches the handoff multipliers`() {
        assertEquals(0.55f, VerbSpeed.FAST.scale)
        assertEquals(1f, VerbSpeed.STANDARD.scale)
        assertEquals(1.6f, VerbSpeed.CINEMATIC.scale)
    }
}
