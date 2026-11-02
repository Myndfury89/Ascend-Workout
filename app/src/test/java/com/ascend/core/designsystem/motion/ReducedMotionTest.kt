package com.ascend.core.designsystem.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReducedMotionTest {
    @Test
    fun `animator scale of zero means reduced motion`() {
        assertTrue(isReducedMotionScale(0f))
        assertFalse(isReducedMotionScale(0.5f))
        assertFalse(isReducedMotionScale(DEFAULT_ANIMATOR_DURATION_SCALE))
        assertFalse(isReducedMotionScale(10f))
    }

    @Test
    fun `either source turns reduced motion on`() {
        assertFalse(effectiveReducedMotion(system = false, inApp = false))
        assertTrue(effectiveReducedMotion(system = true, inApp = false))
        assertTrue(effectiveReducedMotion(system = false, inApp = true))
        assertTrue(effectiveReducedMotion(system = true, inApp = true))
    }
}
