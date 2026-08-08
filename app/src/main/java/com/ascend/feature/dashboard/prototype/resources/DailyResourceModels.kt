package com.ascend.feature.dashboard.prototype.resources

/*
 * Read-only daily-resource state for the HP/MP/XP HUD prototype. HP (Vitality) = goal-relative daily
 * movement; MP (Energy) = goal-relative daily training duration; XP (Growth) = the existing
 * persistent progression value. This layer only PRESENTS evidence — it never awards XP/attributes,
 * never mutates progression, and adds no schema. All types are plain data so no repository (and thus
 * no mutation path) is reachable from here.
 */

/** Whether a resource has usable data today. Zero data is [AVAILABLE] with a 0 total — not [UNAVAILABLE]. */
enum class ResourceAvailability { AVAILABLE, UNAVAILABLE, REST_DAY }

/** Where today's movement total came from. */
enum class MovementSource { STEP_RECORDS, STEPS_QUEST, NONE }

/**
 * Training-duration evidence sources, in priority order (declaration order = priority; earliest
 * wins). Prefer sources whose session identity Ascend owns; use external/imported only where Ascend
 * has no native record; manual last. NOT "Health Connect always wins".
 */
enum class TrainingDurationSource {
    ASCEND_WORKOUT,
    ASCEND_CARDIO,
    QUEST_EVIDENCE,
    HEALTH_CONNECT,
    IMPORTED_ACTIVITY,
    MANUAL_CONFIRMED,
}

/** Minimal coarse classification, used only for dedup compatibility — not the canonical classifier. */
enum class WorkoutKind { STRENGTH, CARDIO, MIXED, UNKNOWN }

/** The prescribed shape of today's training, derived from readiness. */
enum class DailyMpTargetType { TRAIN, MAINTAIN, DELOAD, RECOVERY, REST }

/** Neutral, non-punitive progress bands shared by HP and MP (per the design's percentage bands). */
enum class ResourceBand(val label: String) {
    LOW("Low"),
    BUILDING("Building"),
    ACTIVE("Active"),
    STRONG("Strong"),
    FULL("Full"),
}

/** One piece of verified training-duration evidence for today. */
data class TrainingDurationEvidence(
    val source: TrainingDurationSource,
    val durationMinutes: Int,
    val startTime: Long,
    val endTime: Long,
    val stableSessionId: String? = null,
    val externalRecordId: String? = null,
    val sourceApplication: String? = null,
    val classification: WorkoutKind = WorkoutKind.UNKNOWN,
)

/** Today's training-duration target and its prescribed shape. */
data class DailyMpTarget(
    val targetMinutes: Int,
    val type: DailyMpTargetType,
    val explanation: String,
    val isRestDay: Boolean,
)

/** HP — Vitality: goal-relative daily movement. */
data class DailyHpState(
    val currentSteps: Int,
    val targetSteps: Int?,
    val progressFraction: Float?,
    val availability: ResourceAvailability,
    val source: MovementSource,
)

/** MP — Energy: goal-relative daily training. */
data class DailyMpState(
    val verifiedMinutes: Int,
    val target: DailyMpTarget,
    val progressFraction: Float?,
    val availability: ResourceAvailability,
    val includedSources: List<TrainingDurationSource>,
    val suppressedDuplicateCount: Int,
)

/** XP — Growth: the existing persistent progression value (read-only mirror of PlayerProgress). */
data class XpState(
    val level: Int,
    val currentLevelXp: Long,
    val xpForNextLevel: Long,
) {
    val progressFraction: Float get() = if (xpForNextLevel <= 0L) 0f else (currentLevelXp.toFloat() / xpForNextLevel)
}

/** Map a 0..1+ fraction to a neutral band. FULL at 100%+, then Strong/Active/Building/Low. */
fun resourceBand(fraction: Float): ResourceBand =
    when {
        fraction >= 1f -> ResourceBand.FULL
        fraction >= 0.75f -> ResourceBand.STRONG
        fraction >= 0.50f -> ResourceBand.ACTIVE
        fraction >= 0.25f -> ResourceBand.BUILDING
        else -> ResourceBand.LOW
    }
