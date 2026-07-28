package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestInterval
import javax.inject.Inject

/** An imported step record; [timestamp] is absolute epoch millis (timezone‑safe). */
data class StepRecord(
    val externalRecordId: String,
    val timestamp: Long,
    val steps: Int,
)

/**
 * Resolves imported step records into per‑interval progress by matching each record's
 * absolute timestamp to the interval whose [scheduledStart, scheduledEnd) window
 * contains it. Because boundaries are epoch millis, timezone and DST changes never
 * move them. Each record is assigned to at most one interval and de‑duplicated by
 * [StepRecord.externalRecordId], so imported steps are never double counted across
 * interval + daily totals.
 */
class StepIntervalProgressResolver
    @Inject
    constructor() {
        fun resolve(
            records: List<StepRecord>,
            intervals: List<QuestInterval>,
        ): Map<String, Int> {
            val perInterval = LinkedHashMap<String, Int>()
            val seen = HashSet<String>()
            val ordered = intervals.sortedBy { it.scheduledStart }
            for (record in records) {
                if (!seen.add(record.externalRecordId)) continue // duplicate import guard
                val interval =
                    ordered.firstOrNull { record.timestamp >= it.scheduledStart && record.timestamp < it.scheduledEnd }
                        ?: continue
                perInterval[interval.id] = (perInterval[interval.id] ?: 0) + record.steps
            }
            return perInterval
        }

        /** Total distinct imported steps (daily), independent of interval assignment. */
        fun dailyTotal(records: List<StepRecord>): Int = records.distinctBy { it.externalRecordId }.sumOf { it.steps }
    }
