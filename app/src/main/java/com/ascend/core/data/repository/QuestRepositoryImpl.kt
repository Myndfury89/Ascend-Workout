package com.ascend.core.data.repository

import androidx.room.withTransaction
import com.ascend.core.common.newId
import com.ascend.core.data.mapper.toDomain
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.AddProgressResult
import com.ascend.core.domain.repository.CompleteQuestResult
import com.ascend.core.domain.repository.NewQuestSpec
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.domain.repository.XpAwardResult
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestStatus
import com.ascend.core.model.RewardBreakdown
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestRepositoryImpl
    @Inject
    constructor(
        private val db: AscendDatabase,
        private val questDao: QuestDao,
        private val exerciseDao: ExerciseDao,
        private val progressionRepository: ProgressionRepository,
        private val xpCalculator: XpCalculator,
        private val attributeCalculator: AttributeProgressCalculator,
        private val classRewardApplier: ClassRewardApplier,
    ) : QuestRepository {
        private fun now() = System.currentTimeMillis()

        override fun observeQuestsForUser(userId: String): Flow<List<Quest>> =
            questDao.observeQuestsWithObjectivesForUser(userId).map { list -> list.map { it.toDomain() } }

        override fun observeQuest(questId: String): Flow<Quest?> = questDao.observeQuestWithObjectives(questId).map { it?.toDomain() }

        override suspend fun getQuest(questId: String): Quest? = questDao.getQuestWithObjectives(questId)?.toDomain()

        override suspend fun createQuest(spec: NewQuestSpec): String =
            db.withTransaction {
                val questId = newId()
                val ts = now()
                questDao.upsertQuest(
                    QuestEntity(
                        id = questId, userId = spec.userId, title = spec.title, description = spec.description,
                        questType = spec.type.name, scheduledDate = spec.scheduledDate, deadline = spec.deadline,
                        difficulty = spec.difficulty.name, status = QuestStatus.ACTIVE.name,
                        baseRewardXp = spec.baseRewardXp, partialRewardEnabled = spec.partialRewardEnabled,
                        overCompletionEnabled = spec.overCompletionEnabled, createdAt = ts, updatedAt = ts,
                    ),
                )
                spec.objectives.forEach { o ->
                    questDao.upsertObjective(
                        QuestObjectiveEntity(
                            id = newId(), questId = questId, exerciseId = o.exerciseId, title = o.title,
                            objectiveType = o.type.name, targetValue = o.target, currentValue = 0.0, unit = o.unit,
                            primaryAttribute = o.primaryAttribute?.name, preferredSetSize = o.preferredSetSize,
                            minimumSetSize = o.minimumSetSize, maximumSetSize = o.maximumSetSize,
                            completionRule = "ALL_REQUIRED", orderIndex = o.orderIndex, status = QuestStatus.ACTIVE.name,
                        ),
                    )
                }
                questId
            }

        override suspend fun addProgress(
            objectiveId: String,
            value: Double,
            source: com.ascend.core.model.ProgressSource,
            note: String?,
            perceivedEffort: Int?,
            sourceApplication: String?,
            externalRecordId: String?,
        ): AddProgressResult {
            val objective = questDao.getObjective(objectiveId) ?: return AddProgressResult.NotFound
            return db.withTransaction {
                val entryId = newId()
                val ts = now()
                val row =
                    questDao.insertProgressEntry(
                        QuestProgressEntryEntity(
                            id = entryId, questId = objective.questId, objectiveId = objectiveId, value = value,
                            source = source.name, sourceApplication = sourceApplication,
                            externalRecordId = externalRecordId, completedAt = ts, perceivedEffort = perceivedEffort,
                            note = note, createdAt = ts, updatedAt = ts,
                        ),
                    )
                if (row == -1L) return@withTransaction AddProgressResult.Duplicate
                val newCurrent = recomputeObjective(objectiveId)
                questDao.updateQuestStatus(objective.questId, QuestStatus.IN_PROGRESS.name, ts)
                AddProgressResult.Added(entryId, newCurrent, newCurrent >= objective.targetValue)
            }
        }

        override suspend fun editProgress(
            entryId: String,
            newValue: Double,
        ): Boolean {
            val entry = questDao.getProgressEntry(entryId) ?: return false
            db.withTransaction {
                questDao.updateProgressEntryValue(entryId, newValue, now())
                recomputeObjective(entry.objectiveId)
            }
            return true
        }

        override suspend fun deleteProgress(entryId: String): Boolean {
            val entry = questDao.getProgressEntry(entryId) ?: return false
            db.withTransaction {
                questDao.deleteProgressEntry(entryId)
                recomputeObjective(entry.objectiveId)
            }
            return true
        }

        override suspend fun completeQuest(questId: String): CompleteQuestResult {
            val qwo = questDao.getQuestWithObjectives(questId) ?: return CompleteQuestResult.NotFound
            return db.withTransaction {
                val quest = qwo.quest
                val difficulty = runCatching { Difficulty.valueOf(quest.difficulty) }.getOrDefault(Difficulty.MODERATE)
                val totalTarget = qwo.objectives.sumOf { it.objective.targetValue }
                val totalCurrent = qwo.objectives.sumOf { it.objective.currentValue }
                val fraction = if (totalTarget <= 0.0) 1.0 else totalCurrent / totalTarget
                val overFraction = if (totalTarget <= 0.0) 0.0 else ((totalCurrent - totalTarget) / totalTarget).coerceAtLeast(0.0)

                val xp =
                    xpCalculator.questXp(
                        baseReward = quest.baseRewardXp,
                        completionFraction = fraction,
                        overCompletionFraction = overFraction,
                        partialEnabled = quest.partialRewardEnabled,
                        overCompletionEnabled = quest.overCompletionEnabled,
                    )

                val xpOutcome =
                    progressionRepository.awardXp(
                        userId = quest.userId,
                        amount = xp.total,
                        sourceType = XpSourceType.QUEST_COMPLETION,
                        sourceId = questId,
                        description = "Quest: ${quest.title}",
                    )

                val newStatus =
                    when {
                        fraction > 1.0 && quest.overCompletionEnabled -> QuestStatus.OVER_COMPLETED
                        fraction >= 1.0 -> QuestStatus.COMPLETED
                        else -> QuestStatus.PARTIALLY_COMPLETED
                    }
                questDao.updateQuestStatus(questId, newStatus.name, now())
                qwo.objectives.forEach { owe ->
                    val done = owe.objective.currentValue >= owe.objective.targetValue
                    questDao.updateObjectiveProgress(
                        owe.objective.id,
                        owe.objective.currentValue,
                        if (done) QuestStatus.COMPLETED.name else QuestStatus.PARTIALLY_COMPLETED.name,
                    )
                }

                if (xpOutcome is XpAwardResult.Duplicate) {
                    return@withTransaction CompleteQuestResult.AlreadyCompleted
                }

                // Base attribute distribution the activity trains, before any class shaping.
                val baseDeltas = LinkedHashMap<AttributeType, Long>()
                qwo.objectives.forEach { owe ->
                    val attribute =
                        owe.objective.primaryAttribute
                            ?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() }
                    if (attribute != null) {
                        val points = attributeCalculator.volumePoints(owe.objective.currentValue, difficulty)
                        if (points > 0) baseDeltas[attribute] = (baseDeltas[attribute] ?: 0L) + points
                    }
                }
                baseDeltas[AttributeType.DISCIPLINE] =
                    (baseDeltas[AttributeType.DISCIPLINE] ?: 0L) + attributeCalculator.disciplinePoints(difficulty)

                val awarded = xpOutcome as XpAwardResult.Awarded
                val activityTags = resolveTags(qwo.objectives.mapNotNull { it.objective.exerciseId })

                // Class specialization shapes attributes + Class XP + unique proficiency;
                // Player XP (above) stays class-neutral. Pass-through when no class is set.
                val outcome =
                    classRewardApplier.apply(
                        userId = quest.userId,
                        basePlayerXp = awarded.amount,
                        baseAttributeDistribution = baseDeltas,
                        activityTags = activityTags,
                        sourceType = XpSourceType.QUEST_COMPLETION,
                        sourceId = questId,
                    )

                progressionRepository.awardAttributes(
                    quest.userId,
                    outcome.awardedAttributeProficiency,
                    XpSourceType.QUEST_COMPLETION,
                    questId,
                )

                val breakdown =
                    RewardBreakdown(
                        basePlayerXp = awarded.amount,
                        playerLeveledUp = awarded.leveledUp,
                        newPlayerLevel = awarded.newLevel,
                        baseAttributeDistribution = baseDeltas,
                        awardedAttributeProficiency = outcome.awardedAttributeProficiency,
                        primaryClass = outcome.primaryClass,
                        secondaryClass = outcome.secondaryClass,
                    )
                CompleteQuestResult.Completed(
                    awarded.amount,
                    awarded.newLevel,
                    awarded.leveledUp,
                    outcome.awardedAttributeProficiency,
                    breakdown,
                )
            }
        }

        /** Union of activity tags for the linked exercises (empty -> neutral affinity). */
        private suspend fun resolveTags(exerciseIds: List<String>): Set<String> {
            val out = LinkedHashSet<String>()
            exerciseIds.distinct().forEach { id ->
                exerciseDao.getById(id)?.tags
                    ?.split(",")
                    ?.forEach { tag -> tag.trim().takeIf { it.isNotEmpty() }?.let(out::add) }
            }
            return out
        }

        /** Sum entries -> objective.currentValue + status. The entries are the source of truth. */
        private suspend fun recomputeObjective(objectiveId: String): Double {
            val sum = questDao.sumProgress(objectiveId)
            val objective = questDao.getObjective(objectiveId) ?: return sum
            val status =
                when {
                    sum >= objective.targetValue -> QuestStatus.COMPLETED
                    sum > 0.0 -> QuestStatus.IN_PROGRESS
                    else -> QuestStatus.ACTIVE
                }
            questDao.updateObjectiveProgress(objectiveId, sum, status.name)
            return sum
        }
    }
