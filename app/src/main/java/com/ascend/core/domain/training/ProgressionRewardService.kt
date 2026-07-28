package com.ascend.core.domain.training

import androidx.room.withTransaction
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.AdaptiveTrainingDao
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionMilestone
import com.ascend.core.model.ProgressionMilestoneType
import com.ascend.core.model.ProgressionRewardBreakdown
import com.ascend.core.model.XpSourceType
import javax.inject.Inject

/** Player XP + attribute reward for a milestone type (balancing defaults, tunable). */
data class ProgressionRewardConfig(
    val loadIncreaseXp: Long = 120,
    val dailyQuestTargetXp: Long = 90,
    val repRecordXp: Long = 60,
    val setAddedXp: Long = 90,
    val restEfficiencyXp: Long = 50,
    val assistanceReducedXp: Long = 90,
    val tempoProgressedXp: Long = 70,
    val rangeOfMotionXp: Long = 60,
    val externalLoadAddedXp: Long = 110,
    val variationAdvancedXp: Long = 100,
    val cardioDurationXp: Long = 80,
    val cardioDistanceXp: Long = 90,
    val paceXp: Long = 90,
    val cardioResistanceXp: Long = 80,
    val intervalProgressedXp: Long = 90,
    val adaptivePlanCompletedXp: Long = 70,
    val deloadReturnXp: Long = 80,
    val defaultXp: Long = 60,
    val attributePoints: Long = 30,
)

data class ProgressionRewardSpec(
    val playerXp: Long,
    val attributes: Map<AttributeType, Long>,
)

/** Maps a milestone to its base (class‑neutral) reward. */
class ProgressionRewardCalculator
    @Inject
    constructor(private val config: ProgressionRewardConfig) {
        constructor() : this(ProgressionRewardConfig())

        fun rewardFor(
            milestoneType: ProgressionMilestoneType,
            primaryAttribute: AttributeType?,
        ): ProgressionRewardSpec {
            val xp =
                when (milestoneType) {
                    ProgressionMilestoneType.LOAD_INCREASE_COMPLETED -> config.loadIncreaseXp
                    ProgressionMilestoneType.DAILY_QUEST_TARGET_PROGRESSED -> config.dailyQuestTargetXp
                    ProgressionMilestoneType.REP_RECORD -> config.repRecordXp
                    ProgressionMilestoneType.SET_VOLUME_RECORD, ProgressionMilestoneType.SET_ADDED -> config.setAddedXp
                    ProgressionMilestoneType.REST_EFFICIENCY -> config.restEfficiencyXp
                    ProgressionMilestoneType.ASSISTANCE_REDUCED -> config.assistanceReducedXp
                    ProgressionMilestoneType.TEMPO_PROGRESSED -> config.tempoProgressedXp
                    ProgressionMilestoneType.RANGE_OF_MOTION_IMPROVED -> config.rangeOfMotionXp
                    ProgressionMilestoneType.EXTERNAL_LOAD_ADDED -> config.externalLoadAddedXp
                    ProgressionMilestoneType.VARIATION_ADVANCED -> config.variationAdvancedXp
                    ProgressionMilestoneType.CARDIO_DURATION_MILESTONE -> config.cardioDurationXp
                    ProgressionMilestoneType.DISTANCE_MILESTONE -> config.cardioDistanceXp
                    ProgressionMilestoneType.PACE_MILESTONE -> config.paceXp
                    ProgressionMilestoneType.CARDIO_RESISTANCE_MILESTONE -> config.cardioResistanceXp
                    ProgressionMilestoneType.INTERVAL_PROGRESSED -> config.intervalProgressedXp
                    ProgressionMilestoneType.ADAPTIVE_PLAN_COMPLETED -> config.adaptivePlanCompletedXp
                    ProgressionMilestoneType.DELOAD_RETURN -> config.deloadReturnXp
                    else -> config.defaultXp
                }
            val attribute = primaryAttribute ?: AttributeType.STRENGTH
            return ProgressionRewardSpec(xp, mapOf(attribute to config.attributePoints))
        }
    }

/**
 * Grants a progression reward **only after** a harder prescription is proven. Idempotent
 * on the milestone's identity (unique `milestoneKey`) — the same milestone never awards
 * twice. Player XP stays class‑neutral; class shaping flows through the existing
 * [ClassRewardApplier], reusing the class‑XP / attribute / unique‑proficiency ledgers.
 */
class ProgressionRewardService
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val dao: AdaptiveTrainingDao,
        private val progressionRepository: ProgressionRepository,
        private val classRewardApplier: ClassRewardApplier,
        private val rewardCalculator: ProgressionRewardCalculator,
    ) {
        suspend fun award(
            milestone: ProgressionMilestone,
            primaryAttribute: AttributeType?,
            activityTags: Set<String> = emptySet(),
        ): ProgressionRewardBreakdown =
            db.withTransaction {
                val key = milestoneKey(milestone)
                val row = dao.insertMilestone(milestone.toEntity(key))
                val spec = rewardCalculator.rewardFor(milestone.milestoneType, primaryAttribute)

                if (row == -1L) {
                    // Already awarded for this exact milestone — grant nothing more.
                    return@withTransaction ProgressionRewardBreakdown(
                        milestone.milestoneType,
                        0,
                        emptyMap(),
                        null,
                        null,
                        alreadyAwarded = true,
                    )
                }

                progressionRepository.awardXp(
                    userId = milestone.userId,
                    amount = spec.playerXp,
                    sourceType = XpSourceType.PERSONAL_RECORD,
                    sourceId = milestone.id,
                    description = "Progression: ${milestone.milestoneType.name}",
                )

                val outcome =
                    classRewardApplier.apply(
                        userId = milestone.userId,
                        basePlayerXp = spec.playerXp,
                        baseAttributeDistribution = spec.attributes,
                        activityTags = activityTags,
                        sourceType = XpSourceType.PERSONAL_RECORD,
                        sourceId = milestone.id,
                    )
                progressionRepository.awardAttributes(
                    milestone.userId,
                    outcome.awardedAttributeProficiency,
                    XpSourceType.PERSONAL_RECORD,
                    milestone.id,
                )

                ProgressionRewardBreakdown(
                    milestoneType = milestone.milestoneType,
                    playerXp = spec.playerXp,
                    attributeProficiency = outcome.awardedAttributeProficiency,
                    primaryClass = outcome.primaryClass,
                    secondaryClass = outcome.secondaryClass,
                    alreadyAwarded = false,
                )
            }

        private fun milestoneKey(m: ProgressionMilestone): String =
            listOf(
                m.userId,
                m.milestoneType.name,
                m.exerciseId ?: "-",
                m.questTemplateId ?: "-",
                m.sourceRecommendationId ?: "-",
                m.newValue.toString(),
            ).joinToString("|")
    }
