package com.ascend.feature.dashboard.prototype.resources

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Resolves MP (Energy) from deduplicated verified training-duration evidence against today's target.
 * Works phone-only (no Health Connect required). Deduplication is conservative — it errs toward
 * counting both records so two legitimate workouts are never collapsed.
 *
 * Dedup order (a candidate is the SAME session as a kept record when):
 *   1. they share a non-null stableSessionId, OR
 *   2. they share a non-null (sourceApplication, externalRecordId), OR
 *   3. the conservative time-overlap heuristic holds (different sources only):
 *        - time overlap >= 50% of the shorter record,
 *        - |durationA - durationB| <= max(2 min, 15% of the longer),
 *        - classifications compatible (equal, or one UNKNOWN),
 *        - a strict source-priority difference exists (so two same-source sessions are never merged).
 * When two records are the same session, the higher-priority source's record is kept.
 */
object MpEvidenceResolver {
    private const val OVERLAP_MIN_FRACTION = 0.50f
    private const val DURATION_TOLERANCE_MINUTES = 2
    private const val DURATION_TOLERANCE_FRACTION = 0.15f

    fun resolve(
        evidence: List<TrainingDurationEvidence>,
        target: DailyMpTarget,
    ): DailyMpState {
        if (target.isRestDay) {
            return DailyMpState(0, target, null, ResourceAvailability.REST_DAY, emptyList(), 0)
        }
        val (kept, suppressed) = deduplicate(evidence)
        val minutes = kept.sumOf { it.durationMinutes }
        val fraction = if (target.targetMinutes > 0) (minutes.toFloat() / target.targetMinutes).coerceAtLeast(0f) else null
        return DailyMpState(
            verifiedMinutes = minutes,
            target = target,
            progressFraction = fraction,
            availability = ResourceAvailability.AVAILABLE,
            includedSources = kept.map { it.source }.distinct(),
            suppressedDuplicateCount = suppressed,
        )
    }

    private fun deduplicate(evidence: List<TrainingDurationEvidence>): Pair<List<TrainingDurationEvidence>, Int> {
        val kept = ArrayList<TrainingDurationEvidence>()
        var suppressed = 0
        for (candidate in evidence) {
            val existingIndex = kept.indexOfFirst { sameSession(it, candidate) }
            if (existingIndex == -1) {
                kept.add(candidate)
            } else {
                suppressed++
                if (candidate.source.ordinal < kept[existingIndex].source.ordinal) {
                    kept[existingIndex] = candidate
                }
            }
        }
        return kept to suppressed
    }

    private fun sameSession(
        a: TrainingDurationEvidence,
        b: TrainingDurationEvidence,
    ): Boolean {
        if (a.stableSessionId != null && a.stableSessionId == b.stableSessionId) return true
        if (a.externalRecordId != null && a.sourceApplication != null &&
            a.externalRecordId == b.externalRecordId && a.sourceApplication == b.sourceApplication
        ) {
            return true
        }
        return overlapDuplicate(a, b)
    }

    private fun overlapDuplicate(
        a: TrainingDurationEvidence,
        b: TrainingDurationEvidence,
    ): Boolean {
        if (a.source == b.source) return false // never merge two same-source sessions
        if (!classificationsCompatible(a.classification, b.classification)) return false
        val overlap = min(a.endTime, b.endTime) - max(a.startTime, b.startTime)
        if (overlap <= 0) return false
        val shorter = min(a.endTime - a.startTime, b.endTime - b.startTime)
        if (shorter <= 0) return false
        if (overlap.toFloat() / shorter < OVERLAP_MIN_FRACTION) return false
        val longerDuration = max(a.durationMinutes, b.durationMinutes)
        val tolerance = max(DURATION_TOLERANCE_MINUTES, (longerDuration * DURATION_TOLERANCE_FRACTION).roundToInt())
        return kotlin.math.abs(a.durationMinutes - b.durationMinutes) <= tolerance
    }

    private fun classificationsCompatible(
        a: WorkoutKind,
        b: WorkoutKind,
    ): Boolean = a == b || a == WorkoutKind.UNKNOWN || b == WorkoutKind.UNKNOWN
}
