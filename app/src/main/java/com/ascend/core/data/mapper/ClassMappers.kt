package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ClassDefinitionEntity
import com.ascend.core.database.entity.ClassHistoryEntity
import com.ascend.core.database.entity.PlayerClassEntity
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassHistory
import com.ascend.core.model.ClassPresentation
import com.ascend.core.model.ClassSlot
import com.ascend.core.model.PlayerClassSelection

private fun joinStrings(values: List<String>): String = values.joinToString(",")

private fun splitStrings(raw: String): List<String> = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun joinAttributes(values: List<AttributeType>): String = values.joinToString(",") { it.name }

private fun splitAttributes(raw: String): List<AttributeType> =
    splitStrings(raw).mapNotNull { runCatching { AttributeType.valueOf(it) }.getOrNull() }

private fun joinMultipliers(map: Map<AttributeType, Double>): String = map.entries.joinToString(",") { "${it.key.name}:${it.value}" }

private fun splitMultipliers(raw: String): Map<AttributeType, Double> =
    splitStrings(raw).mapNotNull { pair ->
        val parts = pair.split(":")
        val attr = parts.getOrNull(0)?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() }
        val value = parts.getOrNull(1)?.toDoubleOrNull()
        if (attr != null && value != null) attr to value else null
    }.toMap()

fun ClassDefinition.toEntity(now: Long): ClassDefinitionEntity =
    ClassDefinitionEntity(
        id = id,
        name = name,
        classTitle = classTitle,
        description = description,
        fitnessIdentity = fitnessIdentity,
        favoredWorkoutCategories = joinStrings(favoredWorkoutCategories),
        favoredTags = joinStrings(favoredTags.toList()),
        primaryAttributes = joinAttributes(primaryAttributes),
        secondaryAttributes = joinAttributes(secondaryAttributes),
        attributeMultipliers = joinMultipliers(attributeMultipliers),
        uniqueProficiencyKey = uniqueProficiencyKey,
        uniqueProficiencyName = uniqueProficiencyName,
        favoredClassXpMultiplier = favoredClassXpMultiplier,
        nonFavoredClassXpMultiplier = neutralClassXpMultiplier,
        statusThemeKey = presentation.statusThemeKey,
        frameVariantKey = presentation.frameVariantKey,
        accentTokenKey = presentation.accentTokenKey,
        proficiencyIconKey = presentation.proficiencyIconKey,
        idleEffectKey = presentation.idleEffectKey,
        progressionEffectKey = presentation.progressionEffectKey,
        classQuestTemplateIds = joinStrings(classQuestTemplateIds),
        expeditionTemplateIds = joinStrings(expeditionTemplateIds),
        achievementPathIds = joinStrings(achievementPathIds),
        titleIds = joinStrings(titleIds),
        enabled = enabled,
        createdAt = if (createdAt == 0L) now else createdAt,
        updatedAt = now,
    )

fun ClassDefinitionEntity.toDomain(): ClassDefinition =
    ClassDefinition(
        id = id,
        name = name,
        classTitle = classTitle,
        description = description,
        fitnessIdentity = fitnessIdentity,
        favoredWorkoutCategories = splitStrings(favoredWorkoutCategories),
        favoredTags = splitStrings(favoredTags).toSet(),
        primaryAttributes = splitAttributes(primaryAttributes),
        secondaryAttributes = splitAttributes(secondaryAttributes),
        attributeMultipliers = splitMultipliers(attributeMultipliers),
        uniqueProficiencyKey = uniqueProficiencyKey,
        uniqueProficiencyName = uniqueProficiencyName,
        favoredClassXpMultiplier = favoredClassXpMultiplier,
        neutralClassXpMultiplier = nonFavoredClassXpMultiplier,
        presentation =
            ClassPresentation(
                statusThemeKey = statusThemeKey,
                frameVariantKey = frameVariantKey,
                accentTokenKey = accentTokenKey,
                proficiencyIconKey = proficiencyIconKey,
                idleEffectKey = idleEffectKey,
                progressionEffectKey = progressionEffectKey,
            ),
        classQuestTemplateIds = splitStrings(classQuestTemplateIds),
        expeditionTemplateIds = splitStrings(expeditionTemplateIds),
        achievementPathIds = splitStrings(achievementPathIds),
        titleIds = splitStrings(titleIds),
        enabled = enabled,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun PlayerClassEntity?.toSelection(userId: String): PlayerClassSelection =
    PlayerClassSelection(
        userId = userId,
        primaryClassId = this?.primaryClassId,
        secondaryClassId = this?.secondaryClassId,
        primaryStartedAt = this?.primaryStartedAt,
        secondaryStartedAt = this?.secondaryStartedAt,
        selectionReason = this?.selectionReason,
        changeSource = this?.changeSource,
        cooldownUntil = this?.cooldownUntil,
        respecQuestId = this?.respecQuestId,
    )

fun ClassHistoryEntity.toDomain(): ClassHistory =
    ClassHistory(
        id = id,
        userId = userId,
        classId = classId,
        slot = runCatching { ClassSlot.valueOf(slot) }.getOrDefault(ClassSlot.PRIMARY),
        startedAt = startedAt,
        endedAt = endedAt,
        selectionReason = selectionReason,
        changeSource = changeSource,
    )
