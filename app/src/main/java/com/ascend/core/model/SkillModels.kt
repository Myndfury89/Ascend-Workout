package com.ascend.core.model

/*
 * Skills & Techniques. Skills are persistent abilities unlocked through verified real
 * activity — completed workouts, personal records, proven progressions, duration/distance,
 * or consistency. Player XP and Class Level may reveal or recommend a Skill but can never
 * independently unlock it, and class never gates access to the initial Skills: class affinity
 * may only change Skill-XP rate, effectiveness, presentation, or recommendation priority.
 */

/** The kind of real-activity evidence a prerequisite requires. */
enum class SkillPrerequisiteType {
    CARDIO_CONTINUOUS_SECONDS,
    CARDIO_DISTANCE_METERS,
    STRENGTH_PERSONAL_RECORD,
    LOAD_PROGRESSION_PROVEN,
    BODYWEIGHT_VARIATION_ADVANCED,
    TEMPO_PROGRESSION,
    ASSISTANCE_REDUCED,
    MOBILITY_OR_BALANCE_MILESTONE,
    CONSISTENCY_SESSIONS,
    PROGRESSION_EVENT,
    DURATION_SECONDS,
}

/**
 * One real-activity requirement. [classIds] is an **optional** hook for *future* class-exclusive
 * Skills; for every initial Skill it must be null or empty — a class prerequisite may never gate
 * an initial Skill. [hidden] prerequisites are counted but their detail is not surfaced.
 */
data class SkillPrerequisite(
    val type: SkillPrerequisiteType,
    val threshold: Double,
    val requiredCount: Int = 1,
    val requiresCompletion: Boolean = true,
    val disqualifiedBySafetyEvent: Boolean = true,
    val classIds: List<String>? = null,
    val hidden: Boolean = false,
    // Prerequisites sharing a non-null [group] are OR-ed (any one satisfies the group); a null
    // group is its own AND requirement. A Skill is eligible when every group is satisfied.
    val group: Int? = null,
    val description: String,
) {
    val hasHardClassRequirement: Boolean get() = !classIds.isNullOrEmpty()
}

/** A prototype game effect a Skill grants (used by the simulated Dungeon; never real). */
enum class SkillEffectType {
    REVEAL_ENEMY_WEAKNESS,
    REVEAL_PARTY_COMPOSITION,
    REVEAL_ENCOUNTER_DIFFICULTY,
    REVEAL_DUNGEON_INFO,
    STRENGTH_CONTRIBUTION_MULTIPLIER,
    ARMOR_BREAK,
    HEAVY_ACTION,
    STAMINA_COST_REDUCTION,
    SUSTAINED_CONTRIBUTION,
    ENDURANCE_ABILITY,
    TECHNIQUE_ACTION,
    COUNTER_OR_EVASION,
    BODYWEIGHT_COMBO,
}

data class SkillEffectDefinition(
    val type: SkillEffectType,
    val baseMagnitude: Double,
    val perLevelMagnitude: Double = 0.0,
    val description: String,
)

/**
 * A data-driven Skill. All balancing lives in the seed catalog / DB, never in the engines.
 * [classAffinityTags] favor Skill-XP rate + recommendation for matching classes but never gate
 * access. [transferable] Skills survive a class switch.
 */
data class SkillDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val prerequisites: List<SkillPrerequisite>,
    val effects: List<SkillEffectDefinition>,
    val classAffinityTags: Set<String> = emptySet(),
    val baseXpPerEvidence: Long = 40,
    val maxLevel: Int = 10,
    val transferable: Boolean = true,
    val classIds: List<String>? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
) {
    /** True only if this Skill hard-requires a class (definition-level or any prerequisite). */
    val hasHardClassRequirement: Boolean
        get() = !classIds.isNullOrEmpty() || prerequisites.any { it.hasHardClassRequirement }

    val visiblePrerequisites: List<SkillPrerequisite> get() = prerequisites.filterNot { it.hidden }
    val hiddenPrerequisiteCount: Int get() = prerequisites.count { it.hidden }
}

/** A player's state for one Skill. */
data class PlayerSkill(
    val userId: String,
    val skillId: String,
    val unlocked: Boolean,
    val level: Int,
    val skillXp: Long,
    val currentLevelXp: Long,
    val xpToNextLevel: Long,
    val unlockedAt: Long?,
) {
    val progressFraction: Float
        get() = if (xpToNextLevel <= 0L) 1f else currentLevelXp.toFloat() / xpToNextLevel.toFloat()
}

/** One idempotent Skill-XP grant. Unique on (userId, skillId, sourceType, sourceId). */
data class SkillProgressTransaction(
    val id: String,
    val userId: String,
    val skillId: String,
    val amount: Long,
    val sourceType: String,
    val sourceId: String,
    val createdAt: Long,
)

/** An append-only record that a Skill was unlocked, with the evidence behind it. */
data class SkillUnlockEvent(
    val id: String,
    val userId: String,
    val skillId: String,
    val unlockedAt: Long,
    val triggeringSourceType: String,
    val triggeringSourceId: String,
    val evidence: List<String>,
)

/**
 * Aggregated **real-activity** evidence, derived from existing workout / adaptive-training /
 * quest / progression records (no duplicated raw data). Fed into the eligibility engine. A
 * [disqualifyingSafetyEvent] blocks completion-gated prerequisites.
 */
data class SkillEvidence(
    val cardioContinuousSeconds: Long = 0,
    val cardioDistanceMeters: Double = 0.0,
    val strengthPersonalRecords: Int = 0,
    val loadProgressionsProven: Int = 0,
    val bodyweightVariationAdvances: Int = 0,
    val tempoProgressions: Int = 0,
    val assistanceReductions: Int = 0,
    val mobilityOrBalanceMilestones: Int = 0,
    val consistencySessions: Int = 0,
    val progressionEvents: Int = 0,
    val totalDurationSeconds: Long = 0,
    val disqualifyingSafetyEvent: Boolean = false,
    val sourceActivities: List<String> = emptyList(),
)

/** An explainable eligibility result for one Skill against the current evidence. */
data class SkillEligibility(
    val skillId: String,
    val eligible: Boolean,
    val unlocked: Boolean,
    val matchedPrerequisites: List<String>,
    val missingPrerequisites: List<String>,
    val hiddenPrerequisiteCount: Int,
    val evidence: List<String>,
    val sourceActivities: List<String>,
    val classAffinity: Double,
    val finalXpMultiplier: Double,
)

/** A resolved Skill effect at a given level (magnitude scales with level). */
data class ResolvedSkillEffect(
    val type: SkillEffectType,
    val magnitude: Double,
    val level: Int,
    val classAffinity: Double,
    val description: String,
)
