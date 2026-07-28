package com.ascend.core.model

/**
 * How a Daily Quest's daily total is *executed*. The daily objective ("200 push‑ups
 * today") is separate from the execution plan — the objective can still be completed
 * even if the interval plan changes.
 */
enum class QuestScheduleMode {
    FLEXIBLE_DAILY_TOTAL,
    FIXED_INTERVALS,
    SUGGESTED_INTERVALS,
    TIME_WINDOW_BLOCKS,
    MANUAL_CHECKPOINTS,
}

/** How a target is spread across intervals. */
enum class IntervalDistributionStrategy {
    EQUAL,
    PREFERRED_SET_SIZE,
    FRONT_LOADED,
    BACK_LOADED,
    CUSTOM,
}

/** Strategy for rebalancing remaining work after a missed or partial interval. */
enum class RedistributionPreference {
    EVEN,
    LIGHTER_NEXT,
    HEAVIER_FINAL,
    PRESERVE_ORIGINAL,
    ASK_EVERY_TIME,
}

enum class QuestIntervalStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PARTIAL,
    MISSED,
    SKIPPED,
}

/** A scheduled portion of a Daily Quest's target within a time slot. */
data class QuestInterval(
    val id: String,
    val questId: String,
    val title: String,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val targetValue: Double,
    val currentValue: Double,
    val isCumulative: Boolean,
    val status: QuestIntervalStatus,
    val orderIndex: Int,
    val reminderEnabled: Boolean,
    val completedAt: Long?,
) {
    val remaining: Double get() = (targetValue - currentValue).coerceAtLeast(0.0)
    val isComplete: Boolean get() = currentValue >= targetValue
}

/** The execution plan attached to a Daily Quest. */
data class QuestIntervalSchedule(
    val id: String,
    val questId: String,
    val scheduleMode: QuestScheduleMode,
    val activeWindowStart: Long,
    val activeWindowEnd: Long,
    val intervalCount: Int,
    val distributionStrategy: IntervalDistributionStrategy,
    val adaptiveRedistributionEnabled: Boolean,
    val redistributionPreference: RedistributionPreference,
)

/** A cumulative "by this time" milestone (Manual Checkpoints mode). */
data class QuestCheckpoint(
    val id: String,
    val questId: String,
    val title: String,
    val targetValue: Double,
    val dueAt: Long,
    val isCumulative: Boolean,
    val status: QuestIntervalStatus,
    val completedAt: Long?,
)

/** A single logged contribution toward an interval (multiple per interval allowed). */
data class QuestIntervalProgressEntry(
    val id: String,
    val questIntervalId: String,
    val questId: String,
    val value: Double,
    val source: ProgressSource,
    val sourceApplication: String?,
    val externalRecordId: String?,
    val completedAt: Long,
    val note: String?,
)

/** A time window with a portion of the daily target (Time‑Window Blocks mode). */
data class TimeWindow(
    val label: String,
    val start: Long,
    val end: Long,
)
