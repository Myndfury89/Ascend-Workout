package com.ascend.core.domain.build

/**
 * The seven Build Characteristics — a read-only description of "what kind of training am I actually
 * doing," derived from verified Ascend-native evidence. This layer NEVER grants or removes
 * progression; it only observes.
 *
 * [ACTIVITY] is scored and displayed like the others, but is deliberately **excluded from class
 * signatures and the class coverage math** ([inClassSignature] = false). It could not differentiate
 * between classes (every class is trained by being active) and would manufacture false coverage.
 * Instead Activity feeds *overall* confidence — how much recent evidence exists at all — and, in
 * particular, decides whether an absent signal is a confident [EvidenceState.ZERO] or an
 * uncertain [EvidenceState.INSUFFICIENT].
 */
enum class BuildCharacteristic {
    ACTIVITY,
    STRENGTH,
    VERSATILITY,
    ENDURANCE,
    SPEED,
    DISTANCE,
    RECOVERY,
    ;

    /** False only for [ACTIVITY]: it never contributes a per-class signature weight. */
    val inClassSignature: Boolean get() = this != ACTIVITY
}

/**
 * Availability of a characteristic, kept strictly separate from its score. These four states are not
 * interchangeable and are never collapsed into "0".
 */
enum class EvidenceState {
    /** Enough qualifying, in-window evidence to score reliably. Enters affinity. */
    OK,

    /** Some evidence exists but below the reliability threshold (e.g. a single session). Excluded from affinity. */
    INSUFFICIENT,

    /** The source is available and qualifying evidence is genuinely nil, for a user active enough that the absence is meaningful. Enters affinity as 0. */
    ZERO,

    /** No source for this signal (or too little overall activity to judge). Excluded from affinity. */
    UNAVAILABLE,
}

/** Direction of a characteristic relative to the preceding window. Deliberately non-judgemental language. */
enum class BuildTrend {
    /** Represents more of recent training than in the prior window. */
    DEVELOPING,

    /** Roughly steady between windows. */
    MAINTAINING,

    /** Represents less of recent training than in the prior window (never "declining"). */
    DE_EMPHASIZED,

    /** Not enough evidence in one or both windows to judge a direction. */
    UNKNOWN,
}

/**
 * One resolved characteristic. [score] (0..100) is meaningful only when [state] is
 * [EvidenceState.OK] or [EvidenceState.ZERO]; [confidence] (0..1) is independent of the score.
 */
data class CharacteristicScore(
    val characteristic: BuildCharacteristic,
    val score: Double,
    val state: EvidenceState,
    val trend: BuildTrend,
    val confidence: Double,
)

/**
 * The full read-only build profile: every characteristic plus an [overallConfidence] fed by overall
 * Activity/evidence volume, and the current scoring [windowDays].
 */
data class BuildProfile(
    val scores: Map<BuildCharacteristic, CharacteristicScore>,
    val overallConfidence: Double,
    val windowDays: Int,
) {
    operator fun get(characteristic: BuildCharacteristic): CharacteristicScore? = scores[characteristic]
}
