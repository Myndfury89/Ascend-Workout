package com.ascend.core.data.mapper

import com.ascend.core.database.entity.ProgressionEventEntity
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.XpSourceType

fun ProgressionEventEntity.toDomain(): ProgressionEvent =
    ProgressionEvent(
        id = id,
        userId = userId,
        batchId = batchId,
        sequence = sequence,
        type = runCatching { ProgressionEventType.valueOf(type) }.getOrDefault(ProgressionEventType.XP_GAINED),
        sourceType = runCatching { XpSourceType.valueOf(sourceType) }.getOrDefault(XpSourceType.MANUAL_ADJUSTMENT),
        sourceId = sourceId,
        attribute = attributeType?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() },
        subjectKey = subjectKey,
        fromValue = fromValue,
        toValue = toValue,
        label = label,
        createdAt = createdAt,
        consumedAt = consumedAt,
    )

fun ProgressionEvent.toEntity(): ProgressionEventEntity =
    ProgressionEventEntity(
        id = id,
        userId = userId,
        batchId = batchId,
        sequence = sequence,
        type = type.name,
        sourceType = sourceType.name,
        sourceId = sourceId,
        attributeType = attribute?.name,
        subjectKey = subjectKey,
        fromValue = fromValue,
        toValue = toValue,
        label = label,
        createdAt = createdAt,
        consumedAt = consumedAt,
    )
