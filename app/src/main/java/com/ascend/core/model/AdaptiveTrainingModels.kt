package com.ascend.core.model

/**
 * Adaptive Training & Progressive Overload domain. The core rule: **real performance
 * determines readiness**; game progression (Player/Class level, EXP) only unlocks
 * options and grants rewards — it never independently forces a change in weight,
 * reps, sets, rest, variation, pace, distance, or volume.
 */
enum class TrainingReadinessState {
    INSUFFICIENT_DATA,
    RECOVERY_RECOMMENDED,
    DELOAD_RECOMMENDED,
    REGRESS,
    MAINTAIN,
    READY_FOR_REP_PROGRESSION,
    READY_FOR_LOAD_PROGRESSION,
    READY_FOR_SET_PROGRESSION,
    READY_FOR_REST_PROGRESSION,
    READY_FOR_VARIATION_PROGRESSION,
    READY_FOR_CARDIO_PROGRESSION,
}

enum class ProgressionRecommendationType {
    INCREASE_WEIGHT,
    INCREASE_REPS,
    REDUCE_REPS,
    ADD_SET,
    REMOVE_SET,
    REDUCE_REST,
    INCREASE_REST,
    ADVANCE_VARIATION,
    REGRESS_VARIATION,
    REDUCE_ASSISTANCE,
    INCREASE_ASSISTANCE,
    ADD_EXTERNAL_LOAD,
    SLOW_ECCENTRIC,
    ADD_PAUSE,
    INCREASE_RANGE_OF_MOTION,
    INCREASE_DURATION,
    REDUCE_DURATION,
    INCREASE_DISTANCE,
    INCREASE_PACE,
    REDUCE_PACE,
    INCREASE_INCLINE,
    INCREASE_RESISTANCE,
    ADD_INTERVAL,
    REDUCE_INTERVAL,
    CHANGE_WORK_REST_RATIO,
    INCREASE_DAILY_QUEST_TARGET,
    REDUCE_DAILY_QUEST_TARGET,
    MAINTAIN_PRESCRIPTION,
    LIGHT_SESSION,
    DELOAD,
    REQUEST_MORE_DATA,
}

enum class RecommendationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    APPLIED,
    COMPLETED_SUCCESSFULLY,
    COMPLETED_UNSUCCESSFULLY,
    EXPIRED,
    CANCELLED,
}

/** Never a medical diagnosis — a training‑safety gate only. */
enum class ProgressionSafetyState {
    OK,
    CAUTION,
    BLOCKED,
    STOP_AND_SEEK_GUIDANCE,
}

enum class ProgressionStrategy {
    DOUBLE_PROGRESSION,
    LINEAR_LOAD,
    REP_PROGRESSION,
    VARIATION_PROGRESSION,
    CARDIO_PROGRESSION,
    DAILY_QUEST,
}

/** Milestones that are rewardable — only after the harder prescription is proven. */
enum class ProgressionMilestoneType {
    LOAD_INCREASE_COMPLETED,
    REP_RECORD,
    SET_VOLUME_RECORD,
    SET_ADDED,
    SESSION_VOLUME_RECORD,
    REST_EFFICIENCY,
    VARIATION_ADVANCED,
    ASSISTANCE_REDUCED,
    TEMPO_PROGRESSED,
    RANGE_OF_MOTION_IMPROVED,
    EXTERNAL_LOAD_ADDED,
    CARDIO_DURATION_MILESTONE,
    DISTANCE_MILESTONE,
    PACE_MILESTONE,
    CARDIO_RESISTANCE_MILESTONE,
    INTERVAL_PROGRESSED,
    HR_RECOVERY_IMPROVED,
    DAILY_QUEST_TARGET_PROGRESSED,
    ADAPTIVE_PLAN_COMPLETED,
    DELOAD_RETURN,
    TRAINING_BLOCK_COMPLETED,
}

/** One recorded working set. Optional signals (RIR/RPE/form) may be absent. */
data class SetPerformance(
    val reps: Int,
    val weight: Double? = null,
    val failed: Boolean = false,
    val repsInReserve: Int? = null,
    val rpe: Double? = null,
)

/**
 * A recorded exposure of an exercise. Derived from workout records in production; the
 * subjective fields are optional and only sharpen confidence when supplied.
 */
data class SessionPerformance(
    val exerciseId: String,
    val prescriptionId: String?,
    val sets: List<SetPerformance>,
    val restSeconds: Int? = null,
    val perceivedEffort: Int? = null,
    val formRating: Int? = null,
    val painReported: Boolean = false,
    val injuryReported: Boolean = false,
    val completedAt: Long = 0,
) {
    val topSetReps: Int get() = sets.maxOfOrNull { it.reps } ?: 0
    val totalReps: Int get() = sets.sumOf { it.reps }
    val anyFailure: Boolean get() = sets.any { it.failed }
}

/** A finished Daily Quest outcome, derived from quest records (the baseline source). */
data class QuestOutcome(
    val questId: String,
    val target: Int,
    val completed: Int,
    val status: String,
    val createdAt: Long,
) {
    val completionPercentage: Double get() = if (target <= 0) 1.0 else (completed.toDouble() / target).coerceIn(0.0, 2.0)
}

/** Subjective/recovery check‑in; all optional, used to gate or temper progression. */
data class ReadinessCheckIn(
    val fatigue: Int? = null,
    val soreness: Int? = null,
    val painReported: Boolean = false,
    val injuryReported: Boolean = false,
    val seriousSymptomReported: Boolean = false,
)

/** Structured, explainable readiness — never a single opaque number. */
data class TrainingReadiness(
    val state: TrainingReadinessState,
    val score: Double,
    val confidence: Double,
    val evidence: List<String>,
    val positiveSignals: List<String>,
    val limitingSignals: List<String>,
    val missingSignals: List<String>,
    val safetyState: ProgressionSafetyState,
    val safetyFlags: List<String>,
    val generatedAt: Long,
)

/** A prescribed exercise (resistance/bodyweight/cardio) — dimensions null when N/A. */
data class ExercisePrescription(
    val id: String,
    val userId: String,
    val exerciseId: String,
    val progressionStrategy: ProgressionStrategy,
    val targetSets: Int? = null,
    val minimumReps: Int? = null,
    val maximumReps: Int? = null,
    val targetWeight: Double? = null,
    val targetRestSeconds: Int? = null,
    val targetDurationSeconds: Long? = null,
    val targetDistance: Double? = null,
    val targetPace: Double? = null,
    val tempo: String? = null,
    val variationId: String? = null,
    val assistanceValue: Double? = null,
    val effectiveFrom: Long = 0,
    val status: String = "ACTIVE",
)

/**
 * A progression recommendation the user accepts/rejects. The proposed change is a
 * (partial) prescription; nothing is applied until accepted, and no reward is granted
 * until the harder prescription is proven.
 */
data class ProgressionRecommendation(
    val id: String,
    val userId: String,
    val recommendationType: ProgressionRecommendationType,
    val exerciseId: String?,
    val questTemplateId: String?,
    val currentPrescriptionId: String?,
    val proposed: ExercisePrescription?,
    val proposedTarget: Int?,
    val readinessState: TrainingReadinessState,
    val reason: String,
    val evidence: List<String>,
    val confidence: Double,
    val safetyState: ProgressionSafetyState,
    val requiresConfirmation: Boolean,
    val status: RecommendationStatus,
    val generatedAt: Long,
    val expiresAt: Long,
    val acceptedAt: Long? = null,
    val rejectedAt: Long? = null,
    val appliedAt: Long? = null,
)

/** Rolling baseline for a Daily Quest activity — never a single outlier. */
data class RecentBaseline(
    val representativeTarget: Int,
    val averageCompletedAmount: Double,
    val averageCompletionPercentage: Double,
    val completionConsistency: Double,
    val averagePerceivedEffort: Double?,
    val recentTrend: Double,
    val confidence: Double,
    val sampleCount: Int,
)

/** A proven progression milestone; rewarded exactly once by its identity. */
data class ProgressionMilestone(
    val id: String,
    val userId: String,
    val milestoneType: ProgressionMilestoneType,
    val exerciseId: String?,
    val questTemplateId: String?,
    val sourceRecommendationId: String?,
    val previousValue: Double,
    val newValue: Double,
    val createdAt: Long,
)

/** Inspectable reward for a proven progression (Player XP stays class‑neutral). */
data class ProgressionRewardBreakdown(
    val milestoneType: ProgressionMilestoneType,
    val playerXp: Long,
    val attributeProficiency: Map<AttributeType, Long>,
    val primaryClass: ClassRewardLine?,
    val secondaryClass: ClassRewardLine?,
    val alreadyAwarded: Boolean,
)
