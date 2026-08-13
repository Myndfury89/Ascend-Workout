package com.ascend.core.domain.build

/**
 * Where a piece of evidence came from. The resolvers never branch on this — it exists so provenance
 * and reliability can flow into confidence, and so a Health Connect provider can be added later as
 * just another source feeding the same shapes, without touching the characteristic engine.
 */
enum class EvidenceSource {
    ASCEND_WORKOUT,
    ASCEND_CARDIO,
    ASCEND_QUEST,
    MANUAL,
    HEALTH_CONNECT,
}

/** Movement modality, used to normalize Speed/Distance so (e.g.) 10 km cycling != 10 km running. */
enum class ActivityModality {
    RUN,
    WALK,
    CYCLE,
    ROW,
    SWIM,
    OTHER,
}

/**
 * Meaningful training *families* (modalities), not individual exercises. Versatility counts distinct
 * qualifying families; five chest lifts are one family ([TRADITIONAL_STRENGTH]), which is the
 * baseline and does not itself add versatility.
 */
enum class ActivityFamily {
    TRADITIONAL_STRENGTH,
    BODYWEIGHT,
    RUN_WALK,
    CYCLE,
    ROW,
    CONDITIONING,
    COMBAT,
    MOBILITY,
    AQUATIC,
    TEAM,
    RACQUET,
    DANCE,
    OUTDOOR,
    ;

    /** The one family that does not, by itself, increase Versatility. */
    val isBaseline: Boolean get() = this == TRADITIONAL_STRENGTH
}

/** What a recovery signal represents. Absence of these is never inferred as recovery. */
enum class RecoveryKind {
    READINESS_SNAPSHOT,
    RECOVERY_SESSION,
    REST_ADHERENCE,
}

/** A completed training session (any kind). Drives Activity. [at] is epoch millis. */
data class SessionEvidence(
    val at: Long,
    val durationSeconds: Long,
    val source: EvidenceSource = EvidenceSource.ASCEND_WORKOUT,
)

/** A completed working set of resistance training. Drives Strength. */
data class StrengthSetEvidence(
    val at: Long,
    val volume: Double,
    val isPersonalRecord: Boolean = false,
    val source: EvidenceSource = EvidenceSource.ASCEND_WORKOUT,
)

/** Participation in an activity family. Drives Versatility and (via family weight) Endurance. */
data class FamilyEvidence(
    val at: Long,
    val family: ActivityFamily,
    val durationSeconds: Long,
    val source: EvidenceSource = EvidenceSource.ASCEND_WORKOUT,
)

/** A measured distance effort. Drives Distance (modality-normalized). */
data class DistanceEvidence(
    val at: Long,
    val meters: Double,
    val modality: ActivityModality,
    val source: EvidenceSource = EvidenceSource.ASCEND_CARDIO,
)

/** A measured pace/speed effort. Drives Speed (modality-normalized). */
data class PaceEvidence(
    val at: Long,
    val metersPerSecond: Double,
    val modality: ActivityModality,
    val source: EvidenceSource = EvidenceSource.ASCEND_CARDIO,
)

/** A recovery signal. Drives Recovery. [readinessScore] is 0..100 when present. */
data class RecoverySignal(
    val at: Long,
    val kind: RecoveryKind,
    val readinessScore: Double? = null,
    val source: EvidenceSource = EvidenceSource.ASCEND_WORKOUT,
)

/**
 * All evidence supplied to the engine for one user, already gathered by an evidence provider. The
 * engine is a pure function of this bundle plus `now` and [BuildTuning] — no repository, DB, or
 * source knowledge leaks in, which is what keeps it headlessly testable and HC-ready.
 */
data class BuildEvidence(
    val sessions: List<SessionEvidence> = emptyList(),
    val strengthSets: List<StrengthSetEvidence> = emptyList(),
    val families: List<FamilyEvidence> = emptyList(),
    val distances: List<DistanceEvidence> = emptyList(),
    val paces: List<PaceEvidence> = emptyList(),
    val recovery: List<RecoverySignal> = emptyList(),
) {
    companion object {
        val EMPTY = BuildEvidence()
    }
}
