package com.ascend.core.domain.progression

import com.ascend.core.common.newId
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.ProgressionSnapshot
import com.ascend.core.model.XpSourceType

/**
 * Turns a before/after progression [ProgressionSnapshot] pair into an ordered batch
 * of [ProgressionEvent]s — the animation script for one earning. Pure and
 * deterministic: the same source always produces the same sequence, so persisting
 * it with an exactly‑once guard is safe.
 *
 * Order (the sequence the Status screen plays): XP gain first, then each changed
 * attribute in stable [AttributeType] order, then a level‑up beat, then a rank‑up
 * beat. Only actual changes emit an event.
 */
class ProgressionEventFactory {
    fun build(
        userId: String,
        sourceType: XpSourceType,
        sourceId: String,
        before: ProgressionSnapshot,
        after: ProgressionSnapshot,
        label: String? = null,
        now: Long = System.currentTimeMillis(),
    ): List<ProgressionEvent> {
        val batchId = sourceId
        val events = ArrayList<ProgressionEvent>()

        fun add(
            type: ProgressionEventType,
            fromValue: Long,
            toValue: Long,
            attribute: AttributeType? = null,
        ) {
            events +=
                ProgressionEvent(
                    id = newId(),
                    userId = userId,
                    batchId = batchId,
                    sequence = events.size,
                    type = type,
                    sourceType = sourceType,
                    sourceId = sourceId,
                    attribute = attribute,
                    fromValue = fromValue,
                    toValue = toValue,
                    label = label,
                    createdAt = now,
                )
        }

        if (after.lifetimeXp != before.lifetimeXp) {
            add(ProgressionEventType.XP_GAINED, before.lifetimeXp, after.lifetimeXp)
        }

        AttributeType.entries.forEach { attribute ->
            val from = before.attributes[attribute] ?: 0L
            val to = after.attributes[attribute] ?: 0L
            if (to != from) {
                add(ProgressionEventType.ATTRIBUTE_CHANGED, from, to, attribute)
            }
        }

        if (after.level > before.level) {
            add(ProgressionEventType.LEVEL_UP, before.level.toLong(), after.level.toLong())
        }

        if (after.rank.ordinal > before.rank.ordinal) {
            add(ProgressionEventType.RANK_UP, before.rank.ordinal.toLong(), after.rank.ordinal.toLong())
        }

        return events
    }
}
