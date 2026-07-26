package com.ascend.core.domain.classes

import com.ascend.core.domain.repository.ClassRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassRewardLine
import com.ascend.core.model.ClassSlot
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
    val primaryClass: ClassRewardLine?,
    val secondaryClass: ClassRewardLine?,
)

/**
 * Applies class specialization to a completion's rewards, shared by quests and
 * workouts. Player XP is handled by the caller and never passed through here — this
 * only shapes Class XP, unique proficiency, and the universal attribute
 * distribution. With no class selected it is a pass‑through (existing behavior).
 *
 * Primary class modifies the attribute distribution AND earns Class XP + unique
 * proficiency; the secondary class earns Class XP + unique proficiency at a reduced
 * allocation but does not re‑modify attributes (no double award).
 */
class ClassRewardApplier
    @Inject
    constructor(
        private val classRepository: ClassRepository,
        private val calculator: ClassProgressionCalculator,
    ) {
        suspend fun apply(
            userId: String,
            basePlayerXp: Long,
            baseAttributeDistribution: Map<AttributeType, Long>,
            activityTags: Set<String>,
            sourceType: XpSourceType,
            sourceId: String,
        ): ClassRewardOutcome {
            val primaryDef =
                classRepository.definition(classRepository.getSelection(userId).primaryClassId)
                    ?: return ClassRewardOutcome(baseAttributeDistribution, baseAttributeDistribution, null, null)

            val baseMagnitude = baseAttributeDistribution.values.sum()

            // Primary class: attributes + Class XP + unique proficiency.
            val affinityP = calculator.affinity(activityTags, primaryDef)
            val awardedAttrs = calculator.scaledAttributeProficiency(baseAttributeDistribution, primaryDef)
            val classXpP = calculator.classXp(basePlayerXp, affinityP, primaryDef, allocation = 1.0)
            val uniqueP = calculator.uniqueProficiency(baseMagnitude, affinityP, primaryDef, allocation = 1.0)
            val awardP = classRepository.awardClassXp(userId, primaryDef.id, classXpP, sourceType, sourceId)
            classRepository.awardProficiency(userId, primaryDef.uniqueProficiencyKey, uniqueP, sourceType, sourceId)
            val primaryLine =
                ClassRewardLine(
                    classId = primaryDef.id, className = primaryDef.name, slot = ClassSlot.PRIMARY,
                    allocation = 1.0, affinity = affinityP, classXp = classXpP,
                    newClassLevel = awardP.newLevel, leveledUp = awardP.leveledUp,
                    uniqueProficiencyKey = primaryDef.uniqueProficiencyKey,
                    uniqueProficiencyName = primaryDef.uniqueProficiencyName, uniqueProficiencyGain = uniqueP,
                )

            // Secondary class (optional): Class XP + unique proficiency only.
            val secondaryLine =
                classRepository.definition(classRepository.getSelection(userId).secondaryClassId)?.let { def ->
                    val alloc = calculator.secondaryAllocation()
                    val affinityS = calculator.affinity(activityTags, def)
                    val classXpS = calculator.classXp(basePlayerXp, affinityS, def, allocation = alloc)
                    val uniqueS = calculator.uniqueProficiency(baseMagnitude, affinityS, def, allocation = alloc)
                    val awardS = classRepository.awardClassXp(userId, def.id, classXpS, sourceType, sourceId)
                    classRepository.awardProficiency(userId, def.uniqueProficiencyKey, uniqueS, sourceType, sourceId)
                    ClassRewardLine(
                        classId = def.id, className = def.name, slot = ClassSlot.SECONDARY,
                        allocation = alloc, affinity = affinityS, classXp = classXpS,
                        newClassLevel = awardS.newLevel, leveledUp = awardS.leveledUp,
                        uniqueProficiencyKey = def.uniqueProficiencyKey,
                        uniqueProficiencyName = def.uniqueProficiencyName, uniqueProficiencyGain = uniqueS,
                    )
                }

            return ClassRewardOutcome(baseAttributeDistribution, awardedAttrs, primaryLine, secondaryLine)
        }
    }
