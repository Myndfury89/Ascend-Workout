package com.ascend.core.domain.dungeon

import com.ascend.core.model.DifficultyRating
import com.ascend.core.model.PartyScaling
import javax.inject.Inject

/** The party + encounter inputs the scaling reads (all real/derived; configurable). */
data class PartyScalingInput(
    val baseEnemyHealth: Long,
    val encounterTier: Int,
    val memberCount: Int,
    val activeMemberCount: Int,
    val averageTrainingTier: Int,
    val distinctClasses: Int,
    val distinctSkills: Int,
    val recentActivityFraction: Double,
    val solo: Boolean,
)

/**
 * Scales an encounter to a party. More members raise total enemy health only **sub-linearly**, so
 * each member's expected burden falls while the encounter never becomes trivial. Class/Skill
 * diversity and recent activity nudge the difficulty rating and reward multiplier. Fully data-driven.
 */
class PartyScalingCalculator
    @Inject
    constructor(private val config: DungeonConfig) {
        constructor() : this(DungeonConfig())

        fun scale(input: PartyScalingInput): PartyScaling {
            val members = input.memberCount.coerceAtLeast(1)
            val active = input.activeMemberCount.coerceIn(1, members)

            var health = input.baseEnemyHealth + config.baseHealthPerTier * input.encounterTier
            // Sub-linear growth: total rises with members, but slower than 1:1 so per-member drops.
            health = (health * (1.0 + config.perExtraMemberHealth * (members - 1))).toLong()
            if (input.solo) health = (health * config.soloHealthDiscount).toLong()

            val perMember = health / active
            val capacity = (input.averageTrainingTier.coerceAtLeast(1) * CAPACITY_PER_TIER).toDouble()
            val ratio = perMember / capacity
            val difficulty =
                when {
                    ratio < TRIVIAL_RATIO -> DifficultyRating.TRIVIAL
                    ratio < MODERATE_RATIO -> DifficultyRating.MODERATE
                    ratio < HARD_RATIO -> DifficultyRating.HARD
                    else -> DifficultyRating.SEVERE
                }
            val recommendedDuration = (perMember / CONTRIBUTION_PER_SECOND).coerceAtLeast(MIN_DURATION_SECONDS)
            // A small teamwork bonus for diversity + sustained activity; never inflates per person much.
            val rewardMultiplier =
                config.baseRewardMultiplier +
                    (members - 1) * config.rewardPerMemberBonus +
                    input.distinctClasses * DIVERSITY_BONUS +
                    input.recentActivityFraction * ACTIVITY_BONUS

            return PartyScaling(
                scaledEnemyHealth = health,
                expectedContributionPerMember = perMember,
                recommendedDurationSeconds = recommendedDuration,
                difficulty = difficulty,
                rewardMultiplier = rewardMultiplier,
            )
        }

        private companion object {
            const val CAPACITY_PER_TIER = 900L
            const val CONTRIBUTION_PER_SECOND = 4L
            const val MIN_DURATION_SECONDS = 60L
            const val TRIVIAL_RATIO = 0.8
            const val MODERATE_RATIO = 1.3
            const val HARD_RATIO = 2.0
            const val DIVERSITY_BONUS = 0.03
            const val ACTIVITY_BONUS = 0.05
        }
    }
