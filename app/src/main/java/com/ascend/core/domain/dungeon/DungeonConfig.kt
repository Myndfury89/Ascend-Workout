package com.ascend.core.domain.dungeon

/**
 * All Dungeon balancing as tunable data — kept out of the engines. Timeouts govern the
 * activity/inactivity transitions; the combat coefficients keep the fight deterministic; the
 * scaling coefficients make more members reduce individual burden without trivializing an
 * encounter.
 */
data class DungeonConfig(
    // Activity / inactivity.
    val idleWarningMillis: Long = 45_000,
    val inactivityTimeoutMillis: Long = 90_000,
    // Combat (deterministic).
    val heavyMultiplier: Double = 1.4,
    val armorBreakMultiplier: Double = 1.25,
    val sustainedMultiplier: Double = 1.0,
    val staminaDrainMultiplier: Double = 0.6,
    val criticalMultiplier: Double = 2.0,
    val comboMultiplier: Double = 1.2,
    val counterMultiplier: Double = 0.9,
    val weaknessMultiplier: Double = 1.5,
    val shieldPerRecovery: Long = 40,
    val staminaPerRecovery: Long = 30,
    // Party scaling.
    val baseHealthPerTier: Long = 600,
    val perExtraMemberHealth: Double = 0.6,
    val soloHealthDiscount: Double = 0.55,
    val rewardPerMemberBonus: Double = 0.08,
    // Reward.
    val baseRewardMultiplier: Double = 1.0,
)
