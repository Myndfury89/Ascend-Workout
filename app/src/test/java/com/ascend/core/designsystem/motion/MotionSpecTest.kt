package com.ascend.core.designsystem.motion

import org.junit.Assert.assertEquals
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
}
