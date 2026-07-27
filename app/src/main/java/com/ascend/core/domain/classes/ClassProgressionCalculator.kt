package com.ascend.core.domain.classes

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition

/**
 * Backward‑compatible facade over the focused class calculators
 * ([ActivityAffinityCalculator], [ClassXpCalculator], [UniqueProficiencyCalculator],
 * [ClassAttributeScaler]). Kept so existing callers/tests have one entry point; new
 * code should prefer the specific calculators or [MulticlassRewardCalculator].
 */
class ClassProgressionCalculator(config: ClassRewardConfig = ClassRewardConfig()) {
    private val affinityCalculator = ActivityAffinityCalculator()
    private val classXpCalculator = ClassXpCalculator(config)
    private val uniqueProficiencyCalculator = UniqueProficiencyCalculator(config)
    private val attributeScaler = ClassAttributeScaler()

    fun affinity(
        activityTags: Set<String>,
        def: ClassDefinition,
    ): Double = affinityCalculator.affinity(activityTags, def)

    fun classXpMultiplier(
        affinity: Double,
        def: ClassDefinition,
    ): Double = classXpCalculator.multiplier(affinity, def)

    fun classXp(
        basePlayerXp: Long,
        affinity: Double,
        def: ClassDefinition,
        allocation: Double,
    ): Long = classXpCalculator.classXp(basePlayerXp, affinity, def, allocation)

    fun scaledAttributeProficiency(
        baseDistribution: Map<AttributeType, Long>,
        def: ClassDefinition,
    ): Map<AttributeType, Long> = attributeScaler.scale(baseDistribution, def)

    fun uniqueProficiency(
        baseMagnitude: Long,
        affinity: Double,
        def: ClassDefinition,
        allocation: Double,
    ): Long = uniqueProficiencyCalculator.gain(baseMagnitude, affinity, def, allocation)

    fun secondaryAllocation(): Double = classXpCalculator.secondaryAllocation()
}
