package com.ascend.core.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity

data class ObjectiveWithEntries(
    @Embedded val objective: QuestObjectiveEntity,
    @Relation(parentColumn = "id", entityColumn = "objectiveId")
    val entries: List<QuestProgressEntryEntity>,
)

data class QuestWithObjectives(
    @Embedded val quest: QuestEntity,
    @Relation(
        entity = QuestObjectiveEntity::class,
        parentColumn = "id",
        entityColumn = "questId",
    )
    val objectives: List<ObjectiveWithEntries>,
)
