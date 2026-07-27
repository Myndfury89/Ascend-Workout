package com.ascend.core.domain.classes

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassAttributeModifier
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassSlot
import javax.inject.Inject

/** One class's pure (non‑persisted) contribution to a completion reward. */
data class ClassContribution(
    val def: ClassDefinition,
    val slot: ClassSlot,
    val allocation: Double,
    val affinity: Double,
    val classXp: Long,
    val uniqueProficiencyGain: Long,
)

/**
 * The fully computed multiclass reward for a completion, before persistence.
 * [awardedAttributeProficiency] is shaped by the primary class only; a secondary
 * class contributes Class XP + unique proficiency at reduced allocation but never
 * re‑modifies attributes.
 */
data class MulticlassReward(
    val awardedAttributeProficiency: Map<AttributeType, Long>,
    val attributeModifiers: List<ClassAttributeModifier>,
    val primary: ClassContribution?,
    val secondary: ClassContribution?,
)

/**
 * Pure calculation of a multiclass reward (no persistence, no side effects). The
 * caller supplies the resolved class definitions; this composes affinity, class XP,
 * unique proficiency and attribute scaling. With no primary class it's a
 * pass‑through — attributes stay at their base and no class reward is produced.
 */
class MulticlassRewardCalculator
    @Inject
    constructor(
        private val affinityCalculator: ActivityAffinityCalculator,
        private val classXpCalculator: ClassXpCalculator,
        private val uniqueProficiencyCalculator: UniqueProficiencyCalculator,
        private val attributeScaler: ClassAttributeScaler,
    ) {
        /** Convenience for tests/manual construction with default calculators. */
        constructor() : this(
            ActivityAffinityCalculator(),
            ClassXpCalculator(),
            UniqueProficiencyCalculator(),
            ClassAttributeScaler(),
        )

        fun calculate(
            primaryDef: ClassDefinition?,
            secondaryDef: ClassDefinition?,
            activityTags: Set<String>,
            basePlayerXp: Long,
            baseAttributeDistribution: Map<AttributeType, Long>,
        ): MulticlassReward {
            if (primaryDef == null) {
                return MulticlassReward(baseAttributeDistribution, emptyList(), null, null)
            }
            val baseMagnitude = baseAttributeDistribution.values.sum()

            val primary = contribution(primaryDef, ClassSlot.PRIMARY, allocation = 1.0, activityTags, basePlayerXp, baseMagnitude)
            val awardedAttrs = attributeScaler.scale(baseAttributeDistribution, primaryDef)
            val modifiers = attributeScaler.modifiers(baseAttributeDistribution, primaryDef)

            val secondary =
                secondaryDef?.let {
                    contribution(
                        it,
                        ClassSlot.SECONDARY,
                        classXpCalculator.secondaryAllocation(),
                        activityTags,
                        basePlayerXp,
                        baseMagnitude,
                    )
                }

            return MulticlassReward(awardedAttrs, modifiers, primary, secondary)
        }

        private fun contribution(
            def: ClassDefinition,
            slot: ClassSlot,
            allocation: Double,
            activityTags: Set<String>,
            basePlayerXp: Long,
            baseMagnitude: Long,
        ): ClassContribution {
            val affinity = affinityCalculator.affinity(activityTags, def)
            return ClassContribution(
                def = def,
                slot = slot,
                allocation = allocation,
                affinity = affinity,
                classXp = classXpCalculator.classXp(basePlayerXp, affinity, def, allocation),
                uniqueProficiencyGain = uniqueProficiencyCalculator.gain(baseMagnitude, affinity, def, allocation),
            )
        }
    }
