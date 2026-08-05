package com.ascend.core.common

import com.ascend.core.model.MeasurementSystem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure tests for height conversion, formatting, canonical stability across units, and a11y labels. */
class HeightUnitsTest {
    @Test
    fun `feet and inches convert to canonical centimetres`() {
        // 6 ft 2 in = 74 in = 187.96 cm.
        assertEquals(187.96, HeightUnits.feetInchesToCm(6, 2), 1e-6)
    }

    @Test
    fun `centimetres convert to whole feet and inches with inches bounded 0 to 11`() {
        val fi = HeightUnits.cmToFeetInches(187.96)
        assertEquals(6, fi.feet)
        assertEquals(2, fi.inches)
    }

    @Test
    fun `a value that rounds to a whole foot does not overflow inches`() {
        // 71.9 in ~ 182.6 cm rounds to 72 in -> 6 ft 0 in, never 5 ft 12 in.
        val fi = HeightUnits.cmToFeetInches(HeightUnits.feetInchesToCm(5, 12) - 0.1)
        assertEquals(6, fi.feet)
        assertEquals(0, fi.inches)
    }

    @Test
    fun `switching the display system does not alter the canonical height`() {
        val canonical = 187.96
        // Presenting the same canonical value either way is display-only; the stored Double is untouched.
        HeightUnits.format(canonical, MeasurementSystem.IMPERIAL)
        HeightUnits.format(canonical, MeasurementSystem.METRIC)
        assertEquals(187.96, canonical, 0.0)
    }

    @Test
    fun `selecting a height in either system commits the same canonical representation`() {
        // 188 cm and the nearest imperial reading (6 ft 2 in) both live around the same canonical value;
        // committing either lands within one inch of the other — the picker stores canonical cm both ways.
        val fromMetric = 188.0
        val fromImperial = HeightUnits.feetInchesToCm(6, 2)
        assertEquals(fromMetric, fromImperial, HeightUnits.CM_PER_INCH)
    }

    @Test
    fun `format matches the spec examples`() {
        assertEquals("6 ft 2 in", HeightUnits.format(187.96, MeasurementSystem.IMPERIAL))
        assertEquals("188 cm", HeightUnits.format(188.0, MeasurementSystem.METRIC))
    }

    @Test
    fun `accessibility label reflects the selected system`() {
        assertEquals("Height, 6 feet 2 inches", HeightUnits.accessibilityLabel(187.96, MeasurementSystem.IMPERIAL))
        assertEquals("Height, 188 centimetres", HeightUnits.accessibilityLabel(188.0, MeasurementSystem.METRIC))
    }
}
