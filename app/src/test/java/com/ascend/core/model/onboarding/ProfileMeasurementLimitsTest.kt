package com.ascend.core.model.onboarding

import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The onboarding measurement ranges live in configuration, not in the composables. */
class ProfileMeasurementLimitsTest {
    private val limits = ProfileMeasurementLimits.DEFAULT

    @Test
    fun `default canonical ranges match the suggested guidance`() {
        assertEquals(90.0, limits.heightCmMin, 0.0)
        assertEquals(250.0, limits.heightCmMax, 0.0)
        assertEquals(30.0, limits.weightKgMin, 0.0)
        assertEquals(300.0, limits.weightKgMax, 0.0)
    }

    @Test
    fun `height validation accepts in-range and neutrally rejects out-of-range values`() {
        assertTrue(limits.isHeightValid(188.0))
        assertFalse(limits.isHeightValid(60.0))
        assertFalse(limits.isHeightValid(300.0))
    }

    @Test
    fun `weight validation accepts in-range and neutrally rejects out-of-range values`() {
        assertTrue(limits.isWeightValid(112.5))
        assertFalse(limits.isWeightValid(10.0))
        assertFalse(limits.isWeightValid(500.0))
    }

    @Test
    fun `imperial feet options derive from the canonical centimetre bounds`() {
        // 90 cm -> 2 ft floor; 250 cm -> 8 ft floor.
        assertEquals(2, limits.heightFeetOptions.first)
        assertEquals(8, limits.heightFeetOptions.last)
    }

    @Test
    fun `metric height options span the canonical range inclusively`() {
        assertEquals(90, limits.heightCmOptions.first)
        assertEquals(250, limits.heightCmOptions.last)
    }

    @Test
    fun `kilogram picker options are canonical and stay within range`() {
        val opts = limits.weightOptionsKg(WeightUnit.KILOGRAMS)
        assertEquals(30.0, opts.first(), 0.0)
        assertTrue("last option within range", opts.last() <= 300.0 + 1e-6)
        assertTrue("supports a half-kg step", opts.contains(30.5))
    }

    @Test
    fun `pound picker options are stored canonically in kilograms`() {
        val opts = limits.weightOptionsKg(WeightUnit.POUNDS)
        // Each option is a canonical-kg value derived from a whole pound reading.
        val firstLb = WeightUnits.kgToLb(opts.first())
        assertEquals(firstLb, Math.round(firstLb).toDouble(), 1e-6)
        assertTrue("stays inside the canonical range", opts.all { it in 30.0..300.0 })
    }

    @Test
    fun `a custom configuration overrides the defaults without code changes`() {
        val custom = ProfileMeasurementLimits(heightCmMin = 100.0, weightKgMax = 250.0)
        assertFalse(custom.isHeightValid(95.0))
        assertFalse(custom.isWeightValid(280.0))
    }
}
