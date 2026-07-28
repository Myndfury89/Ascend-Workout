package com.ascend.core.domain.training

import com.ascend.core.common.newId
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionRecommendation
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.QuestTemplate
import com.ascend.core.model.ReadinessCheckIn
import com.ascend.core.model.RecommendationStatus
import com.ascend.core.model.SessionPerformance
import com.ascend.core.model.TrainingReadiness
import com.ascend.core.model.TrainingReadinessState
import javax.inject.Inject

private const val RECOMMENDATION_TTL_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Turns resistance readiness into a single, explainable recommendation. Load only
 * increases when readiness says so (real performance); a rep‑progression state keeps
 * the same prescription and nudges toward the top of the range.
 */
class ExerciseProgressionEngine
    @Inject
    constructor(
        private val readinessCalculator: TrainingReadinessCalculator,
        private val loadProgressionCalculator: LoadProgressionCalculator,
    ) {
        fun recommend(
            userId: String,
            prescription: ExercisePrescription,
            sessions: List<SessionPerformance>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            equipmentKey: String? = null,
            autoSafeAdaptation: Boolean = false,
            now: Long = System.currentTimeMillis(),
        ): ProgressionRecommendation {
            val readiness = readinessCalculator.evaluateResistance(sessions, prescription, checkIn, now)

            var proposed: ExercisePrescription? = null
            val type =
                when (readiness.state) {
                    TrainingReadinessState.READY_FOR_LOAD_PROGRESSION -> {
                        proposed = loadProgressionCalculator.nextLoadPrescription(prescription, equipmentKey).copy(userId = userId)
                        ProgressionRecommendationType.INCREASE_WEIGHT
                    }
                    TrainingReadinessState.READY_FOR_REP_PROGRESSION -> ProgressionRecommendationType.INCREASE_REPS
                    TrainingReadinessState.DELOAD_RECOMMENDED -> ProgressionRecommendationType.DELOAD
                    TrainingReadinessState.REGRESS -> ProgressionRecommendationType.REGRESS_VARIATION
                    TrainingReadinessState.INSUFFICIENT_DATA -> ProgressionRecommendationType.REQUEST_MORE_DATA
                    else -> ProgressionRecommendationType.MAINTAIN_PRESCRIPTION
                }

            return build(
                userId = userId, type = type, readiness = readiness,
                exerciseId = prescription.exerciseId, questTemplateId = null,
                currentPrescriptionId = prescription.id, proposed = proposed, proposedTarget = null,
                autoSafeAdaptation = autoSafeAdaptation, now = now,
            )
        }
    }

/**
 * Turns Daily Quest readiness (from the recent baseline) into a target recommendation,
 * always capped by the template's configurable min/max. Never changes the target
 * itself — it produces a recommendation for the user to accept.
 */
class QuestProgressionEngine
    @Inject
    constructor(
        private val baselineCalculator: RecentBaselineCalculator,
        private val readinessCalculator: TrainingReadinessCalculator,
        private val targetCalculator: DailyQuestTargetProgressionCalculator,
    ) {
        fun recommend(
            userId: String,
            template: QuestTemplate,
            outcomes: List<com.ascend.core.model.QuestOutcome>,
            checkIn: ReadinessCheckIn = ReadinessCheckIn(),
            autoSafeAdaptation: Boolean = false,
            now: Long = System.currentTimeMillis(),
        ): ProgressionRecommendation {
            val baseline = baselineCalculator.calculate(outcomes)
            val readiness = readinessCalculator.evaluateDailyQuest(baseline, checkIn, now)

            var proposedTarget: Int? = null
            val type =
                when {
                    readiness.state == TrainingReadinessState.READY_FOR_REP_PROGRESSION && baseline != null -> {
                        proposedTarget = targetCalculator.increasedTarget(baseline, template)
                        ProgressionRecommendationType.INCREASE_DAILY_QUEST_TARGET
                    }
                    readiness.state == TrainingReadinessState.DELOAD_RECOMMENDED && baseline != null -> {
                        proposedTarget = targetCalculator.reducedTarget(baseline, template)
                        ProgressionRecommendationType.REDUCE_DAILY_QUEST_TARGET
                    }
                    readiness.state == TrainingReadinessState.INSUFFICIENT_DATA -> ProgressionRecommendationType.REQUEST_MORE_DATA
                    else -> ProgressionRecommendationType.MAINTAIN_PRESCRIPTION
                }

            return build(
                userId = userId, type = type, readiness = readiness,
                exerciseId = template.exerciseId, questTemplateId = template.id,
                currentPrescriptionId = null, proposed = null, proposedTarget = proposedTarget,
                autoSafeAdaptation = autoSafeAdaptation, now = now,
            )
        }
    }

private fun build(
    userId: String,
    type: ProgressionRecommendationType,
    readiness: TrainingReadiness,
    exerciseId: String?,
    questTemplateId: String?,
    currentPrescriptionId: String?,
    proposed: ExercisePrescription?,
    proposedTarget: Int?,
    autoSafeAdaptation: Boolean,
    now: Long,
): ProgressionRecommendation {
    val isChange = type != ProgressionRecommendationType.MAINTAIN_PRESCRIPTION && type != ProgressionRecommendationType.REQUEST_MORE_DATA
    return ProgressionRecommendation(
        id = newId(),
        userId = userId,
        recommendationType = type,
        exerciseId = exerciseId,
        questTemplateId = questTemplateId,
        currentPrescriptionId = currentPrescriptionId,
        proposed = proposed,
        proposedTarget = proposedTarget,
        readinessState = readiness.state,
        reason = readiness.positiveSignals.firstOrNull() ?: readiness.limitingSignals.firstOrNull() ?: type.name,
        evidence = readiness.evidence + readiness.positiveSignals + readiness.limitingSignals,
        confidence = readiness.confidence,
        safetyState = readiness.safetyState,
        // Always confirm a change unless the user opted into automatic safe adaptation.
        requiresConfirmation = isChange && !autoSafeAdaptation,
        status = RecommendationStatus.PENDING,
        generatedAt = now,
        expiresAt = now + RECOMMENDATION_TTL_MS,
    )
}
