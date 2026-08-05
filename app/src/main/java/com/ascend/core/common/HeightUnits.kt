package com.ascend.core.common

import com.ascend.core.model.MeasurementSystem
import kotlin.math.roundToInt

/**
 * Height counterpart to [WeightUnits]. **All heights are stored canonically in centimetres**
 * (Double); conversion to feet/inches happens only at the presentation and input boundaries. Like
 * weight, changing the displayed unit never alters the stored value — so switching Metric/Imperial
 * can never be read as a body change. `1 in = 2.54 cm`, `1 ft = 12 in`.
 */
object HeightUnits {
    const val CM_PER_INCH = 2.54
    const val INCHES_PER_FOOT = 12

    /** A height expressed in whole feet + inches (0–11), for Imperial presentation. */
    data class FeetInches(val feet: Int, val inches: Int)

    /** Canonical centimetres → nearest whole feet/inches. Inches stays in 0–11 (rollover handled). */
    fun cmToFeetInches(canonicalCm: Double): FeetInches {
        val totalInches = (canonicalCm / CM_PER_INCH).roundToInt()
        return FeetInches(feet = totalInches / INCHES_PER_FOOT, inches = totalInches % INCHES_PER_FOOT)
    }

    /** Feet + inches → canonical centimetres (input boundary). */
    fun feetInchesToCm(
        feet: Int,
        inches: Int,
    ): Double = (feet * INCHES_PER_FOOT + inches) * CM_PER_INCH

    /** Canonical centimetres → whole centimetres for the Metric picker/display. */
    fun cmWhole(canonicalCm: Double): Int = canonicalCm.roundToInt()

    /** The numeric+unit display string for the given system, e.g. "6 ft 2 in" or "188 cm". */
    fun format(
        canonicalCm: Double,
        system: MeasurementSystem,
    ): String =
        if (system == MeasurementSystem.IMPERIAL) {
            val fi = cmToFeetInches(canonicalCm)
            "${fi.feet} ft ${fi.inches} in"
        } else {
            "${cmWhole(canonicalCm)} cm"
        }

    /** A screen-reader label that never relies on colour, e.g. "Height, 6 feet 2 inches". */
    fun accessibilityLabel(
        canonicalCm: Double,
        system: MeasurementSystem,
    ): String =
        if (system == MeasurementSystem.IMPERIAL) {
            val fi = cmToFeetInches(canonicalCm)
            "Height, ${fi.feet} feet ${fi.inches} inches"
        } else {
            "Height, ${cmWhole(canonicalCm)} centimetres"
        }
}
