package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ExerciseVariationEdgeEntity
import com.ascend.core.database.entity.ExerciseVariationEntity
import com.ascend.core.model.AssistanceType
import com.ascend.core.model.ExerciseVariation
import com.ascend.core.model.ExerciseVariationProgressionEdge
import com.ascend.core.model.VariationProgressionType

private fun encodeTags(values: List<String>): String = values.joinToString(",")

private fun decodeTags(raw: String): List<String> = raw.split(",").filter { it.isNotBlank() }

fun ExerciseVariation.toEntity(now: Long): ExerciseVariationEntity =
    ExerciseVariationEntity(
        id = id, exerciseId = exerciseId, name = name, description = description, difficultyTier = difficultyTier,
        variationTags = encodeTags(variationTags), assistanceType = assistanceType.name, assistanceValue = assistanceValue,
        externalLoadSupported = externalLoadSupported, rangeOfMotionLevel = rangeOfMotionLevel, tempoProfile = tempoProfile,
        enabled = enabled, createdAt = if (createdAt == 0L) now else createdAt, updatedAt = now,
    )

fun ExerciseVariationEntity.toDomain(): ExerciseVariation =
    ExerciseVariation(
        id = id, exerciseId = exerciseId, name = name, description = description, difficultyTier = difficultyTier,
        variationTags = decodeTags(variationTags),
        assistanceType = runCatching { AssistanceType.valueOf(assistanceType) }.getOrDefault(AssistanceType.NONE),
        assistanceValue = assistanceValue, externalLoadSupported = externalLoadSupported,
        rangeOfMotionLevel = rangeOfMotionLevel, tempoProfile = tempoProfile, enabled = enabled,
        createdAt = createdAt, updatedAt = updatedAt,
    )

fun ExerciseVariationProgressionEdge.toEntity(): ExerciseVariationEdgeEntity =
    ExerciseVariationEdgeEntity(
        id = id, sourceVariationId = sourceVariationId, destinationVariationId = destinationVariationId,
        progressionType = progressionType.name, minimumSuccessfulExposures = minimumSuccessfulExposures,
        minimumCompletedReps = minimumCompletedReps, minimumCompletedSets = minimumCompletedSets, maximumRpe = maximumRpe,
        minimumRir = minimumRir, maximumAssistanceValue = maximumAssistanceValue, requiredRangeOfMotion = requiredRangeOfMotion,
        requiredTempoControl = requiredTempoControl, classUnlockRequirement = classUnlockRequirement, safetyNotes = safetyNotes,
        enabled = enabled,
    )

fun ExerciseVariationEdgeEntity.toDomain(): ExerciseVariationProgressionEdge =
    ExerciseVariationProgressionEdge(
        id = id, sourceVariationId = sourceVariationId, destinationVariationId = destinationVariationId,
        progressionType = runCatching { VariationProgressionType.valueOf(progressionType) }.getOrDefault(VariationProgressionType.ADVANCE),
        minimumSuccessfulExposures = minimumSuccessfulExposures, minimumCompletedReps = minimumCompletedReps,
        minimumCompletedSets = minimumCompletedSets, maximumRpe = maximumRpe, minimumRir = minimumRir,
        maximumAssistanceValue = maximumAssistanceValue, requiredRangeOfMotion = requiredRangeOfMotion,
        requiredTempoControl = requiredTempoControl, classUnlockRequirement = classUnlockRequirement, safetyNotes = safetyNotes,
        enabled = enabled,
    )
