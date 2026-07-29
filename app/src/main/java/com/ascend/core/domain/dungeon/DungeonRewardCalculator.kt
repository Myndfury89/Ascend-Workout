package com.ascend.core.domain.dungeon

import com.ascend.core.model.DungeonDefinition
import com.ascend.core.model.DungeonParty
import com.ascend.core.model.DungeonReward
import com.ascend.core.model.PartyScaling
import javax.inject.Inject

/**
 * Computes the reward for a completed encounter, split across members in proportion to the
 * contribution each **earned** (which is retained even if a member went inactive). Idempotent:
 * when [alreadyAwarded] is true it returns a zero, flagged reward so an encounter can never pay out
 * twice.
 */
class DungeonRewardCalculator
    @Inject
    constructor(private val config: DungeonConfig) {
        constructor() : this(DungeonConfig())

        fun award(
            encounterId: String,
            definition: DungeonDefinition,
            party: DungeonParty,
            scaling: PartyScaling,
            alreadyAwarded: Boolean,
        ): DungeonReward {
            if (alreadyAwarded) {
                return DungeonReward(encounterId, 0, emptyMap(), scaling.rewardMultiplier, alreadyAwarded = true)
            }
            val total = (definition.baseRewardXp * scaling.rewardMultiplier * config.baseRewardMultiplier).toLong()
            val totalContribution = party.members.sumOf { it.earnedContribution }.coerceAtLeast(1)
            val perMember =
                party.members.associate { member ->
                    member.id to (total * member.earnedContribution / totalContribution)
                }
            return DungeonReward(encounterId, total, perMember, scaling.rewardMultiplier, alreadyAwarded = false)
        }
    }
