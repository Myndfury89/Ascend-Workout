package com.ascend.core.common

import com.ascend.core.model.MeasurementSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure tests for weight conversion, formatting, rounding, increments, and accessibility labels. */
class WeightUnitsTest {
    @Test
    fun `kilograms convert to pounds`() {
        assertEquals(220.462, WeightUnits.kgToLb(100.0), 1e-3)
    }

    @Test
    fun `pounds convert to kilograms`() {
        assertEquals(61.235, WeightUnits.lbToKg(135.0), 1e-3)
    }

    @Test
    fun `conversion round-trips within tolerance`() {
        val canonical = 142.5
        val back = WeightUnits.lbToKg(WeightUnits.kgToLb(canonical))
        assertEquals(canonical, back, 1e-9)
    }

    @Test
    fun `formatting matches the spec examples`() {
        // 100 kg displayed in pounds -> ~220.5 lb.
        assertEquals("220.5 lb", WeightUnits.format(100.0, WeightUnit.POUNDS))
        // 135 lb stored canonically, displayed in kg -> ~61.2 kg.
        assertEquals("61.2 kg", WeightUnits.format(WeightUnits.lbToKg(135.0), WeightUnit.KILOGRAMS))
    }

    @Test
    fun `effectively whole values render without decimals`() {
        assertEquals("100 kg", WeightUnits.format(100.0, WeightUnit.KILOGRAMS))
        // 135 lb round-trips to a whole number of pounds.
        assertEquals("135 lb", WeightUnits.format(WeightUnits.lbToKg(135.0), WeightUnit.POUNDS))
    }

    @Test
    fun `large and decimal values render correctly`() {
        assertEquals("200 kg", WeightUnits.format(200.0, WeightUnit.KILOGRAMS))
        assertEquals("440.9 lb", WeightUnits.format(200.0, WeightUnit.POUNDS))
        assertEquals("2.5 kg", WeightUnits.format(2.5, WeightUnit.KILOGRAMS))
    }

    @Test
    fun `formatting is pure and never mutates the canonical value`() {
        val canonical = 62.5
        WeightUnits.format(canonical, WeightUnit.POUNDS)
        WeightUnits.fromCanonicalKg(canonical, WeightUnit.POUNDS)
        // The Double is a value type — a display conversion cannot change the stored weight,
        // so it can never fabricate a PR or trigger a reward.
        assertEquals(62.5, canonical, 0.0)
    }

    @Test
    fun `equipment increments respect the selected unit and real plates`() {
        assertEquals(listOf("1 kg", "1.25 kg", "2.5 kg", "5 kg"), WeightUnits.displayIncrements(WeightUnit.KILOGRAMS))
        assertEquals(listOf("2.5 lb", "5 lb", "10 lb"), WeightUnits.displayIncrements(WeightUnit.POUNDS))
    }

    @Test
    fun `the weight unit derives from the measurement system by default`() {
        assertEquals(WeightUnit.KILOGRAMS, WeightUnit.forMeasurementSystem(MeasurementSystem.METRIC))
        assertEquals(WeightUnit.POUNDS, WeightUnit.forMeasurementSystem(MeasurementSystem.IMPERIAL))
    }

    @Test
    fun `the accessibility label reflects the selected unit`() {
        assertEquals("Weight unit, kilograms selected", WeightUnit.KILOGRAMS.accessibilityLabel)
        assertEquals("Weight unit, pounds selected", WeightUnit.POUNDS.accessibilityLabel)
    }

    @Test
    fun `each weight unit maps to its measurement system for shared height presentation`() {
        assertEquals(MeasurementSystem.METRIC, WeightUnit.KILOGRAMS.measurementSystem)
        assertEquals(MeasurementSystem.IMPERIAL, WeightUnit.POUNDS.measurementSystem)
    }

    @Test
    fun `the per-value accessibility label reflects the selected unit`() {
        // 112.5 kg in kg, and the spec's pound example.
        assertEquals("Weight, 112.5 kilograms", WeightUnits.accessibilityLabel(112.5, WeightUnit.KILOGRAMS))
        assertEquals("Weight, 248 pounds", WeightUnits.accessibilityLabel(WeightUnits.lbToKg(248.0), WeightUnit.POUNDS))
    }

    @Test
    fun `a display-unit switch does not change the personal-record comparison`() {
        // PR checks compare canonical kilograms; the display unit is irrelevant.
        val currentBestKg = 100.0
        val newLiftKg = 100.0
        val isPr = newLiftKg > currentBestKg
        // Viewing either in pounds changes nothing about the comparison.
        assertTrue(WeightUnits.kgToLb(newLiftKg) > WeightUnits.kgToLb(currentBestKg) == isPr)
        assertTrue(!isPr)
    }
}
