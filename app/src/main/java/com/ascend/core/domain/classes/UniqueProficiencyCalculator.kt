package com.ascend.core.domain.classes

import com.ascend.core.model.ClassDefinition
import javax.inject.Inject
import kotlin.math.roundToLong

/**
 * The class's unique proficiency (Force / Body Mastery / Energy Control) — kept
 * entirely separate from the five universal attributes. Driven heavily by favored
 * activities: a non‑favored activity (affinity 0) yields nothing.
 */
class UniqueProficiencyCalculator
    @Inject
    constructor(private val config: ClassRewardConfig) {
        constructor() : this(ClassRewardConfig())

        fun gain(
            baseMagnitude: Long,
            affinity: Double,
            def: ClassDefinition,
            allocation: Double,
        ): Long {
            if (baseMagnitude <= 0 || affinity <= 0.0) return 0
            return (
                baseMagnitude * affinity.coerceIn(0.0, 1.0) *
                    def.favoredClassXpMultiplier * config.uniqueProficiencyRate * allocation
            ).roundToLong().coerceAtLeast(0)
        }
    }
