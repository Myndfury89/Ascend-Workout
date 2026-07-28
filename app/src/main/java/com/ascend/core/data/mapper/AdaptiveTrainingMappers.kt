package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ExercisePrescriptionEntity
import com.ascend.core.database.entity.ProgressionMilestoneEntity
import com.ascend.core.database.entity.ProgressionRecommendationEntity
import com.ascend.core.database.entity.TrainingReadinessSnapshotEntity
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionMilestone
import com.ascend.core.model.ProgressionMilestoneType
import com.ascend.core.model.ProgressionRecommendation
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.ProgressionStrategy
import com.ascend.core.model.RecommendationStatus
import com.ascend.core.model.TrainingReadiness
import com.ascend.core.model.TrainingReadinessState

private fun encodeList(values: List<String>): String = values.joinToString("|")

private fun decodeList(raw: String): List<String> = raw.split("|").filter { it.isNotEmpty() }

fun ExercisePrescription.toEntity(now: Long): ExercisePrescriptionEntity =
    ExercisePrescriptionEntity(
        id = id, userId = userId, exerciseId = exerciseId, progressionStrategy = progressionStrategy.name,
        targetSets = targetSets, minimumReps = minimumReps, maximumReps = maximumReps, targetWeight = targetWeight,
        targetRestSeconds = targetRestSeconds, targetDurationSeconds = targetDurationSeconds, targetDistance = targetDistance,
        targetPace = targetPace, tempo = tempo, variationId = variationId, assistanceValue = assistanceValue,
        effectiveFrom = if (effectiveFrom == 0L) now else effectiveFrom, status = status, createdAt = now, updatedAt = now,
    )

fun ExercisePrescriptionEntity.toDomain(): ExercisePrescription =
    ExercisePrescription(
        id = id, userId = userId, exerciseId = exerciseId,
        progressionStrategy =
            runCatching {
                ProgressionStrategy.valueOf(
                    progressionStrategy,
                )
            }.getOrDefault(ProgressionStrategy.DOUBLE_PROGRESSION),
        targetSets = targetSets, minimumReps = minimumReps, maximumReps = maximumReps, targetWeight = targetWeight,
        targetRestSeconds = targetRestSeconds, targetDurationSeconds = targetDurationSeconds, targetDistance = targetDistance,
        targetPace = targetPace, tempo = tempo, variationId = variationId, assistanceValue = assistanceValue,
        effectiveFrom = effectiveFrom, status = status,
    )

fun TrainingReadiness.toEntity(
    id: String,
    userId: String,
    exerciseId: String?,
    questTemplateId: String?,
): TrainingReadinessSnapshotEntity =
    TrainingReadinessSnapshotEntity(
        id = id, userId = userId, exerciseId = exerciseId, questTemplateId = questTemplateId,
        readinessState = state.name, score = score, confidence = confidence,
        evidence = encodeList(evidence), positiveSignals = encodeList(positiveSignals),
        limitingSignals = encodeList(limitingSignals), missingSignals = encodeList(missingSignals),
        safetyState = safetyState.name, safetyFlags = encodeList(safetyFlags), createdAt = generatedAt,
    )

fun ProgressionRecommendation.toEntity(): ProgressionRecommendationEntity =
    ProgressionRecommendationEntity(
        id = id, userId = userId, recommendationType = recommendationType.name, exerciseId = exerciseId,
        questTemplateId = questTemplateId, currentPrescriptionId = currentPrescriptionId,
        proposedPrescriptionId = proposed?.id, proposedTarget = proposedTarget, readinessState = readinessState.name,
        reason = reason, evidence = encodeList(evidence), confidence = confidence, safetyState = safetyState.name,
        requiresConfirmation = requiresConfirmation, status = status.name, generatedAt = generatedAt, expiresAt = expiresAt,
        acceptedAt = acceptedAt, rejectedAt = rejectedAt, appliedAt = appliedAt,
    )

fun ProgressionRecommendationEntity.toDomain(proposed: ExercisePrescription?): ProgressionRecommendation =
    ProgressionRecommendation(
        id = id, userId = userId,
        recommendationType =
            runCatching { ProgressionRecommendationType.valueOf(recommendationType) }
                .getOrDefault(ProgressionRecommendationType.MAINTAIN_PRESCRIPTION),
        exerciseId = exerciseId, questTemplateId = questTemplateId, currentPrescriptionId = currentPrescriptionId,
        proposed = proposed, proposedTarget = proposedTarget,
        readinessState = runCatching { TrainingReadinessState.valueOf(readinessState) }.getOrDefault(TrainingReadinessState.MAINTAIN),
        reason = reason, evidence = decodeList(evidence), confidence = confidence,
        safetyState = runCatching { ProgressionSafetyState.valueOf(safetyState) }.getOrDefault(ProgressionSafetyState.OK),
        requiresConfirmation = requiresConfirmation,
        status = runCatching { RecommendationStatus.valueOf(status) }.getOrDefault(RecommendationStatus.PENDING),
        generatedAt = generatedAt, expiresAt = expiresAt, acceptedAt = acceptedAt, rejectedAt = rejectedAt, appliedAt = appliedAt,
    )

fun ProgressionMilestone.toEntity(milestoneKey: String): ProgressionMilestoneEntity =
    ProgressionMilestoneEntity(
        id = id, userId = userId, milestoneKey = milestoneKey, milestoneType = milestoneType.name,
        exerciseId = exerciseId, questTemplateId = questTemplateId, sourceRecommendationId = sourceRecommendationId,
        previousValue = previousValue, newValue = newValue, createdAt = createdAt,
    )

fun ProgressionMilestoneEntity.toDomain(): ProgressionMilestone =
    ProgressionMilestone(
        id = id, userId = userId,
        milestoneType = runCatching { ProgressionMilestoneType.valueOf(milestoneType) }.getOrDefault(ProgressionMilestoneType.REP_RECORD),
        exerciseId = exerciseId, questTemplateId = questTemplateId, sourceRecommendationId = sourceRecommendationId,
        previousValue = previousValue, newValue = newValue, createdAt = createdAt,
    )
