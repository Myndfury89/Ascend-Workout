package com.ascend.core.domain.classes

import com.ascend.core.model.ClassDefinition
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * Class‑specific XP — deliberately separate from the class‑neutral player
 * `XpCalculator`, which this never touches. The multiplier interpolates from the
 * class's non‑favored rate (affinity 0) to its favored rate (affinity 1); a
 * non‑favored activity therefore still yields reduced‑but‑nonzero Class XP.
 */
class ClassXpCalculator
    @Inject
    constructor(private val config: ClassRewardConfig) {
        constructor() : this(ClassRewardConfig())

        fun multiplier(
            affinity: Double,
            def: ClassDefinition,
        ): Double {
            val a = affinity.coerceIn(0.0, 1.0)
            return def.neutralClassXpMultiplier + (def.favoredClassXpMultiplier - def.neutralClassXpMultiplier) * a
        }

        fun classXp(
            basePlayerXp: Long,
            affinity: Double,
            def: ClassDefinition,
            allocation: Double,
        ): Long {
            require(basePlayerXp >= 0) { "basePlayerXp must be >= 0" }
            return (basePlayerXp * config.classXpBaseFraction * multiplier(affinity, def) * allocation)
                .roundToLong()
                .coerceAtLeast(0)
        }

        fun secondaryAllocation(): Double = config.secondaryAllocation
    }
