package com.ascend.core.domain.classes

import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassAttributeModifier
import com.ascend.core.model.ClassRewardLine
import com.ascend.core.model.XpSourceType
import javax.inject.Inject

/**
 * The class‑shaped portion of a completion reward. [awardedAttributeProficiency] is
 * what the caller should actually write to the attribute ledger (class‑scaled when a
 * class is set, identical to [baseAttributeDistribution] when not).
 */
data class ClassRewardOutcome(
    val baseAttributeDistribution: Map<AttributeType, Long>,
    val awardedAttributeProficiency: Map<AttributeType, Long>,
    val attributeModifiers: List<ClassAttributeModifier>,
    val primaryClass: ClassRewardLine?,
    val secondaryClass: ClassRewardLine?,
)

/**
 * Applies class specialization to a completion, shared by quests and workouts.
 * Player XP is handled by the caller and never passed through here — this only
 * shapes Class XP, unique proficiency, and the universal attribute distribution. It
 * computes the reward purely via [MulticlassRewardCalculator], then **persists**
 * each class's Class XP + unique proficiency idempotently. With no class selected it
 * is a pass‑through (existing behavior).
 */
class ClassRewardApplier
    @Inject
    constructor(
        private val classRepository: ClassRepository,
        private val multiclassCalculator: MulticlassRewardCalculator,
    ) {
        suspend fun apply(
            userId: String,
            basePlayerXp: Long,
            baseAttributeDistribution: Map<AttributeType, Long>,
            activityTags: Set<String>,
            sourceType: XpSourceType,
            sourceId: String,
        ): ClassRewardOutcome {
            val selection = classRepository.getSelection(userId)
            val primaryDef =
                classRepository.definition(selection.primaryClassId)
                    ?: return ClassRewardOutcome(baseAttributeDistribution, baseAttributeDistribution, emptyList(), null, null)
            val secondaryDef = classRepository.definition(selection.secondaryClassId)

            val reward =
                multiclassCalculator.calculate(
                    primaryDef = primaryDef,
                    secondaryDef = secondaryDef,
                    activityTags = activityTags,
                    basePlayerXp = basePlayerXp,
                    baseAttributeDistribution = baseAttributeDistribution,
                )

            val primaryLine = reward.primary?.let { persistAndLine(userId, it, sourceType, sourceId) }
            val secondaryLine = reward.secondary?.let { persistAndLine(userId, it, sourceType, sourceId) }

            return ClassRewardOutcome(
                baseAttributeDistribution = baseAttributeDistribution,
                awardedAttributeProficiency = reward.awardedAttributeProficiency,
                attributeModifiers = reward.attributeModifiers,
                primaryClass = primaryLine,
                secondaryClass = secondaryLine,
            )
        }

        private suspend fun persistAndLine(
            userId: String,
            contribution: ClassContribution,
            sourceType: XpSourceType,
            sourceId: String,
        ): ClassRewardLine {
            val def = contribution.def
            val award = classRepository.awardClassXp(userId, def.id, contribution.classXp, sourceType, sourceId)
            classRepository.awardProficiency(userId, def.uniqueProficiencyKey, contribution.uniqueProficiencyGain, sourceType, sourceId)
            return ClassRewardLine(
                classId = def.id,
                className = def.name,
                slot = contribution.slot,
                allocation = contribution.allocation,
                affinity = contribution.affinity,
                classXp = contribution.classXp,
                newClassLevel = award.newLevel,
                leveledUp = award.leveledUp,
                uniqueProficiencyKey = def.uniqueProficiencyKey,
                uniqueProficiencyName = def.uniqueProficiencyName,
                uniqueProficiencyGain = contribution.uniqueProficiencyGain,
            )
        }
    }
