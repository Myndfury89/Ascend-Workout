package com.ascend.core.domain.skill

import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.PlayerSkill
import com.ascend.core.model.ResolvedSkillEffect
import com.ascend.core.model.SkillDefinition
import com.ascend.core.model.SkillEligibility
import com.ascend.core.model.SkillEvidence
import com.ascend.core.model.SkillPrerequisite
import com.ascend.core.model.SkillPrerequisiteType
import javax.inject.Inject

/** Tunable Skill balancing — affinity bonuses only ever change XP/effectiveness, never access. */
data class SkillConfig(
    val xpAffinityBonus: Double = 0.30,
    val effectAffinityBonus: Double = 0.15,
    val xpPerLevel: Long = 100,
)

/**
 * How favored a Skill is by a class — tag overlap only, in 0.0..1.0. This shapes Skill‑XP rate,
 * effectiveness, and recommendation priority; it **never** gates eligibility. No class (or a
 * class with no overlap) is simply neutral (0.0).
 */
class SkillClassAffinityResolver
    @Inject
    constructor() {
        fun affinity(
            skill: SkillDefinition,
            classDef: ClassDefinition?,
        ): Double {
            if (classDef == null || skill.classAffinityTags.isEmpty()) return 0.0
            val matched = skill.classAffinityTags.count { it in classDef.favoredTags }
            return matched.toDouble() / skill.classAffinityTags.size
        }
    }

/**
 * Decides Skill eligibility from **real-activity evidence**, explainably. Prerequisites grouped
 * under the same id are OR-ed; distinct groups are AND-ed. A disqualifying safety event blocks a
 * completion-gated prerequisite. Class affinity is reported (for XP/priority) but is **never** a
 * gate — a Skill with no hard class requirement is reachable by any class or no class.
 */
class SkillEligibilityEngine
    @Inject
    constructor(private val config: SkillConfig) {
        constructor() : this(SkillConfig())

        fun evaluate(
            skill: SkillDefinition,
            evidence: SkillEvidence,
            classAffinity: Double,
            unlocked: Boolean,
        ): SkillEligibility {
            val matched = mutableListOf<String>()
            val missing = mutableListOf<String>()

            groupsOf(skill).forEach { group ->
                val satisfied = group.any { satisfies(it, evidence) }
                val visible = group.filterNot { it.hidden }
                if (visible.isNotEmpty()) {
                    val label = visible.joinToString(" or ") { it.description }
                    if (satisfied) matched += label else missing += label
                }
            }
            // Every group (incl. hidden-only groups) must be satisfied for eligibility.
            val allSatisfied = groupsOf(skill).all { group -> group.any { satisfies(it, evidence) } }

            return SkillEligibility(
                skillId = skill.id,
                eligible = allSatisfied,
                unlocked = unlocked,
                matchedPrerequisites = matched,
                missingPrerequisites = missing,
                hiddenPrerequisiteCount = skill.hiddenPrerequisiteCount,
                evidence = describeEvidence(evidence),
                sourceActivities = evidence.sourceActivities,
                classAffinity = classAffinity,
                finalXpMultiplier = 1.0 + classAffinity * config.xpAffinityBonus,
            )
        }

        private fun groupsOf(skill: SkillDefinition): List<List<SkillPrerequisite>> {
            val map = LinkedHashMap<Int, MutableList<SkillPrerequisite>>()
            skill.prerequisites.forEachIndexed { i, p ->
                val g = p.group ?: (NULL_GROUP_BASE + i)
                map.getOrPut(g) { mutableListOf() }.add(p)
            }
            return map.values.toList()
        }

        private fun satisfies(
            p: SkillPrerequisite,
            e: SkillEvidence,
        ): Boolean {
            if (p.disqualifiedBySafetyEvent && e.disqualifyingSafetyEvent) return false
            return when (p.type) {
                SkillPrerequisiteType.CARDIO_CONTINUOUS_SECONDS -> e.cardioContinuousSeconds >= p.threshold
                SkillPrerequisiteType.CARDIO_DISTANCE_METERS -> e.cardioDistanceMeters >= p.threshold
                SkillPrerequisiteType.STRENGTH_PERSONAL_RECORD -> e.strengthPersonalRecords >= p.requiredCount
                SkillPrerequisiteType.LOAD_PROGRESSION_PROVEN -> e.loadProgressionsProven >= p.requiredCount
                SkillPrerequisiteType.BODYWEIGHT_VARIATION_ADVANCED -> e.bodyweightVariationAdvances >= p.requiredCount
                SkillPrerequisiteType.TEMPO_PROGRESSION -> e.tempoProgressions >= p.requiredCount
                SkillPrerequisiteType.ASSISTANCE_REDUCED -> e.assistanceReductions >= p.requiredCount
                SkillPrerequisiteType.MOBILITY_OR_BALANCE_MILESTONE -> e.mobilityOrBalanceMilestones >= p.requiredCount
                SkillPrerequisiteType.CONSISTENCY_SESSIONS -> e.consistencySessions >= p.threshold
                SkillPrerequisiteType.PROGRESSION_EVENT -> e.progressionEvents >= p.requiredCount
                SkillPrerequisiteType.DURATION_SECONDS -> e.totalDurationSeconds >= p.threshold
            }
        }

        private fun describeEvidence(e: SkillEvidence): List<String> =
            buildList {
                if (e.cardioContinuousSeconds > 0) add("Longest continuous cardio ${e.cardioContinuousSeconds}s")
                if (e.cardioDistanceMeters > 0) add("Cardio distance ${e.cardioDistanceMeters.toInt()}m")
                if (e.strengthPersonalRecords > 0) add("${e.strengthPersonalRecords} strength PR(s)")
                if (e.loadProgressionsProven > 0) add("${e.loadProgressionsProven} load progression(s) proven")
                if (e.bodyweightVariationAdvances > 0) add("${e.bodyweightVariationAdvances} variation advance(s)")
                if (e.tempoProgressions > 0) add("${e.tempoProgressions} tempo progression(s)")
                if (e.assistanceReductions > 0) add("${e.assistanceReductions} assistance reduction(s)")
                if (e.mobilityOrBalanceMilestones > 0) add("${e.mobilityOrBalanceMilestones} mobility/balance milestone(s)")
                if (e.consistencySessions > 0) add("${e.consistencySessions} consistent session(s)")
                if (e.disqualifyingSafetyEvent) add("A safety event blocks completion-gated unlocks")
            }

        private companion object {
            const val NULL_GROUP_BASE = 100_000
        }
    }

/** Finds Skills that just became eligible but aren't unlocked yet (real evidence only). */
class SkillUnlockEvaluator
    @Inject
    constructor(private val eligibilityEngine: SkillEligibilityEngine) {
        fun newlyUnlockable(
            skills: List<SkillDefinition>,
            evidence: SkillEvidence,
            unlockedIds: Set<String>,
            affinityOf: (SkillDefinition) -> Double = { 0.0 },
        ): List<SkillEligibility> =
            skills
                .filter { it.enabled && it.id !in unlockedIds }
                .map { eligibilityEngine.evaluate(it, evidence, affinityOf(it), unlocked = false) }
                .filter { it.eligible }
    }

/** Pure Skill-XP amount for one evidence grant, scaled by class affinity (XP only). */
class SkillProgressCalculator
    @Inject
    constructor(private val config: SkillConfig) {
        constructor() : this(SkillConfig())

        fun xpFor(
            skill: SkillDefinition,
            evidenceUnits: Int,
            classAffinity: Double,
        ): Long {
            val base = skill.baseXpPerEvidence * evidenceUnits.coerceAtLeast(1)
            return (base * (1.0 + classAffinity * config.xpAffinityBonus)).toLong()
        }
    }

data class SkillLevelState(
    val level: Int,
    val currentLevelXp: Long,
    val xpToNextLevel: Long,
)

/** Pure Skill level curve. Level 1 at 0 XP; each [SkillConfig.xpPerLevel] XP is one level. */
class SkillLevelCalculator
    @Inject
    constructor(private val config: SkillConfig) {
        constructor() : this(SkillConfig())

        fun resolve(
            skillXp: Long,
            maxLevel: Int,
        ): SkillLevelState {
            val per = config.xpPerLevel.coerceAtLeast(1)
            val rawLevel = (skillXp / per).toInt() + 1
            val level = rawLevel.coerceIn(1, maxLevel)
            val atMax = level >= maxLevel
            return SkillLevelState(
                level = level,
                currentLevelXp = if (atMax) per else skillXp % per,
                xpToNextLevel = per,
            )
        }
    }

/** Resolves a Skill's effects at a level. Off-class effects stay usable (affinity only bonuses). */
class SkillEffectResolver
    @Inject
    constructor(private val config: SkillConfig) {
        constructor() : this(SkillConfig())

        fun resolve(
            skill: SkillDefinition,
            level: Int,
            classAffinity: Double,
        ): List<ResolvedSkillEffect> =
            skill.effects.map { e ->
                val leveled = e.baseMagnitude + e.perLevelMagnitude * (level - 1).coerceAtLeast(0)
                val magnitude = leveled * (1.0 + classAffinity * config.effectAffinityBonus)
                ResolvedSkillEffect(e.type, magnitude, level, classAffinity, e.description)
            }
    }

/**
 * Decides which unlocked Skills survive a class switch. Transferable Skills always stay; a
 * (future) class-exclusive Skill stays only if its [SkillDefinition.classIds] includes the new
 * class. Every initial Skill is transferable, so a switch never removes one.
 */
class TransferableSkillResolver
    @Inject
    constructor() {
        fun retainedAfterSwitch(
            playerSkills: List<PlayerSkill>,
            definitionsById: Map<String, SkillDefinition>,
            newClassId: String?,
        ): List<PlayerSkill> =
            playerSkills.filter { ps ->
                val def = definitionsById[ps.skillId] ?: return@filter true
                def.transferable || (def.classIds?.contains(newClassId) == true)
            }
    }
