package com.ascend.core.model

/*
 * Cardio progression. The engine works from the minimum viable evidence — manual
 * duration, distance, pace, and perceived effort — and never requires Health Connect or a
 * wearable. Heart-rate-zone and heart-rate-recovery fields are optional and only sharpen a
 * decision when present. One primary cardio variable moves at a time.
 */

/** The shape of a cardio session, which decides the primary progression variable. */
enum class CardioMode {
    STEADY_STATE,
    DISTANCE,
    INTERVAL,
    PACE_WORK,
}

/**
 * A prescribed cardio session. Dimensions are null when not applicable to the [mode]
 * (pace/distance for runs; work/rest/count for intervals; incline/resistance for machines).
 * Pace is seconds per kilometre — **lower is faster**.
 */
data class CardioPrescription(
    val id: String,
    val userId: String,
    val exerciseId: String,
    val mode: CardioMode,
    val targetDurationSeconds: Long? = null,
    val targetDistanceMeters: Double? = null,
    val targetPaceSecondsPerKm: Double? = null,
    val targetSpeed: Double? = null,
    val incline: Double? = null,
    val resistance: Double? = null,
    val intervalCount: Int? = null,
    val workIntervalSeconds: Int? = null,
    val restIntervalSeconds: Int? = null,
    val targetHrZoneSeconds: Long? = null,
    val effectiveFrom: Long = 0,
    val status: String = "ACTIVE",
)

/**
 * A recorded cardio exposure, derived from real logs. Only [actualDurationSeconds] and
 * effort/completion are needed to progress; HR fields are optional (wearable-only) and
 * default absent so the calculator degrades gracefully.
 */
data class CardioPerformanceSummary(
    val exerciseId: String,
    val actualDurationSeconds: Long? = null,
    val actualDistanceMeters: Double? = null,
    val actualPaceSecondsPerKm: Double? = null,
    val completedIntervals: Int? = null,
    val perceivedEffort: Int? = null,
    val averageHeartRate: Int? = null,
    val hrRecoveryBpm: Int? = null,
    val completed: Boolean = true,
    val completedAt: Long = 0,
)
