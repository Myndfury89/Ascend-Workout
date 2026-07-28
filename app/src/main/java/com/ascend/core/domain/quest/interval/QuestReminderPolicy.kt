package com.ascend.core.domain.quest.interval

import com.ascend.core.model.QuestInterval
import com.ascend.core.model.QuestIntervalStatus
import javax.inject.Inject

/**
 * Decides whether an interval reminder should fire. Suppressed when the interval or
 * quest is already done, the interval was skipped/missed, notifications are off, or
 * the moment falls within quiet hours.
 */
class QuestReminderPolicy
    @Inject
    constructor() {
        fun shouldRemind(
            interval: QuestInterval,
            now: Long,
            questComplete: Boolean,
            notificationsEnabled: Boolean,
            quietHours: List<Pair<Long, Long>> = emptyList(),
        ): Boolean {
            if (!notificationsEnabled) return false
            if (questComplete) return false
            if (interval.isComplete) return false
            if (interval.status == QuestIntervalStatus.SKIPPED || interval.status == QuestIntervalStatus.COMPLETED) return false
            if (inQuietHours(now, quietHours)) return false
            return true
        }

        fun inQuietHours(
            now: Long,
            quietHours: List<Pair<Long, Long>>,
        ): Boolean = quietHours.any { (start, end) -> now in start until end }
    }
