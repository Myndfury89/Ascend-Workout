package com.ascend.core.data.mapper

import com.ascend.core.database.entity.QuestTemplateEntity
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestTemplate

private fun joinInts(values: List<Int>): String = values.joinToString(",")

private fun splitInts(raw: String): List<Int> = raw.split(",").mapNotNull { it.trim().toIntOrNull() }

private fun joinStrings(values: List<String>): String = values.joinToString("|")

private fun splitStrings(raw: String): List<String> = raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }

fun QuestTemplateEntity.toDomain(): QuestTemplate =
    QuestTemplate(
        id = id,
        name = name,
        objectiveType = runCatching { ObjectiveType.valueOf(objectiveType) }.getOrDefault(ObjectiveType.REPETITIONS),
        unit = unit,
        primaryAttribute = primaryAttribute?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() },
        exerciseId = exerciseId,
        minimumTarget = minimumTarget,
        maximumTarget = maximumTarget,
        defaultTarget = defaultTarget,
        targetStep = targetStep,
        defaultQuickAddValues = splitInts(defaultQuickAddValues),
        defaultPreferredSetSize = defaultPreferredSetSize,
        minimumAllowedSetSize = minimumAllowedSetSize,
        maximumAllowedSetSize = maximumAllowedSetSize,
        supportsAutomaticProgress = supportsAutomaticProgress,
        supportsManualProgress = supportsManualProgress,
        supportedVariations = splitStrings(supportedVariations),
        safetyWarningThreshold = safetyWarningThreshold,
        baseRewardXp = baseRewardXp,
        isBuiltIn = isBuiltIn,
    )

fun QuestTemplate.toEntity(now: Long): QuestTemplateEntity =
    QuestTemplateEntity(
        id = id,
        name = name,
        objectiveType = objectiveType.name,
        unit = unit,
        primaryAttribute = primaryAttribute?.name,
        exerciseId = exerciseId,
        minimumTarget = minimumTarget,
        maximumTarget = maximumTarget,
        defaultTarget = defaultTarget,
        targetStep = targetStep,
        defaultQuickAddValues = joinInts(defaultQuickAddValues),
        defaultPreferredSetSize = defaultPreferredSetSize,
        minimumAllowedSetSize = minimumAllowedSetSize,
        maximumAllowedSetSize = maximumAllowedSetSize,
        supportsAutomaticProgress = supportsAutomaticProgress,
        supportsManualProgress = supportsManualProgress,
        supportedVariations = joinStrings(supportedVariations),
        safetyWarningThreshold = safetyWarningThreshold,
        baseRewardXp = baseRewardXp,
        isBuiltIn = isBuiltIn,
        createdAt = now,
        updatedAt = now,
    )
