package com.ascend.core.model.onboarding

import com.ascend.core.common.HeightUnits
import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Configurable validation ranges for the optional height/weight profile fields. These are **onboarding
 * validation configuration**, deliberately kept out of the composables so the picker UI never hardcodes
 * a limit. All bounds are canonical (centimetres / kilograms); Imperial-facing ranges are derived so a
 * unit switch never changes the underlying canonical range.
 *
 * The ranges are broad, safety-oriented input guards — not a judgement about the user. Manual entry of
 * any value inside the supported range is accepted even if it falls outside a picker's suggested list.
 */
data class ProfileMeasurementLimits(
    val heightCmMin: Double = 90.0,
    val heightCmMax: Double = 250.0,
    val weightKgMin: Double = 30.0,
    val weightKgMax: Double = 300.0,
) {
    fun isHeightValid(canonicalCm: Double): Boolean = canonicalCm in heightCmMin..heightCmMax

    fun isWeightValid(canonicalKg: Double): Boolean = canonicalKg in weightKgMin..weightKgMax

    /** Whole-centimetre options for the Metric height picker (inclusive, lazy-rendered by the UI). */
    val heightCmOptions: IntProgression
        get() = ceil(heightCmMin).toInt()..floor(heightCmMax).toInt()

    /** Selectable whole-feet range for the Imperial height picker, derived from the canonical bounds. */
    val heightFeetOptions: IntProgression
        get() {
            val cmPerFoot = HeightUnits.CM_PER_INCH * HeightUnits.INCHES_PER_FOOT
            return floor(heightCmMin / cmPerFoot).toInt()..floor(heightCmMax / cmPerFoot).toInt()
        }

    /** Display-unit weight options (kg in 0.5 steps, lb in 1 steps), as canonical-kg values. */
    fun weightOptionsKg(unit: WeightUnit): List<Double> =
        if (unit == WeightUnit.KILOGRAMS) {
            generateSequence(weightKgMin) { it + KG_PICKER_STEP }
                .takeWhile { it <= weightKgMax + EPS }
                .map { (it * 2).toLong() / 2.0 } // snap to a clean 0.5 grid
                .toList()
        } else {
            val lbMin = ceil(WeightUnits.kgToLb(weightKgMin)).toInt()
            val lbMax = floor(WeightUnits.kgToLb(weightKgMax)).toInt()
            (lbMin..lbMax).map { WeightUnits.lbToKg(it.toDouble()) }
        }

    companion object {
        val DEFAULT = ProfileMeasurementLimits()
        private const val KG_PICKER_STEP = 0.5
        private const val EPS = 1e-6
    }
}
