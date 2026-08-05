package com.ascend.core.common

import com.ascend.core.model.MeasurementSystem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The global weight-unit system. **All weights are stored canonically in kilograms** (Double);
 * conversion happens only at the presentation and user-input boundaries. Changing the display unit
 * never alters a stored value — so a unit switch can never create a personal record or trigger a
 * progression reward. `1 kg = 2.2046226218 lb` (per spec).
 */
enum class WeightUnit(
    val symbol: String,
    val displayName: String,
) {
    KILOGRAMS("kg", "Kilograms"),
    POUNDS("lb", "Pounds"),
    ;

    /** A screen-reader label that never relies on colour to convey the selection. */
    val accessibilityLabel: String get() = "Weight unit, ${displayName.lowercase()} selected"

    /**
     * The measurement system this unit belongs to, so a single unit toggle can also drive height
     * presentation (kg → Metric/centimetres, lb → Imperial/feet-inches) without a second setting.
     */
    val measurementSystem: MeasurementSystem
        get() = if (this == POUNDS) MeasurementSystem.IMPERIAL else MeasurementSystem.METRIC

    companion object {
        /** The default weight unit for a measurement system (Metric → kg, Imperial → lb). */
        fun forMeasurementSystem(system: MeasurementSystem): WeightUnit = if (system == MeasurementSystem.IMPERIAL) POUNDS else KILOGRAMS

        fun fromNameOrDefault(
            name: String?,
            default: WeightUnit = KILOGRAMS,
        ): WeightUnit = entries.firstOrNull { it.name == name } ?: default
    }
}

/** Configurable display rounding. Whole numbers render without decimals within [wholeEpsilon]. */
data class WeightRoundingConfig(
    val kgDecimals: Int = 1,
    val lbDecimals: Int = 1,
    val wholeEpsilon: Double = 0.05,
)

/**
 * Pure conversion + formatting between canonical kilograms and the selected display unit, plus the
 * equipment-increment sets (stored canonically, displayed in the selected unit). No state, no I/O —
 * so it can never mutate a workout record.
 */
object WeightUnits {
    const val LB_PER_KG = 2.2046226218

    fun kgToLb(kg: Double): Double = kg * LB_PER_KG

    fun lbToKg(lb: Double): Double = lb / LB_PER_KG

    /** Convert a user-entered value in [unit] to canonical kilograms (input boundary). */
    fun toCanonicalKg(
        value: Double,
        unit: WeightUnit,
    ): Double = if (unit == WeightUnit.KILOGRAMS) value else lbToKg(value)

    /** Convert a canonical kilogram value to the display [unit] (presentation boundary). */
    fun fromCanonicalKg(
        canonicalKg: Double,
        unit: WeightUnit,
    ): Double = if (unit == WeightUnit.KILOGRAMS) canonicalKg else kgToLb(canonicalKg)

    /** The numeric part only, rounded per [config], with whole-number display when effectively whole. */
    fun formatValue(
        canonicalKg: Double,
        unit: WeightUnit,
        config: WeightRoundingConfig = WeightRoundingConfig(),
    ): String {
        val value = fromCanonicalKg(canonicalKg, unit)
        val decimals = if (unit == WeightUnit.KILOGRAMS) config.kgDecimals else config.lbDecimals
        return if (abs(value - value.roundToLong()) < config.wholeEpsilon) {
            value.roundToLong().toString()
        } else {
            String.format(Locale.US, "%.${decimals}f", value)
        }
    }

    /** The full display string, e.g. "220.5 lb" or "100 kg". */
    fun format(
        canonicalKg: Double,
        unit: WeightUnit,
        config: WeightRoundingConfig = WeightRoundingConfig(),
    ): String = "${formatValue(canonicalKg, unit, config)} ${unit.symbol}"

    /** A screen-reader label that never relies on colour, e.g. "Weight, 112.5 kilograms". */
    fun accessibilityLabel(
        canonicalKg: Double,
        unit: WeightUnit,
        config: WeightRoundingConfig = WeightRoundingConfig(),
    ): String = "Weight, ${formatValue(canonicalKg, unit, config)} ${unit.displayName.lowercase()}"

    // ---- equipment increments (canonical kg; displayed in the selected unit) ----

    private val KG_INCREMENTS = listOf(1.0, 1.25, 2.5, 5.0)
    private val LB_INCREMENTS_KG = listOf(2.5, 5.0, 10.0).map { lbToKg(it) }

    /** Canonical-kg increment options appropriate to the selected [unit]'s real equipment. */
    fun incrementsFor(unit: WeightUnit): List<Double> = if (unit == WeightUnit.KILOGRAMS) KG_INCREMENTS else LB_INCREMENTS_KG

    /**
     * Format one canonical-kg increment for display in [unit], keeping up to two decimals so a
     * fine plate like 1.25 kg reads exactly (trailing zeros trimmed): "1.25 kg", "1 kg", "2.5 lb".
     */
    fun formatIncrement(
        canonicalKg: Double,
        unit: WeightUnit,
    ): String {
        val value = fromCanonicalKg(canonicalKg, unit)
        val trimmed = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        return "$trimmed ${unit.symbol}"
    }

    /** The increment options as display strings in the selected unit (e.g. "2.5 lb", "1.25 kg"). */
    fun displayIncrements(unit: WeightUnit): List<String> = incrementsFor(unit).map { formatIncrement(it, unit) }
}
