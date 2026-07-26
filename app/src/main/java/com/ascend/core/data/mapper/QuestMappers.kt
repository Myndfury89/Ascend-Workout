package com.ascend.core.data.mapper

import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.relation.ObjectiveWithEntries
import com.ascend.core.database.relation.QuestWithObjectives
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.ProgressSource
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestObjective
import com.ascend.core.model.QuestProgressEntry
import com.ascend.core.model.QuestStatus
import com.ascend.core.model.QuestType

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

fun QuestProgressEntryEntity.toDomain(): QuestProgressEntry =
    QuestProgressEntry(
        id = id,
        objectiveId = objectiveId,
        value = value,
        source = source.toEnum(ProgressSource.MANUAL),
        note = note,
        completedAt = completedAt,
        sourceApplication = sourceApplication,
        externalRecordId = externalRecordId,
    )

fun ObjectiveWithEntries.toDomain(): QuestObjective =
    QuestObjective(
        id = objective.id,
        questId = objective.questId,
        exerciseId = objective.exerciseId,
        title = objective.title,
        type = objective.objectiveType.toEnum(ObjectiveType.REPETITIONS),
        target = objective.targetValue,
        current = objective.currentValue,
        unit = objective.unit,
        preferredSetSize = objective.preferredSetSize,
        minimumSetSize = objective.minimumSetSize,
        maximumSetSize = objective.maximumSetSize,
        primaryAttribute =
            objective.primaryAttribute
                ?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() },
        status = objective.status.toEnum(QuestStatus.ACTIVE),
        orderIndex = objective.orderIndex,
        entries = entries.sortedBy { it.completedAt }.map { it.toDomain() },
    )

fun QuestWithObjectives.toDomain(): Quest =
    Quest(
        id = quest.id,
        userId = quest.userId,
        title = quest.title,
        description = quest.description,
        type = quest.questType.toEnum(QuestType.ACCUMULATION),
        status = quest.status.toEnum(QuestStatus.ACTIVE),
        scheduledDate = quest.scheduledDate,
        deadline = quest.deadline,
        difficulty = quest.difficulty.toEnum(Difficulty.MODERATE),
        baseRewardXp = quest.baseRewardXp,
        partialRewardEnabled = quest.partialRewardEnabled,
        overCompletionEnabled = quest.overCompletionEnabled,
        objectives = objectives.sortedBy { it.objective.orderIndex }.map { it.toDomain() },
    )
