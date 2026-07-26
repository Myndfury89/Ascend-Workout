package com.ascend.core.domain.progression

import com.ascend.core.common.newId
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.ProgressionSnapshot
import com.ascend.core.model.XpSourceType

/** One class's before/after facts for building class presentation events. */
data class ClassEventInput(
    val classId: String,
    val className: String,
    val classXpFrom: Long,
    val classXpTo: Long,
    val classLevelFrom: Int,
    val classLevelTo: Int,
    val proficiencyKey: String,
    val proficiencyName: String,
    val proficiencyFrom: Long,
    val proficiencyTo: Long,
)

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
        classChanges: List<ClassEventInput> = emptyList(),
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
            subjectKey: String? = null,
            labelOverride: String? = null,
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
                    subjectKey = subjectKey,
                    fromValue = fromValue,
                    toValue = toValue,
                    label = labelOverride ?: label,
                    createdAt = now,
                )
        }

        // Player beats first: XP -> attributes -> level -> rank.
        if (after.lifetimeXp != before.lifetimeXp) {
            add(ProgressionEventType.XP_GAINED, before.lifetimeXp, after.lifetimeXp)
        }
        AttributeType.entries.forEach { attribute ->
            val from = before.attributes[attribute] ?: 0L
            val to = after.attributes[attribute] ?: 0L
            if (to != from) add(ProgressionEventType.ATTRIBUTE_CHANGED, from, to, attribute)
        }
        if (after.level > before.level) {
            add(ProgressionEventType.LEVEL_UP, before.level.toLong(), after.level.toLong())
        }
        if (after.rank.ordinal > before.rank.ordinal) {
            add(ProgressionEventType.RANK_UP, before.rank.ordinal.toLong(), after.rank.ordinal.toLong())
        }

        // Class beats, per class: Class XP -> class level -> unique proficiency.
        classChanges.forEach { c ->
            if (c.classXpTo != c.classXpFrom) {
                add(ProgressionEventType.CLASS_XP_GAINED, c.classXpFrom, c.classXpTo, subjectKey = c.classId, labelOverride = c.className)
            }
            if (c.classLevelTo > c.classLevelFrom) {
                add(
                    ProgressionEventType.CLASS_LEVEL_UP,
                    c.classLevelFrom.toLong(),
                    c.classLevelTo.toLong(),
                    subjectKey = c.classId,
                    labelOverride = c.className,
                )
            }
            if (c.proficiencyTo != c.proficiencyFrom) {
                add(
                    ProgressionEventType.PROFICIENCY_GAINED,
                    c.proficiencyFrom,
                    c.proficiencyTo,
                    subjectKey = c.proficiencyKey,
                    labelOverride = c.proficiencyName,
                )
            }
        }

        return events
    }
}
