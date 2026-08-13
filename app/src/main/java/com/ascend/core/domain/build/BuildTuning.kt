package com.ascend.core.domain.build

/**
 * Every tunable number for the Build engine, in one place. These are the approved P1 *starting
 * hypotheses*, not final balance — kept parameterized so they can be tuned from real telemetry
 * without touching resolver logic (the same discipline applied to the comparison window).
 *
 * Window model (approved): a [currentWindowDays] rolling window drives scores and current identity;
 * a [lookbackWindowDays] window feeds only confidence/coverage for sparse signals. Older evidence
 * can raise confidence that a signal is *known*, but never makes a stale emphasis look current.
 */
data class BuildTuning(
    // --- Windows ---
    val currentWindowDays: Int = 28,
    val lookbackWindowDays: Int = 56,
    /** Distinct qualifying days a sparse signal (Speed, Distance, Endurance, Recovery) needs to reach OK. */
    val sparseGuardMinSessions: Int = 2,
    /** Distinct days Activity/Strength need to reach OK. */
    val minSessionsForOk: Int = 2,
    /** Qualifying strength sets needed (across [minSessionsForOk] days) for Strength OK. */
    val minStrengthSets: Int = 3,
    /** At/above this many distinct active days in the current window, an absent signal counts as a confident ZERO (not INSUFFICIENT). */
    val confidentAbsenceMinActiveDays: Int = 4,
    /** A family session must reach this many endurance-weighted minutes to qualify as an Endurance session (60 min of lifting at 0.2x = 12, deliberately below this). */
    val enduranceQualifyingWeightedMinutes: Double = 15.0,
    /** A family must reach this many aggregate minutes in-window to count toward Versatility. */
    val versatilityFamilyMinMinutes: Double = 20.0,
    // --- Saturation half-points (value at which score reaches 50) ---
    val activityHalfSatDays: Double = 6.0,
    val strengthHalfSatVolume: Double = 12_000.0,
    val versatilityHalfSatFamilies: Double = 1.5,
    val enduranceHalfSatWeightedMinutes: Double = 120.0,
    val distanceHalfSatUnits: Double = 4.0,
    val recoveryCountHalfSat: Double = 2.0,
    /** Upper clamp on a single pace effort's emphasis, so one very fast effort can't dominate. */
    val speedEmphasisCap: Double = 1.5,
    // --- Bounded contributions ---
    val personalRecordBonusPerPr: Double = 4.0,
    val personalRecordBonusCap: Double = 12.0,
    /** Distinct lookback days at which confidence saturates to 1.0. */
    val confidenceFullDays: Double = 6.0,
    val insufficientConfidenceCap: Double = 0.3,
    /** Minimum class Coverage to be eligible as a dominant/strongest affinity (confirmed against modeled density; parameterized for telemetry). */
    val dominantCoverageGate: Double = 0.5,
    /** Fractional score change (of 100) beyond which a trend is DEVELOPING / DE_EMPHASIZED rather than MAINTAINING. */
    val trendMarginPoints: Double = 8.0,
    /** Reference speed (m/s) per modality at which Speed emphasis for that modality reads as ~1.0. */
    val speedReferenceMps: Map<ActivityModality, Double> =
        mapOf(
            ActivityModality.RUN to 3.3,
            ActivityModality.WALK to 1.6,
            ActivityModality.CYCLE to 8.3,
            ActivityModality.ROW to 3.8,
            ActivityModality.SWIM to 1.2,
            ActivityModality.OTHER to 3.0,
        ),
    /** Reference distance (metres) per modality that counts as ~1.0 distance unit. */
    val distanceReferenceMeters: Map<ActivityModality, Double> =
        mapOf(
            ActivityModality.RUN to 5_000.0,
            ActivityModality.WALK to 6_000.0,
            ActivityModality.CYCLE to 15_000.0,
            ActivityModality.ROW to 4_000.0,
            ActivityModality.SWIM to 1_500.0,
            ActivityModality.OTHER to 5_000.0,
        ),
    /** How strongly each family contributes to Endurance (steady cardio ~1.0; lifting down-weighted so it never qualifies alone). */
    val enduranceFamilyWeight: Map<ActivityFamily, Double> =
        mapOf(
            ActivityFamily.RUN_WALK to 1.0,
            ActivityFamily.CYCLE to 1.0,
            ActivityFamily.ROW to 1.0,
            ActivityFamily.OUTDOOR to 0.8,
            ActivityFamily.AQUATIC to 0.9,
            ActivityFamily.CONDITIONING to 0.7,
            ActivityFamily.TEAM to 0.7,
            ActivityFamily.RACQUET to 0.6,
            ActivityFamily.COMBAT to 0.5,
            ActivityFamily.DANCE to 0.5,
            ActivityFamily.BODYWEIGHT to 0.3,
            ActivityFamily.TRADITIONAL_STRENGTH to 0.2,
            ActivityFamily.MOBILITY to 0.1,
        ),
) {
    /** Endurance weight for a family, defaulting low for anything unmapped. */
    fun enduranceWeightFor(family: ActivityFamily): Double = enduranceFamilyWeight[family] ?: 0.2

    fun speedReferenceFor(modality: ActivityModality): Double = speedReferenceMps[modality] ?: 3.0

    fun distanceReferenceFor(modality: ActivityModality): Double = distanceReferenceMeters[modality] ?: 5_000.0
}
