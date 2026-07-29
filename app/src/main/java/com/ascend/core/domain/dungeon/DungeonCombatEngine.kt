package com.ascend.core.domain.dungeon

import com.ascend.core.model.ContributionType
import com.ascend.core.model.DungeonCombatState
import com.ascend.core.model.DungeonEncounterState
import com.ascend.core.model.DungeonEnemy
import com.ascend.core.model.EnemyPhase
import com.ascend.core.model.WorkoutContribution
import javax.inject.Inject
import kotlin.math.min

/** Resolves which enemy phase is active for a given health fraction (deterministic). */
class EnemyPhaseEngine
    @Inject
    constructor() {
        fun phaseFor(
            enemy: DungeonEnemy,
            healthFraction: Float,
        ): EnemyPhase {
            val ordered = enemy.phases.sortedByDescending { it.healthThresholdFraction }
            return ordered.lastOrNull { healthFraction <= it.healthThresholdFraction } ?: ordered.first()
        }
    }

/**
 * The deterministic combat engine. Applying a [WorkoutContribution] to a [DungeonCombatState]
 * always yields the same result — no randomness anywhere. Damage is a fixed function of the
 * contribution type, the enemy's weakness/armor, and the current phase's resistance; support
 * contributions add shield/stamina instead. Visuals only render this state; they never compute it.
 */
class DungeonCombatEngine
    @Inject
    constructor(
        private val config: DungeonConfig,
        private val phaseEngine: EnemyPhaseEngine,
    ) {
        constructor() : this(DungeonConfig(), EnemyPhaseEngine())

        fun initialState(scaledHealth: Long): DungeonCombatState =
            DungeonCombatState(
                enemyIndex = 0,
                enemyHealth = scaledHealth,
                maxEnemyHealth = scaledHealth,
                phaseIndex = 0,
                partyShield = 0,
                partyStamina = 0,
                totalDamageDealt = 0,
                state = DungeonEncounterState.IN_PROGRESS,
                log = emptyList(),
            )

        fun apply(
            state: DungeonCombatState,
            enemy: DungeonEnemy,
            contribution: WorkoutContribution,
        ): DungeonCombatState {
            if (state.state != DungeonEncounterState.IN_PROGRESS) return state
            val phase = phaseEngine.phaseFor(enemy, state.enemyHealthFraction)

            if (contribution.type.isSupport) {
                return state.copy(
                    partyShield = state.partyShield + config.shieldPerRecovery,
                    partyStamina = state.partyStamina + config.staminaPerRecovery,
                    phaseIndex = phase.index,
                    log = state.log + "${contribution.memberId}: ${contribution.type} (+shield/stamina)",
                )
            }

            val damage = damageFor(contribution, enemy, phase)
            val newHealth = (state.enemyHealth - damage).coerceAtLeast(0)
            val defeated = newHealth <= 0
            return state.copy(
                enemyHealth = newHealth,
                phaseIndex =
                    phaseEngine.phaseFor(
                        enemy,
                        if (state.maxEnemyHealth <= 0) 0f else newHealth.toFloat() / state.maxEnemyHealth,
                    ).index,
                totalDamageDealt = state.totalDamageDealt + damage,
                state = if (defeated) DungeonEncounterState.VICTORY else DungeonEncounterState.IN_PROGRESS,
                log = state.log + "${contribution.memberId}: ${contribution.type} -$damage",
            )
        }

        private fun damageFor(
            c: WorkoutContribution,
            enemy: DungeonEnemy,
            phase: EnemyPhase,
        ): Long {
            val typeMult =
                when (c.type) {
                    ContributionType.HEAVY_ATTACK -> config.heavyMultiplier
                    ContributionType.ARMOR_BREAK -> config.armorBreakMultiplier
                    ContributionType.SUSTAINED_DAMAGE -> config.sustainedMultiplier
                    ContributionType.STAMINA_DRAIN -> config.staminaDrainMultiplier
                    ContributionType.CRITICAL_STRIKE -> config.criticalMultiplier
                    ContributionType.COMBO -> config.comboMultiplier
                    ContributionType.COUNTER -> config.counterMultiplier
                    else -> 1.0
                }
            val weaknessMult = if (c.elementTag in enemy.weaknessTags) config.weaknessMultiplier else 1.0
            // Armor mitigates a fraction of damage unless the contribution is armor-breaking.
            val armorFactor = if (c.armorBreaking) 1.0 else 1.0 - min(MAX_ARMOR_MITIGATION, enemy.armor / (enemy.armor + ARMOR_SOFTNESS))
            val phaseFactor = 1.0 - phase.damageResistance
            val raw = c.magnitude * typeMult * weaknessMult * armorFactor * phaseFactor
            return raw.toLong().coerceAtLeast(1)
        }

        private companion object {
            const val MAX_ARMOR_MITIGATION = 0.6
            const val ARMOR_SOFTNESS = 1000.0
        }
    }
