package com.ascend.core.model

/*
 * The Simulated Party Dungeon domain. Combat is fully **deterministic** — the same inputs always
 * produce the same outputs — so visuals only ever render results, never compute them. There is no
 * Bluetooth, no real proximity, and no stranger matchmaking anywhere in this domain; party members
 * are simulated. Real workout activity drives contribution; a safety stop always suspends it, and
 * the domain never encourages continuing through pain, dizziness, chest pain, injury, severe
 * fatigue, illness, or severe shortness of breath.
 */

/** The real-activity action a member performs, before it is mapped to a combat contribution. */
enum class WorkoutActionType {
    STRENGTH_SET,
    CARDIO_DURATION,
    PERSONAL_RECORD,
    QUEST_INTERVAL,
    MOBILITY_OR_AGILITY,
    RECOVERY,
}

/** The combat contribution a workout action maps to. */
enum class ContributionType {
    HEAVY_ATTACK,
    ARMOR_BREAK,
    SUSTAINED_DAMAGE,
    STAMINA_DRAIN,
    CRITICAL_STRIKE,
    COMBO,
    EVASION,
    COUNTER,
    SHIELD,
    STAMINA_RESTORE,
    HEAL,
    ;

    /** Support contributions don't deal damage — they protect/sustain the party. */
    val isSupport: Boolean get() = this == SHIELD || this == STAMINA_RESTORE || this == HEAL || this == EVASION
}

/** A member's live participation state. Only [ACTIVE] contributes. */
enum class MemberActivityState {
    ACTIVE,
    IDLE_WARNING,
    INACTIVE,
    PAUSED,
    DISCONNECTED,
    SAFETY_PAUSED,
    ;

    val canContribute: Boolean get() = this == ACTIVE
}

enum class DifficultyRating { TRIVIAL, MODERATE, HARD, SEVERE }

enum class DungeonEncounterState { PREPARING, IN_PROGRESS, VICTORY, DEFEAT, ABANDONED }

/** One enemy phase, triggered when health falls to [healthThresholdFraction] of max. */
data class EnemyPhase(
    val index: Int,
    val name: String,
    val healthThresholdFraction: Float,
    val damageResistance: Float,
    val enraged: Boolean = false,
)

/** An original Ascend enemy. [weaknessTags] match a contribution's element for bonus damage. */
data class DungeonEnemy(
    val id: String,
    val name: String,
    val baseHealth: Long,
    val armor: Long,
    val weaknessTags: Set<String>,
    val phases: List<EnemyPhase>,
    val description: String,
)

/** A dungeon: its enemies, tier, party guidance, and prerequisites. */
data class DungeonDefinition(
    val id: String,
    val name: String,
    val tier: Int,
    val enemies: List<DungeonEnemy>,
    val recommendedPartySize: Int,
    val soloAllowed: Boolean,
    val baseRewardXp: Long,
    val description: String,
)

/** A (simulated) party member. [earnedContribution] is retained even after going inactive. */
data class DungeonPartyMember(
    val id: String,
    val displayName: String,
    val trainingTier: Int,
    val classId: String?,
    val skillIds: Set<String>,
    val activityState: MemberActivityState,
    val earnedContribution: Long,
    val lastActivityAt: Long,
    val hasLeft: Boolean = false,
)

data class DungeonParty(
    val members: List<DungeonPartyMember>,
    val solo: Boolean,
) {
    val activeMembers: List<DungeonPartyMember> get() = members.filter { it.activityState.canContribute && !it.hasLeft }
}

/** One mapped combat contribution (deterministic magnitude + element). */
data class WorkoutContribution(
    val memberId: String,
    val type: ContributionType,
    val magnitude: Long,
    val elementTag: String,
    val sourceType: String,
    val sourceId: String,
    val armorBreaking: Boolean = false,
)

/** The deterministic combat state for one encounter. */
data class DungeonCombatState(
    val enemyIndex: Int,
    val enemyHealth: Long,
    val maxEnemyHealth: Long,
    val phaseIndex: Int,
    val partyShield: Long,
    val partyStamina: Long,
    val totalDamageDealt: Long,
    val state: DungeonEncounterState,
    val log: List<String>,
) {
    val enemyHealthFraction: Float get() = if (maxEnemyHealth <= 0) 0f else (enemyHealth.toFloat() / maxEnemyHealth)
    val defeated: Boolean get() = enemyHealth <= 0
}

/** The configurable scaling result for a party against an encounter. */
data class PartyScaling(
    val scaledEnemyHealth: Long,
    val expectedContributionPerMember: Long,
    val recommendedDurationSeconds: Long,
    val difficulty: DifficultyRating,
    val rewardMultiplier: Double,
)

/** The reward for a completed encounter (idempotent per encounter+member at award time). */
data class DungeonReward(
    val encounterId: String,
    val playerXp: Long,
    val perMemberXp: Map<String, Long>,
    val rewardMultiplier: Double,
    val alreadyAwarded: Boolean = false,
)

/** Whether a member is ready to attempt a solo encounter (from real fitness evidence). */
data class SoloReadiness(
    val eligible: Boolean,
    val reasons: List<String>,
    val missing: List<String>,
)

/** The real-fitness inputs for solo readiness — Class Level is intentionally NOT among them. */
data class SoloReadinessInput(
    val activeWorkoutSeconds: Long,
    val trainingTier: Int,
    val recentQualifyingActivity: Boolean,
    val unlockedSkillIds: Set<String>,
    val recentProgressionEvents: Int,
    val safetyStopActive: Boolean,
)
