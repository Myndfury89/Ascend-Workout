package com.ascend.core.domain.dungeon

import com.ascend.core.domain.skill.SkillCatalog
import com.ascend.core.model.ContributionType
import com.ascend.core.model.WorkoutActionType
import com.ascend.core.model.WorkoutContribution
import javax.inject.Inject

/** One real workout action to be mapped into a combat contribution (deterministic input). */
data class WorkoutActionInput(
    val memberId: String,
    val type: WorkoutActionType,
    val metricValue: Long,
    val sourceType: String,
    val sourceId: String,
    val skillIds: Set<String> = emptySet(),
)

/**
 * Maps a real workout action to a deterministic [WorkoutContribution]. Skills the member has
 * unlocked shape the mapping (Strength Boost → armor-breaking heavy attacks; Breath Control →
 * stronger sustained output; Body Awareness → technique counters) but the mapping is pure and
 * repeatable — the same input always yields the same contribution.
 */
class WorkoutContributionMapper
    @Inject
    constructor(private val config: DungeonConfig) {
        constructor() : this(DungeonConfig())

        fun map(input: WorkoutActionInput): WorkoutContribution {
            val hasStrength = SkillCatalog.STRENGTH_BOOST in input.skillIds
            val hasBreath = SkillCatalog.BREATH_CONTROL in input.skillIds
            val hasBody = SkillCatalog.BODY_AWARENESS in input.skillIds

            return when (input.type) {
                WorkoutActionType.STRENGTH_SET ->
                    contribution(
                        input,
                        ContributionType.HEAVY_ATTACK,
                        DungeonElements.PHYSICAL,
                        magnitude = scaled(input.metricValue, if (hasStrength) STRENGTH_SKILL_BONUS else 1.0),
                        armorBreaking = hasStrength,
                    )
                WorkoutActionType.CARDIO_DURATION ->
                    contribution(
                        input,
                        ContributionType.SUSTAINED_DAMAGE,
                        DungeonElements.SUSTAINED,
                        magnitude = scaled(input.metricValue / SECONDS_PER_POWER, if (hasBreath) BREATH_SKILL_BONUS else 1.0),
                    )
                WorkoutActionType.PERSONAL_RECORD ->
                    contribution(input, ContributionType.CRITICAL_STRIKE, DungeonElements.CRITICAL, magnitude = input.metricValue)
                WorkoutActionType.QUEST_INTERVAL ->
                    contribution(input, ContributionType.COMBO, DungeonElements.COMBO, magnitude = input.metricValue)
                WorkoutActionType.MOBILITY_OR_AGILITY ->
                    contribution(
                        input,
                        ContributionType.COUNTER,
                        DungeonElements.TECHNIQUE,
                        magnitude = scaled(input.metricValue, if (hasBody) BODY_SKILL_BONUS else 1.0),
                    )
                WorkoutActionType.RECOVERY ->
                    contribution(input, ContributionType.SHIELD, DungeonElements.RESTORATIVE, magnitude = config.shieldPerRecovery)
            }
        }

        private fun scaled(
            base: Long,
            mult: Double,
        ): Long = (base.coerceAtLeast(0) * mult).toLong()

        private fun contribution(
            input: WorkoutActionInput,
            type: ContributionType,
            element: String,
            magnitude: Long,
            armorBreaking: Boolean = false,
        ) = WorkoutContribution(
            memberId = input.memberId,
            type = type,
            magnitude = magnitude,
            elementTag = element,
            sourceType = input.sourceType,
            sourceId = input.sourceId,
            armorBreaking = armorBreaking,
        )

        private companion object {
            const val STRENGTH_SKILL_BONUS = 1.15
            const val BREATH_SKILL_BONUS = 1.10
            const val BODY_SKILL_BONUS = 1.15
            const val SECONDS_PER_POWER = 6L
        }
    }
