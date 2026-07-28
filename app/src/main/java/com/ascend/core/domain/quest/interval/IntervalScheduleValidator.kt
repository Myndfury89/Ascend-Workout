package com.ascend.core.domain.quest.interval

import javax.inject.Inject

enum class IntervalIssue {
    END_BEFORE_START,
    OVERLAP,
    NEGATIVE_TARGET,
}

/**
 * The assignment picture for a schedule: [issues] found, plus how the interval
 * targets compare to the authoritative daily target (so the UI can offer distribute /
 * add‑to‑final / scale / keep‑flexible actions).
 */
data class IntervalScheduleValidation(
    val issues: List<IntervalIssue>,
    val assigned: Int,
    val unassigned: Int,
    val overAssigned: Int,
) {
    val isValid: Boolean get() = issues.isEmpty()
}

/**
 * Validates a generated/edited interval schedule: no overlapping windows, ends after
 * starts, non‑negative targets, and how much of the daily target is (un/over)assigned.
 */
class IntervalScheduleValidator
    @Inject
    constructor() {
        fun validate(
            intervals: List<GeneratedInterval>,
            dailyTarget: Int,
        ): IntervalScheduleValidation {
            val issues = LinkedHashSet<IntervalIssue>()

            if (intervals.any { it.scheduledEnd <= it.scheduledStart }) issues += IntervalIssue.END_BEFORE_START
            if (intervals.any { it.target < 0 }) issues += IntervalIssue.NEGATIVE_TARGET

            val ordered = intervals.sortedBy { it.scheduledStart }
            val overlaps = ordered.zipWithNext().any { (a, b) -> a.scheduledEnd > b.scheduledStart }
            if (overlaps) issues += IntervalIssue.OVERLAP

            val assigned = intervals.sumOf { it.target }
            val unassigned = (dailyTarget - assigned).coerceAtLeast(0)
            val overAssigned = (assigned - dailyTarget).coerceAtLeast(0)

            return IntervalScheduleValidation(issues.toList(), assigned, unassigned, overAssigned)
        }
    }
