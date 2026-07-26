package com.ascend.core.domain.classes

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition
import kotlin.math.roundToLong

/**
 * Tunable knobs for how a class reshapes a reward. Balancing defaults only — never
 * treat these as fixed constants.
 */
data class ClassRewardConfig(
    // Class XP base as a fraction of the (class‑neutral) Player XP for the activity.
    val classXpBaseFraction: Double = 0.6,
    // Class XP / unique‑proficiency rate for the secondary class.
    val secondaryAllocation: Double = 0.5,
    // How strongly favored activities feed the class's unique proficiency.
    val uniqueProficiencyRate: Double = 1.5,
)

/**
 * Pure, data‑driven class math. Every method takes a [ClassDefinition] as input and
 * **never branches on the class id** — affinity comes from tag overlap alone, and
 * attribute scaling only reshapes an activity's *existing* base distribution (it
 * never invents gains in unrelated attributes; constraint #4).
 */
class ClassProgressionCalculator(private val config: ClassRewardConfig = ClassRewardConfig()) {
    /** Fraction of the activity's tags that this class favors, in 0.0..1.0. */
    fun affinity(
        activityTags: Set<String>,
        def: ClassDefinition,
    ): Double {
        if (activityTags.isEmpty()) return 0.0
        val overlap = activityTags.count { it in def.favoredTags }
        return overlap.toDouble() / activityTags.size
    }

    /** Class‑XP multiplier interpolated from neutral (affinity 0) to favored (affinity 1). */
    fun classXpMultiplier(
        affinity: Double,
        def: ClassDefinition,
    ): Double {
        val a = affinity.coerceIn(0.0, 1.0)
        return def.neutralClassXpMultiplier + (def.favoredClassXpMultiplier - def.neutralClassXpMultiplier) * a
    }

    /** Class XP for one class slot; Player XP itself is never touched by this. */
    fun classXp(
        basePlayerXp: Long,
        affinity: Double,
        def: ClassDefinition,
        allocation: Double,
    ): Long {
        require(basePlayerXp >= 0) { "basePlayerXp must be >= 0" }
        val raw = basePlayerXp * config.classXpBaseFraction * classXpMultiplier(affinity, def) * allocation
        return raw.roundToLong().coerceAtLeast(0)
    }

    /**
     * Scale an activity's existing base attribute distribution by the class's
     * per‑attribute multipliers. Keys are preserved 1:1 — no attribute the activity
     * didn't already train is created.
     */
    fun scaledAttributeProficiency(
        baseDistribution: Map<AttributeType, Long>,
        def: ClassDefinition,
    ): Map<AttributeType, Long> {
        val out = LinkedHashMap<AttributeType, Long>()
        baseDistribution.forEach { (attribute, base) ->
            val scaled = (base * def.attributeMultiplier(attribute)).roundToLong().coerceAtLeast(0)
            if (scaled > 0) out[attribute] = scaled
        }
        return out
    }

    /**
     * The class's unique proficiency gain — driven heavily by favored activities:
     * base activity magnitude × affinity × the class's favored multiplier × rate.
     * A non‑favored activity (affinity 0) yields nothing.
     */
    fun uniqueProficiency(
        baseMagnitude: Long,
        affinity: Double,
        def: ClassDefinition,
        allocation: Double,
    ): Long {
        if (baseMagnitude <= 0 || affinity <= 0.0) return 0
        val raw =
            baseMagnitude * affinity.coerceIn(0.0, 1.0) *
                def.favoredClassXpMultiplier * config.uniqueProficiencyRate * allocation
        return raw.roundToLong().coerceAtLeast(0)
    }

    fun secondaryAllocation(): Double = config.secondaryAllocation
}
