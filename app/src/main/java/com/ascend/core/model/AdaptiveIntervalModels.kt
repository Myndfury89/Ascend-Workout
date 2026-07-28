package com.ascend.core.model

/*
 * Adaptive Daily-Quest interval redistribution. The **daily total is authoritative** — if
 * the day's target is met, interval timing never counts as failure, and a missed interval
 * is a scheduling event, not a shortfall to punish. Automatic adaptation is opt-in and may
 * never exceed the max interval target or set size, enter quiet hours, ignore high fatigue,
 * override a safety flag, or increase the daily total.
 */

/** What to do with work left over from a missed or partial interval. */
enum class AdaptiveIntervalAction {
    REDISTRIBUTE_EVENLY,
    LIGHTER_NEXT_INTERVAL,
    HEAVIER_FINAL_INTERVAL,
    PRESERVE_FUTURE_LEAVE_FLEXIBLE,
    ADD_NEW_INTERVAL,
    REDUCE_INTERVAL_SIZES,
    REDUCE_DAILY_TOTAL,
    CONVERT_TO_FLEXIBLE,
    MAINTAIN_PLAN,
    REQUEST_USER_CHOICE,
}

/** A future interval that may receive redistributed work. */
data class FutureInterval(
    val intervalId: String,
    val currentTarget: Int,
    val scheduledStart: Long,
)

/**
 * Everything the redistribution engine needs, as pure data. `leftover` is the amount from
 * a missed/partial interval that still needs a home. The daily total is
 * [dailyTarget]; [currentDailyProgress] already counts everything logged today (regardless
 * of which interval it landed in).
 */
data class AdaptiveIntervalContext(
    val dailyTarget: Int,
    val currentDailyProgress: Int,
    val leftover: Int,
    val futureIntervals: List<FutureInterval>,
    val timeRemainingMillis: Long,
    val quietHours: List<Pair<Long, Long>> = emptyList(),
    val minimumSetSize: Int = 1,
    val maximumSetSize: Int = Int.MAX_VALUE,
    val maximumIntervalTarget: Int = Int.MAX_VALUE,
    val fatigue: Int? = null,
    val highFatigueThreshold: Int = 8,
    val readinessState: TrainingReadinessState = TrainingReadinessState.MAINTAIN,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
    val redistributionPreference: RedistributionPreference = RedistributionPreference.EVEN,
    val autoSafeAdaptationEnabled: Boolean = false,
    val now: Long = 0,
) {
    val remainingDailyTarget: Int get() = (dailyTarget - currentDailyProgress).coerceAtLeast(0)
    val dailyTargetMet: Boolean get() = currentDailyProgress >= dailyTarget
    val highFatigue: Boolean get() = (fatigue ?: 0) >= highFatigueThreshold
}

/**
 * A single redistribution recommendation. [proposedTargets] pairs each affected future
 * interval id with its new target; [leftoverFlexible] is any amount intentionally left
 * unscheduled. [autoApplied] is true only when automatic safe adaptation actually applied
 * the change (which never happens under any blocked constraint).
 */
data class AdaptiveIntervalRecommendation(
    val action: AdaptiveIntervalAction,
    val summary: String,
    val reasons: List<String>,
    val proposedTargets: Map<String, Int> = emptyMap(),
    val newIntervalTarget: Int? = null,
    val proposedDailyTotal: Int? = null,
    val leftoverFlexible: Int = 0,
    val requiresConfirmation: Boolean = true,
    val autoApplied: Boolean = false,
    val safetyState: ProgressionSafetyState = ProgressionSafetyState.OK,
    val constraintNotes: List<String> = emptyList(),
)
